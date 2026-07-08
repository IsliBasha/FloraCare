"""Test whether onnx2tf's own INT8 pipeline works correctly with more
calibration images (64 instead of 40), per PINTO0309/onnx2tf#724's finding
that too few calibration images causes bloated/broken quantization.

Reuses the cached ONNX export from convert_houseplant_vit.py.
"""

from __future__ import annotations

import io
import pathlib
import urllib.request

import numpy as np
from PIL import Image

HERE = pathlib.Path(__file__).parent
BUILD = HERE / "build"
ONNX_PATH = BUILD / "houseplant_vit.onnx"
SAVEDMODEL_DIR = BUILD / "saved_model_64calib"
CALIBRATION_NPY = BUILD / "calibration_data_64.npy"
TFLITE_OUT = BUILD / "houseplant_vit_v1_64calib.tflite"

IMG_SIZE = 224
NUM_CALIBRATION_IMAGES = 64

print(f"Fetching {NUM_CALIBRATION_IMAGES} calibration images...")
images = []
for i in range(NUM_CALIBRATION_IMAGES):
    url = f"https://picsum.photos/seed/floracare64_{i}/{IMG_SIZE}/{IMG_SIZE}"
    with urllib.request.urlopen(url, timeout=15) as resp:
        img = Image.open(io.BytesIO(resp.read())).convert("RGB")
    arr = np.array(img, dtype=np.float32) / 255.0
    images.append(arr)
calibration_images = np.stack(images, axis=0)
np.save(CALIBRATION_NPY, calibration_images)
print(f"Wrote {CALIBRATION_NPY} {calibration_images.shape}")

# Same negative-scalar-buffer workaround as convert_houseplant_vit.py --
# needed regardless of calibration set size, this is bug #1, already filed
# as PINTO0309/onnx2tf#941.
import onnx2tf.tflite_builder.model_writer as mw
import onnx2tf.tflite_builder.tensor_buffer_builder as tbb

_orig = tbb.build_tensors_and_buffers


def _patched(schema_tflite, tensors):
    for tensor in tensors.values():
        if tensor.data is not None and not isinstance(tensor.data, (bytes, np.ndarray)):
            tensor.data = np.asarray(tensor.data)
    return _orig(schema_tflite=schema_tflite, tensors=tensors)


tbb.build_tensors_and_buffers = _patched
mw.build_tensors_and_buffers = _patched

import onnx2tf

zero = [[[[0.0, 0.0, 0.0]]]]
inv_255 = [[[[1 / 255.0, 1 / 255.0, 1 / 255.0]]]]

print("Running onnx2tf's own INT8 pipeline with 64 calibration images...")
onnx2tf.convert(
    input_onnx_file_path=str(ONNX_PATH),
    output_folder_path=str(SAVEDMODEL_DIR),
    output_integer_quantized_tflite=True,
    quant_type="per-channel",
    custom_input_op_name_np_data_path=[
        ["pixel_values_0_255", str(CALIBRATION_NPY), zero, inv_255],
    ],
    input_quant_dtype="uint8",
    output_quant_dtype="uint8",
    not_use_onnxsim=False,
)

candidates = sorted(SAVEDMODEL_DIR.glob("*_full_integer_quant.tflite"))
if not candidates:
    raise RuntimeError(f"No full_integer_quant.tflite found in {SAVEDMODEL_DIR}")
chosen = candidates[0]
TFLITE_OUT.write_bytes(chosen.read_bytes())
size_mb = TFLITE_OUT.stat().st_size / 1e6
print(f"\nWrote {TFLITE_OUT} ({size_mb:.1f} MB) -- compare: 40-image run was 341MB, correct is ~88MB")

import tensorflow as tf

interp = tf.lite.Interpreter(model_path=str(TFLITE_OUT))
interp.allocate_tensors()
in_details = interp.get_input_details()[0]
out_details = interp.get_output_details()[0]
print(f"Input:  dtype={in_details['dtype'].__name__} quant={in_details['quantization']}")
print(f"Output: dtype={out_details['dtype'].__name__} quant={out_details['quantization']}")

real_image = (calibration_images[0:1] * 255.0).astype(np.uint8)
interp.set_tensor(in_details["index"], real_image)
interp.invoke()
raw_out = interp.get_tensor(out_details["index"])[0]
scale, zero_point = out_details["quantization"]
dequant = (raw_out.astype(np.float32) - zero_point) * scale
print(f"Output sums to {dequant.sum():.4f} on a real calibration image (expect ~1.0)")
