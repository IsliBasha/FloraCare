"""Diagnose which tensor triggers onnx2tf's 'ValueError: negative count'.

Reuses the ONNX + calibration artifacts already produced by
convert_houseplant_vit.py's earlier stages (build/houseplant_vit.onnx,
build/calibration_data.npy) instead of re-running the slow HF export.
"""

from __future__ import annotations

import pathlib

import numpy as np

HERE = pathlib.Path(__file__).parent
BUILD = HERE / "build"
ONNX_PATH = BUILD / "houseplant_vit.onnx"
SAVEDMODEL_DIR = BUILD / "saved_model"
CALIBRATION_NPY = BUILD / "calibration_data.npy"

import onnx2tf.tflite_builder.tensor_buffer_builder as tbb
import onnx2tf.tflite_builder.model_writer as mw

_orig = tbb.build_tensors_and_buffers


def _patched(schema_tflite, tensors):
    for name, tensor in tensors.items():
        data = tensor.data
        if data is not None and not isinstance(data, (bytes, np.ndarray)):
            print(f"SUSPECT tensor name={name!r} dtype={tensor.dtype} "
                  f"shape={tensor.shape} type(data)={type(data)} data={data!r}")
            tensor.data = np.asarray(data)  # apply the real fix so we can get past this
    return _orig(schema_tflite=schema_tflite, tensors=tensors)


tbb.build_tensors_and_buffers = _patched
mw.build_tensors_and_buffers = _patched

import onnx2tf

zero = [[[[0.0, 0.0, 0.0]]]]
inv_255 = [[[[1 / 255.0, 1 / 255.0, 1 / 255.0]]]]

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
    verbosity="debug",
    non_verbose=False,
)
