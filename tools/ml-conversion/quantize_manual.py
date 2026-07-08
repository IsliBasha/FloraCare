"""Manual INT8 quantization, bypassing onnx2tf's built-in calibration pipeline.

onnx2tf's "flatbuffer_direct" fast path does its own internal INT8
quantization/calibration, and it's producing a broken result for our
output tensor (verified: dequantized softmax sums to ~23 instead of ~1,
and both the input and output tensors end up with quantization params
that only make sense if no real calibration data was actually used).

This script instead asks onnx2tf to ALSO emit a plain TensorFlow
SavedModel (flatbuffer_direct_output_saved_model=True), then drives
TensorFlow's own, standard, well-documented TFLiteConverter INT8
quantization directly ourselves -- same representative_dataset formula
already verified correct in isolation, but now under our own control
and inspection instead of onnx2tf's internal wiring.
"""

from __future__ import annotations

import pathlib

import numpy as np
import onnx2tf
import tensorflow as tf

HERE = pathlib.Path(__file__).parent
BUILD = HERE / "build"
ONNX_PATH = BUILD / "houseplant_vit.onnx"
CALIBRATION_NPY = BUILD / "calibration_data.npy"
SAVEDMODEL_EXPORT_DIR = BUILD / "saved_model_export"
OUT_TFLITE = BUILD / "houseplant_vit_v1_manual_int16act.tflite"

calib_data = np.load(CALIBRATION_NPY)  # (N, 224, 224, 3), pre-scaled to [0, 1]

if OUT_TFLITE.exists():
    print(f"Reusing already-produced {OUT_TFLITE} -- skipping steps 1-2.")
else:
    print("Step 1: asking onnx2tf to also emit a plain TF SavedModel...")
    onnx2tf.convert(
        input_onnx_file_path=str(ONNX_PATH),
        output_folder_path=str(SAVEDMODEL_EXPORT_DIR),
        flatbuffer_direct_output_saved_model=True,
        output_integer_quantized_tflite=False,
        non_verbose=True,
    )

    pb_path = SAVEDMODEL_EXPORT_DIR / "saved_model.pb"
    print(f"saved_model.pb exists: {pb_path.exists()}")
    if not pb_path.exists():
        print("No SavedModel produced -- listing directory contents instead:")
        for p in SAVEDMODEL_EXPORT_DIR.rglob("*"):
            print(" ", p)
        raise SystemExit(1)

    print("\nStep 2: quantizing it ourselves with plain TFLiteConverter...")

    def representative_dataset():
        for i in range(calib_data.shape[0]):
            raw_pixels = calib_data[i : i + 1] * 255.0  # back to real [0, 255] range
            yield [tf.cast(raw_pixels, tf.float32)]

    converter = tf.lite.TFLiteConverter.from_saved_model(str(SAVEDMODEL_EXPORT_DIR))
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.representative_dataset = representative_dataset
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.EXPERIMENTAL_TFLITE_BUILTINS_ACTIVATIONS_INT16_WEIGHTS_INT8,
    ]
    converter.inference_input_type = tf.uint8
    converter.inference_output_type = tf.uint8

    tflite_model = converter.convert()
    OUT_TFLITE.write_bytes(tflite_model)
    print(f"Wrote {OUT_TFLITE} ({OUT_TFLITE.stat().st_size / 1e6:.1f} MB)")

print("\nStep 3: verifying...")
interp = tf.lite.Interpreter(model_path=str(OUT_TFLITE))
interp.allocate_tensors()
in_details = interp.get_input_details()[0]
out_details = interp.get_output_details()[0]
print(f"Input:  dtype={in_details['dtype'].__name__} shape={in_details['shape']} "
      f"quant={in_details['quantization']}")
print(f"Output: dtype={out_details['dtype'].__name__} shape={out_details['shape']} "
      f"quant={out_details['quantization']}")

scale, zero_point = out_details["quantization"]

real_image = (calib_data[0:1] * 255.0).astype(np.uint8)
interp.set_tensor(in_details["index"], real_image)
interp.invoke()
raw_out2 = interp.get_tensor(out_details["index"])[0]
dequant2 = (raw_out2.astype(np.float32) - zero_point) * scale
print(f"Dequantized output on a real calibration image sums to {dequant2.sum():.4f} (expect ~1.0)")

try:
    sample = np.random.randint(0, 256, size=in_details["shape"], dtype=np.uint8)
    interp.set_tensor(in_details["index"], sample)
    interp.invoke()
    raw_out = interp.get_tensor(out_details["index"])[0]
    dequant = (raw_out.astype(np.float32) - zero_point) * scale
    print(f"Dequantized output on random noise sums to {dequant.sum():.4f} (expect ~1.0)")
except RuntimeError as e:
    print(f"Random noise input crashed the interpreter: {e}")
