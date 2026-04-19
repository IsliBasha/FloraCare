# ML models

Drop the following files in this folder to enable real inference. While they're missing,
`TfliteSpeciesClassifier` and `TfliteDiseaseClassifier` return mock predictions so the
full UI flow is still testable on day one.

## Required files

| File                      | Source style              | Purpose                         |
| ------------------------- | ------------------------- | ------------------------------- |
| `plant_species_v1.tflite` | PlantNet / iNaturalist    | Species identification from a leaf photo. |
| `species_labels.txt`      | One label per line        | Output index → label map.       |
| `leaf_disease_v1.tflite`  | PlantVillage              | Disease classification.         |
| `disease_labels.txt`      | One label per line        | Output index → label map.       |

## Expected input shape

Both models are assumed to accept `224x224x3` RGB tensors, float32 normalised to `[0, 1]`.
If your model uses a different shape, adjust `TfliteModelLoader` and the classifier
preprocessing before inference.

## Notes

- Models are excluded from compression via `noCompress += "tflite"` in `app/build.gradle.kts`.
- GPU delegate is attempted first; CPU fallback is automatic.
- Keep label count and output tensor shape consistent with the label file.
