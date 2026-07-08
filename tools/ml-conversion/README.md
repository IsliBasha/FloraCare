# ML conversion tooling (not shipped in the app)

One-off tooling used to convert `dima806/house-plant-image-detection` (a Hugging
Face Vision Transformer) into a TFLite model FloraCare's existing
`TfliteRunner`/`TfliteModelLoader` pipeline can run, the same way
`plant_species_v1.tflite` (AIY Vision Plants V1) already works today.

This directory is **not part of the Android build** — nothing under `assets/ml/`
depends on Python at runtime. It exists purely to reproduce the conversion if the
model needs to be re-converted (e.g. a new HF checkpoint version, or recalibrating
quantization against real photos instead of the generic stock-photo calibration
set this script currently uses — see the note in `convert_houseplant_vit.py`).

## Why this isn't a simple `tflite_convert` call

Three things needed to be handled deliberately, not left to conversion defaults —
see the module docstring in `convert_houseplant_vit.py` for the full reasoning:

1. Default TFLite full-integer quantization emits **signed int8**.
   `TfliteRunner` only handles **uint8** or float32 — the converter is told to
   force uint8 input/output explicitly.
2. The ViT's real normalization (`image_mean`/`image_std`, read from its own
   processor config, not assumed to be any particular value) is baked into the
   exported graph, so the model accepts raw `[0, 255]` pixel bytes directly —
   matching how `ImagePreprocessor.kt`'s uint8 path already feeds AIY.
3. Softmax is baked into the graph's final op, because `TfliteRunner` only
   applies its own temperature-scaled softmax calibration to float32 outputs;
   a uint8 output tensor is dequantized and trusted as-is.

## Setup

```bash
cd tools/ml-conversion
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

## Run

```bash
.venv/bin/python convert_houseplant_vit.py
```

Produces `build/houseplant_vit_v1.tflite` and `build/houseplant_vit_labels.txt`.
The script's own `verify_tflite()` step asserts the output dtypes are uint8 (not
int8) and sanity-checks that the dequantized output sums to ~1.0 (proof softmax
was actually baked in) before declaring success.

Copy the two output files into the app:

```bash
cp build/houseplant_vit_v1.tflite   ../../app/src/main/assets/ml/
cp build/houseplant_vit_labels.txt  ../../app/src/main/assets/ml/
```

## Known simplification

INT8 calibration currently uses 40 generic stock photos (picsum.photos), not
real houseplant/leaf photos. This mainly affects activation-range statistics
used during quantization, not the model's learned weights, so it's a reasonable
first pass — but if the on-device accuracy-floor test
(`HouseplantVitCalibrationTest`) comes in low, swap `fetch_calibration_images()`
for a real set of leaf photos and re-run.

## `.venv/` and intermediate artifacts

Gitignored (`.venv/`, `*.onnx`, `saved_model/`, `__pycache__/`) — multi-GB and
fully reproducible from this script, not meant to be committed.
