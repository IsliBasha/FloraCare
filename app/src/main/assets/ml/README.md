# ML models

Drop real model files here to swap the classifier off of its mock
predictions. The loader auto-adapts to float32 **or** quantised uint8
models and to common label-file formats, so no code changes are needed.

## Recommended starting model — AIY Vision Classifier Plants V1

Google's AIY Vision Plants V1 is a MobileNet-class TFLite bundle covering
~2,100 plant species. It's a good real-world baseline while we wait for a
fine-tuned model.

### One-time download

1. Sign in to Kaggle (free account) and open
   <https://www.kaggle.com/models/google/aiy/tfLite/vision-classifier-plants-v1/3>
2. Click **"Download"** → unpack the archive. You'll get two files:
   - `*.tflite`          → rename to **`plant_species_v1.tflite`**
   - `*_labelmap.csv`    → rename to **`species_labels.txt`**
3. Drop both into this folder (`app/src/main/assets/ml/`).
4. Rebuild & reinstall (`./gradlew :app:installDebug`).

On next launch, `TfliteModelLoader.isLoaded` flips to `true` and the
Identify flow starts running real inference. No code changes needed.

## File contract

| Intent                        | Filename                   | Format |
| ----------------------------- | -------------------------- | ------ |
| Species classifier model       | `plant_species_v1.tflite`  | float32 *or* uint8 TFLite; 224×224×3 input |
| Species labels                | `species_labels.txt`       | one label per line, **or** CSV/TSV with optional header |
| Disease classifier model      | `leaf_disease_v1.tflite`   | same rules as above |
| Disease labels                | `disease_labels.txt`       | same rules as above |

### Labels parser

`parseLabels` handles:
- Plain one-per-line text.
- `id,name` / `index,label` CSV (header auto-skipped).
- Multi-column CSV — the last *non-numeric, non-`/m/…`* cell becomes the
  user-facing label. That matches AIY Vision's label maps out of the box.

### Runtime dtype adaptation

`TfliteRunner` introspects the model's input/output tensors:
- **float32 input** → floats in `[0, 1]` sent as-is.
- **uint8 input**   → the same `[0, 1]` floats are rescaled to `[0, 255]`
  and written as bytes.
- **uint8 output**  → dequantised to floats using the tensor's
  `quantizationParams()` scale + zeroPoint before `parseTopK`.

This means a quantised Aiy Vision bundle "just works", and so does a
full-float model you export from TFLite Model Maker.

## Notes

- `.tflite` files are excluded from APK compression via
  `noCompress += "tflite"` in `app/build.gradle.kts`.
- GPU delegate is attempted first; CPU fallback is automatic.
- Models above ~50 MB inflate the APK noticeably — prefer the quantised
  variant when the Kaggle model card offers both.
