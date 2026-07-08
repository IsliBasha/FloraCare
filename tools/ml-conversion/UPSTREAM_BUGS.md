# Upstream bugs found while converting `dima806/house-plant-image-detection` to TFLite

Found 2026-07-08 while converting a Hugging Face ViT model to a uint8 TFLite file for
FloraCare's houseplant classifier. Logged here so real GitHub issues can be filed for the
maintainers -- not filed automatically, since that's a public action.

---

## 1. CONFIRMED: `ValueError: negative count` on negative INT8 scalar constants

**Repo:** https://github.com/PINTO0309/onnx2tf
**Version:** 2.5.0 (latest on PyPI as of 2026-07-08)
**File:** `onnx2tf/tflite_builder/tensor_buffer_builder.py`, line 134, function `build_tensors_and_buffers`

### The bug

```python
if tensor.data is not None:
    b = schema_tflite["BufferT"]()
    if isinstance(tensor.data, np.ndarray):
        ...
        b.data = tensor.data.tobytes()
    else:
        b.data = tensor.data if isinstance(tensor.data, bytes) else bytes(tensor.data)  # line 134
```

When `tensor.data` is a bare NumPy scalar (e.g. `np.int8(-127)`) rather than an `np.ndarray`,
it falls into the `else` branch and hits Python's builtin `bytes(int)`, which allocates a
zero-filled buffer of that *length* rather than encoding the value. `bytes(-127)` raises
`ValueError: negative count` for any negative scalar.

### How to reproduce

Convert an ONNX graph containing INT8-quantized scalar constants with negative values to TFLite
with `output_integer_quantized_tflite=True`. In our case, this happened converting a ViT model
(`dima806/house-plant-image-detection`) whose exported graph includes per-layer GELU/Erf
activation polynomial-approximation coefficients and LayerNorm epsilon/one constants as
shape-`[1]` scalar tensors -- e.g. tensor `val_93_erf_a2` quantizes to `np.int8(-127)`.

Confirmed via a monkeypatched diagnostic dump right before the crash:
```
SUSPECT tensor name='val_93_erf_a2' dtype=INT8 shape=[1] type(data)=<class 'numpy.int8'> data=np.int8(-127)
SUSPECT tensor name='val_93_erf_minus_one' dtype=INT8 shape=[1] type(data)=<class 'numpy.int8'> data=np.int8(-127)
SUSPECT tensor name='val_93_erf_a4' dtype=INT8 shape=[1] type(data)=<class 'numpy.int8'> data=np.int8(-127)
```
(dozens more of the same pattern, one set per transformer block)

Full traceback:
```
File ".../onnx2tf/tflite_builder/model_writer.py", line 762, in _build_subgraph_tensors_and_append_buffers
    tensors, local_buffers, tensor_index_map = build_tensors_and_buffers(
File ".../onnx2tf/tflite_builder/tensor_buffer_builder.py", line 134, in build_tensors_and_buffers
    b.data = tensor.data if isinstance(tensor.data, bytes) else bytes(tensor.data)
ValueError: negative count
```

### Suggested fix

Handle NumPy scalar types (`np.generic`) the same way as `np.ndarray`, e.g.:

```python
if isinstance(tensor.data, (np.ndarray, np.generic)):
    b.data = tensor.data.tobytes()
else:
    b.data = tensor.data if isinstance(tensor.data, bytes) else bytes(tensor.data)
```

### Our workaround (not upstream-worthy, just unblocks us)

In `convert_houseplant_vit.py`, before calling `onnx2tf.convert()`, we monkeypatch
`build_tensors_and_buffers` to normalize any bare numpy scalar into a 0-d `np.ndarray` first,
so it takes the already-correct `.tobytes()` path. See `_patch_onnx2tf_negative_scalar_buffer_bug()`
in that file for the exact code and full comment.

### Checked for existing reports

Searched onnx2tf's GitHub issues (web search, 2026-07-08) for `"negative count"` and
`"tensor_buffer_builder"` -- no existing issue found matching this.

---

## 2. CONFIRMED: `flatbuffer_direct` INT8 pipeline miscalibrates input/output tensors (and barely compresses weights at all)

**Repo:** https://github.com/PINTO0309/onnx2tf
**Version:** 2.5.0
**Suspected area:** the "flatbuffer_direct" fast path's INT8 quantization/calibration handling
of `custom_input_op_name_np_data_path` (exact code location not identified -- the
`tf.lite.TFLiteConverter`-based code path in `onnx2tf.py` around line 7680 that looked like a
plausible culprit turns out to be legacy/unused by this fast path; the actual "flatbuffer_direct"
code that handles calibration wasn't located within our time budget).

### Symptom

After producing a `uint8`-in/`uint8`-out `.tflite` file via
`onnx2tf.convert(..., output_integer_quantized_tflite=True, custom_input_op_name_np_data_path=[...])`,
the model's output tensor (a softmax layer, mathematically guaranteed to sum to ~1.0 regardless
of input) instead summed to ~23.4 on a real forward pass. Isolated the graph itself as correct
first (the float32 TFLite variant produced in the same run sums to exactly 1.0 on the same input).

Both the input and output tensors ended up with `scale=0.00392157 (1/255), zero_point=128` --
consistent with a generic default `[-0.5, 0.5]`-ish range assumption, not real observed
statistics from our supplied calibration images. Confirmed our own calibration-data
reconstruction formula (`(calib_data - mean) / std`) is correct in isolation (produces real
`[0, 255]` values).

**Confirmed via clean A/B comparison:** ran the identical ONNX graph + identical calibration
data through plain `tf.lite.TFLiteConverter.from_saved_model(...)` instead (using
`flatbuffer_direct_output_saved_model=True` to get a plain SavedModel out of onnx2tf first, then
quantizing ourselves). Same data, same intent, correct result: input tensor calibrated to
`scale=1.0, zero_point=0` (exactly right for a raw `[0,255]` byte range) and output tensor to
`scale=0.00390625 (1/256), zero_point=0` (correct for a `[0,1]` softmax range). Also revealing:
the broken onnx2tf-produced file was **341MB**, nearly identical to the un-quantized float32
model's 343MB, while the correctly-calibrated file was **87.9MB** -- onnx2tf's own INT8 pipeline
appears to barely compress the model's weights at all here, not just miscalibrate one tensor.

### Our workaround

`export_saved_model()` + `convert_to_tflite()` in `convert_houseplant_vit.py`: use onnx2tf only
to translate ONNX -> a plain TF SavedModel (`flatbuffer_direct_output_saved_model=True,
output_integer_quantized_tflite=False`), then do all INT8 quantization ourselves with plain
`tf.lite.TFLiteConverter`.

### Checked for existing reports

Not searched as thoroughly as bug #1 -- worth checking onnx2tf's issues for
`custom_input_op_name_np_data_path` + `flatbuffer_direct` calibration complaints before filing.

---

## Related findings (not onnx2tf bugs, but real gotchas worth documenting)

### LayerNorm epsilon underflows to exactly 0 under INT8 activation quantization

Not a library bug -- an inherent numeric-precision fact about INT8's 256 discrete levels,
applicable to any tool quantizing a model with a very small LayerNorm eps. This model's real
eps is `1e-12` (typical for HF ViT/BERT-style models). Under full INT8 activation quantization,
this eps constant collapsed to the literal integer 0, reintroducing the exact divide-by-zero it
exists to prevent -- deterministic on every input, not an adversarial edge case. Fixed by
bumping eps to `1e-3` before ONNX export (still utterly negligible next to real activation
variance). See `export_onnx()` in `convert_houseplant_vit.py`.

### This ViT's GELU/Erf decomposition can't be INT8-activation-quantized at all

Even after the eps fix, full INT8 activation quantization still crashed with the same
`data[i] != 0` error, at a *different* DIV node: `1.0 / (1 + data_dependent_activation)`, part
of PyTorch's automatic decomposition of `torch.erf` into more primitive ops during ONNX export.
Unlike eps, this denominator is genuinely input-dependent (not a fixed constant we can bump), so
under INT8's coarse resolution some element out of ~600K in that tensor can legitimately land on
the quantized zero for some input. Tried switching to TFLite's INT16-activation mode for more
precision, but hit a hard wall instead: TFLite's 16x8 quantizer (`EXPERIMENTAL_TFLITE_BUILTINS_
ACTIVATIONS_INT16_WEIGHTS_INT8`) flatly refuses to quantize `DIV`, `CAST`, `SIGN`, or `NOT_EQUAL`
at all (`RuntimeError: Quantization to 16x8-bit not yet supported for op: 'DIV'`) -- a hard API
limitation, not something fixable by adjusting precision. Resolved by switching to *dynamic
range* quantization instead (INT8 weights only, float32 activations, no calibration needed at
all) -- sidesteps the entire class of activation-quantization issues, at the cost of a float32
model boundary instead of uint8/int8.
