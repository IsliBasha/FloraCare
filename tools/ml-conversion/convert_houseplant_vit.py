"""Convert dima806/house-plant-image-detection (HF ViT) to an Android-ready TFLite model.

This is a true drop-in for FloraCare's existing `TfliteRunner` (see
app/src/main/java/com/floracare/app/data/ml/TfliteRunner.kt), which auto-detects
a model's actual input/output dtype at runtime (TfliteRunner.kt:141-142) and has
two supported paths: UINT8 (dequantize and trust as-is) or float32 (apply its own
temperature-scaled softmax calibration). This model uses the float32 path:

1. **Float32 input/output**, not uint8. A pure-uint8 boundary was tried first
   (see UPSTREAM_BUGS.md and git history), but full INT8 quantization of this
   model's activations crashes at runtime: its GELU/Erf approximation includes
   a "1 / (1 + data_dependent_value)" division, and INT8's coarse 256-level
   resolution lets that denominator legitimately land on exactly 0 for some
   input (reproduced with both random noise and real photos). INT16
   activations don't work around it either -- TFLite's 16x8 quantizer flatly
   refuses to quantize DIV at all. Quantization here is *dynamic range*
   instead: only weights compress to INT8; every activation (including that
   DIV's denominator) runs in genuine, uncalibrated float32, so it can never
   round to a quantized zero. Weights are the large majority of a
   transformer's size, so this keeps most of the compression win (87.8MB vs.
   343MB float32) with none of the activation-quantization fragility.
2. Normalization baked into the graph itself. The model's real preprocessing
   config (image_mean/image_std, pulled from its own processor config, not
   assumed) is applied *inside* the exported model, so a caller can feed raw
   [0, 255] pixel values directly.
3. Softmax baked into the graph output, since TfliteRunner's float32 path
   applies its *own* softmax calibration on top -- meaning this model's output
   gets softmaxed twice. That's an accepted, deliberate tradeoff (see the
   AskUserQuestion decision in conversation history): it compresses the
   confidence percentages TfliteRunner reports, but doesn't change which
   class ranks first, since softmax preserves relative ordering.

Quantization is done with plain `tf.lite.TFLiteConverter`, not onnx2tf's own
built-in INT8 pipeline -- that pipeline has two confirmed bugs (see
UPSTREAM_BUGS.md): it crashes on negative INT8 scalar constants, and even
past that, it miscalibrates the input/output tensors (the exact same
calibration data, run through plain TFLiteConverter instead, calibrates
correctly). onnx2tf is only used here to get from ONNX to a plain TF
SavedModel; everything from there is standard TensorFlow we control directly.

Separately, LayerNorm's eps constant (1e-12 in this model) is bumped to 1e-3
before export. This was needed to unblock an earlier full-INT8-activation
attempt (the original value rounds to exactly 0 under INT8's 256 discrete
levels, reintroducing the divide-by-zero eps exists to prevent) and isn't
strictly required for the dynamic range quantization actually used now
(activations stay float32, so eps is never quantized) -- left in as a
harmless, negligible-impact safety margin in case activation quantization
is revisited later.

Usage:
    .venv/bin/python convert_houseplant_vit.py

Output:
    build/houseplant_vit_v1.tflite
    build/houseplant_vit_labels.txt   (id,name -- same CSV shape as species_labels.txt)
"""

from __future__ import annotations

import csv
import io
import pathlib
import urllib.request

import numpy as np
import onnx
import torch
import torch.nn as nn
from transformers import AutoImageProcessor, AutoModelForImageClassification

MODEL_ID = "dima806/house-plant-image-detection"
HERE = pathlib.Path(__file__).parent
BUILD = HERE / "build"
BUILD.mkdir(exist_ok=True)

ONNX_PATH = BUILD / "houseplant_vit.onnx"
SAVEDMODEL_DIR = BUILD / "saved_model"
TFLITE_OUT = BUILD / "houseplant_vit_v1.tflite"
LABELS_OUT = BUILD / "houseplant_vit_labels.txt"

IMG_SIZE = 224
NUM_CALIBRATION_IMAGES = 40


class NormalizedViT(nn.Module):
    """Wraps the HF ViT so the exported graph accepts raw [0,255] NCHW pixels
    and emits softmax probabilities -- i.e. everything TfliteRunner's uint8
    path assumes is already true of the model, is actually true of the model.
    """

    def __init__(self, hf_model: AutoModelForImageClassification, mean: list[float], std: list[float]):
        super().__init__()
        self.hf_model = hf_model
        # Register as buffers (not parameters) so they export as constants,
        # not learnable weights, and move with .to(device) for free.
        self.register_buffer("mean", torch.tensor(mean).view(1, 3, 1, 1))
        self.register_buffer("std", torch.tensor(std).view(1, 3, 1, 1))

    def forward(self, pixel_values_0_255: torch.Tensor) -> torch.Tensor:
        # pixel_values_0_255: (N, 3, 224, 224) float32, raw byte range [0, 255]
        x = pixel_values_0_255 / 255.0
        x = (x - self.mean) / self.std
        logits = self.hf_model(pixel_values=x).logits
        return torch.softmax(logits, dim=-1)


def export_onnx() -> dict[int, str]:
    print(f"Loading {MODEL_ID} ...")
    processor = AutoImageProcessor.from_pretrained(MODEL_ID)
    model = AutoModelForImageClassification.from_pretrained(MODEL_ID)
    model.eval()

    # See module docstring: LayerNorm's real eps (1e-12) underflows to exactly
    # 0 under INT8 quantization, which crashes TFLite's DIV kernel on every
    # input (confirmed via a real conversion run). 1e-3 is still negligible
    # for numerical stability but survives INT8 rounding.
    layernorm_count = 0
    for module in model.modules():
        if isinstance(module, nn.LayerNorm):
            module.eps = 1e-3
            layernorm_count += 1
    print(f"Bumped eps to 1e-3 on {layernorm_count} LayerNorm modules (quantization safety)")

    mean = processor.image_mean
    std = processor.image_std
    size = processor.size.get("height") or processor.size.get("shortest_edge") or IMG_SIZE
    print(f"Processor config: mean={mean} std={std} size={size}")
    if size != IMG_SIZE:
        print(f"WARNING: model's native size is {size}, not {IMG_SIZE} -- update IMG_SIZE.")

    id2label = {int(k): v for k, v in model.config.id2label.items()}
    print(f"{len(id2label)} labels")

    wrapped = NormalizedViT(model, mean, std)
    wrapped.eval()

    dummy = torch.randint(0, 256, (1, 3, IMG_SIZE, IMG_SIZE), dtype=torch.float32)
    torch.onnx.export(
        wrapped,
        dummy,
        str(ONNX_PATH),
        input_names=["pixel_values_0_255"],
        output_names=["probabilities"],
        opset_version=17,
        dynamic_axes=None,  # fixed batch size 1 -- simplest/most reliable for onnx2tf
    )
    onnx.checker.check_model(str(ONNX_PATH))
    print(f"Wrote {ONNX_PATH}")
    return id2label


CALIBRATION_NPY = BUILD / "calibration_data.npy"


def fetch_calibration_images() -> np.ndarray:
    """Real photographic content for INT8 calibration statistics.

    Returns an (N, H, W, C) float32 array normalized to [0, 1] -- TF's
    SavedModel layout is NHWC (not ONNX's NCHW). `convert_to_tflite`'s
    representative_dataset multiplies by 255 to get back to the raw
    [0, 255] range our graph expects as input (it does its own mean/std
    normalization internally, not the caller).

    NOTE: these are generic stock photos (picsum.photos), not plant-specific.
    Calibration mainly needs realistic activation-range statistics, which
    generic photos provide reasonably well for a ViT's early layers, but this
    is a known simplification -- if the on-device accuracy-floor test
    (HouseplantVitCalibrationTest, tracked separately) comes in low, swap this
    for real leaf/houseplant photos and re-run the conversion.
    """
    from PIL import Image

    images = []
    print(f"Fetching {NUM_CALIBRATION_IMAGES} calibration images...")
    for i in range(NUM_CALIBRATION_IMAGES):
        url = f"https://picsum.photos/seed/floracare{i}/{IMG_SIZE}/{IMG_SIZE}"
        with urllib.request.urlopen(url, timeout=15) as resp:
            img = Image.open(io.BytesIO(resp.read())).convert("RGB")
        arr = np.array(img, dtype=np.float32) / 255.0  # HWC, [0,1]
        images.append(arr)
    print(f"Fetched {len(images)} calibration images")
    return np.stack(images, axis=0)  # (N, H, W, C)


def export_saved_model() -> None:
    """Get a plain TF SavedModel from onnx2tf -- no quantization involved.

    See module docstring: onnx2tf's own built-in INT8 quantization is
    confirmed broken twice over, so it's only used here for the ONNX -> TF
    graph translation. `flatbuffer_direct_output_saved_model=True` makes it
    additionally emit a standard SavedModel (needs the `tf_keras` package).
    """
    import onnx2tf

    print("Running onnx2tf (ONNX -> plain TF SavedModel, no quantization)...")
    onnx2tf.convert(
        input_onnx_file_path=str(ONNX_PATH),
        output_folder_path=str(SAVEDMODEL_DIR),
        flatbuffer_direct_output_saved_model=True,
        output_integer_quantized_tflite=False,
        non_verbose=True,
    )
    if not (SAVEDMODEL_DIR / "saved_model.pb").exists():
        raise RuntimeError(f"onnx2tf did not produce a SavedModel in {SAVEDMODEL_DIR}")


def convert_to_tflite(calibration_images: np.ndarray) -> None:
    import tensorflow as tf

    export_saved_model()

    np.save(CALIBRATION_NPY, calibration_images)
    print(f"Wrote {CALIBRATION_NPY} {calibration_images.shape}")

    # Full INT8 (int8 weights + int8 activations) crashes at runtime: this
    # ViT's GELU/Erf approximation includes a "1 / (1 + data_dependent_value)"
    # division, and INT8's coarse 256-level resolution makes it possible for
    # some element of that data-dependent denominator to legitimately quantize
    # to exactly 0 -- reproduced with both random noise and real images.
    # INT16 activations don't work around it either -- TFLite's 16x8
    # quantizer flatly refuses to quantize DIV (and CAST/SIGN/NOT_EQUAL, also
    # part of this same Erf approximation) at all, a hard API limitation, not
    # a precision one.
    #
    # Dynamic range quantization sidesteps this entirely: only weights get
    # compressed to INT8; every activation (including this DIV's denominator)
    # is computed in genuine float32 at inference time, so it can never round
    # to a quantized zero. No representative_dataset needed either, since
    # there's no activation range left to calibrate. Weights are the large
    # majority of a transformer's size, so this keeps most of the compression
    # win with none of the activation-quantization fragility. Float32 I/O
    # matches TfliteRunner's other already-supported path (confirmed via
    # TfliteRunner.kt:141-142, which reads dtype from the model itself, no
    # hardcoded per-model assumption).
    print("Quantizing with dynamic range quantization (INT8 weights, float32 activations + I/O)...")
    converter = tf.lite.TFLiteConverter.from_saved_model(str(SAVEDMODEL_DIR))
    converter.optimizations = [tf.lite.Optimize.DEFAULT]

    tflite_model = converter.convert()
    TFLITE_OUT.write_bytes(tflite_model)
    print(f"Wrote {TFLITE_OUT} ({TFLITE_OUT.stat().st_size / 1e6:.1f} MB)")


def verify_tflite(id2label: dict[int, str]) -> None:
    import tensorflow as tf

    interpreter = tf.lite.Interpreter(model_path=str(TFLITE_OUT))
    interpreter.allocate_tensors()
    in_details = interpreter.get_input_details()[0]
    out_details = interpreter.get_output_details()[0]

    print(f"Input:  dtype={in_details['dtype'].__name__} shape={in_details['shape']}")
    print(f"Output: dtype={out_details['dtype'].__name__} shape={out_details['shape']}")

    assert in_details["dtype"] == np.float32, (
        f"input dtype is {in_details['dtype']}, not float32 -- "
        "TfliteRunner's float32 path expects this; if this model is uint8 again, "
        "the DIV-by-zero fix in convert_to_tflite() may have been reverted."
    )
    assert out_details["dtype"] == np.float32, (
        f"output dtype is {out_details['dtype']}, not float32."
    )

    # Sanity-check a real forward pass: raw [0,255] pixel values in (float32,
    # matching what the graph's own first op expects), output should look
    # like a probability distribution (sums to ~1) since softmax is baked in.
    sample = np.random.randint(0, 256, size=in_details["shape"]).astype(np.float32)
    interpreter.set_tensor(in_details["index"], sample)
    interpreter.invoke()
    probs = interpreter.get_tensor(out_details["index"])[0]
    total = probs.sum()
    print(f"Output sums to {total:.4f} (expect ~1.0 if softmax was baked in correctly)")
    top_idx = int(probs.argmax())
    print(f"Top class on random noise input: {id2label.get(top_idx, '?')} ({probs[top_idx]:.4f})")


def write_labels(id2label: dict[int, str]) -> None:
    with LABELS_OUT.open("w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["id", "name"])
        for idx in sorted(id2label):
            writer.writerow([idx, id2label[idx]])
    print(f"Wrote {LABELS_OUT}")


def main() -> None:
    id2label = export_onnx()
    calibration_images = fetch_calibration_images()
    convert_to_tflite(calibration_images)
    verify_tflite(id2label)
    write_labels(id2label)
    print("\nDone. Copy these into the app:")
    print(f"  {TFLITE_OUT} -> app/src/main/assets/ml/houseplant_vit_v1.tflite")
    print(f"  {LABELS_OUT} -> app/src/main/assets/ml/houseplant_vit_labels.txt")


if __name__ == "__main__":
    main()
