# Plan — Identify flow (HANDOFF task #3)

## Goal
Full camera-based species identification: permission gate → CameraX preview →
capture → TFLite top-3 → pick → name → save. Writes a `Plant` row (and a
backing `Species` row when the prediction isn't already in the DB) so the plant
list picks it up live via the reactive VM built in task #2.

## Approach — Option A

Single screen. State machine in `IdentifyViewModel`. CameraX bound via
`LifecycleCameraController` to the composable's `LifecycleOwner`. Prediction
bitmap held in the VM as `lastCapture: Bitmap?` until user saves or cancels.

## State machine

```kotlin
sealed interface IdentifyUiState {
    data object RequestPermission : IdentifyUiState
    data object PermissionDenied  : IdentifyUiState
    data object Ready             : IdentifyUiState          // preview live
    data object Capturing         : IdentifyUiState          // shutter pressed
    data object Classifying       : IdentifyUiState
    data class  Picker(val predictions: List<Prediction>) : IdentifyUiState
    data class  Naming(val selectedLabel: String)         : IdentifyUiState
    data class  Saved(val plantId: String)                : IdentifyUiState
    data class  Error(val message: String)                : IdentifyUiState
}
```

Transitions:
```
RequestPermission ──granted──► Ready
                  ──denied───► PermissionDenied
Ready          ──onShutter──► Capturing
Capturing      ──bitmap─────► Classifying
Classifying    ──topK───────► Picker
Classifying    ──empty──────► Error (no predictions / classifier off)
Picker         ──select─────► Naming
Picker         ──retake─────► Ready
Naming         ──save───────► Saved
Naming         ──cancel─────► Picker
Error          ──retry──────► Ready
```

## Files

| File | Status |
| --- | --- |
| `data/ml/BitmapCapture.kt` | **new** — `ImageProxy.toUprightBitmap()` + `imageToJpegBitmap()` helpers |
| `domain/usecase/ResolveOrCreateSpeciesUseCase.kt` | **new** — match prediction label to existing species, else create minimal Species row |
| `ui/feature/identify/IdentifyUiState.kt` | **new** — sealed state |
| `ui/feature/identify/IdentifyViewModel.kt` | **rewrite** — state machine |
| `ui/feature/identify/IdentifyScreen.kt` | **rewrite** — permission + preview + shutter + picker sheet + name dialog |
| `ui/feature/identify/components/CameraPreview.kt` | **new** — small composable wrapping `PreviewView` + controller |
| `ui/feature/addplant/AddPlantScreen.kt` | **rewrite** — two action tiles (Identify + Manual stub) |
| `ui/navigation/FloraCareNavHost.kt` | pass `onDone` from Identify back to PlantList |
| `domain/repository/PlantRepository.kt` | add `upsertSpecies(species)` + `findSpeciesByScientificName(name)` |
| `data/local/Daos.kt` | add `SpeciesDao.findByScientificName(name)` |
| `data/repository/PlantRepositoryImpl.kt` | wire new methods |
| `test/.../ResolveOrCreateSpeciesUseCaseTest.kt` | **new** — 4–5 tests |
| `test/.../IdentifyViewModelTest.kt` | **new** — state transitions |
| `test/.../FakePlantRepository.kt` (shared in tests) | leave inline per test class for now |

## Species resolution rules

For prediction label `L` (typical: scientific name like `"Monstera deliciosa"`):
1. Lookup `SpeciesDao.findByScientificName(L)` (case-insensitive).
2. If present → return its id.
3. Else synthesise minimal `Species(id = "sp-user-${UUID}", scientificName = L, commonName = L, waterFrequencyDays = 7, lightNeed = MEDIUM, humidityNeed = MEDIUM, tempMin=15f, tempMax=28f, toxicity = NONE, careNotes = "")` and upsert.
4. Either way, use the resolved id on the new `Plant`.

## CameraX choice

`LifecycleCameraController` + `PreviewView.COMPAT_MODE`. Capture via
`controller.takePicture(executor, OnImageCapturedCallback { … })`.
`ImageProxy.toBitmap()` is available since camera-core 1.3.0 — we're on 1.4.1.
Rotation corrected via `imageInfo.rotationDegrees`.

## Permission

`accompanist-permissions` is already in deps. Use `rememberPermissionState(Manifest.permission.CAMERA)`. On `PermissionDenied`, show a "Grant camera" CTA that either re-requests or opens app settings depending on `shouldShowRequestPermissionRationale`.

## Save path

On `Naming.save(nickname)`:
1. Resolve or create Species id.
2. Create `Plant(id = UUID, nickname, speciesId, locationTag = INDOOR, acquiredAt = now, coverPhotoUri = null /* MVP; wiring photo to internal storage is a follow-up */, notes = "")`.
3. `plants.upsert(plant)` → reactive VM picks it up; `onDone(plantId)` nav-pops back to list.

## Out of scope
- Saving the capture to internal storage as `coverPhotoUri` (defer; the list tile derives its accent from species id anyway).
- Retake thumbnail preview between capture and picker.
- Analytics + crashlytics hooks.

## Test matrix

**`ResolveOrCreateSpeciesUseCaseTest`**
- exact match → returns existing id
- case-insensitive match → returns existing id
- whitespace-trimmed match → returns existing id
- no match → upserts new Species with predicted name as scientific + common
- returned id is idempotent across two calls (second call finds the synthesised row)

**`IdentifyViewModelTest`**
- initial state = RequestPermission
- onPermissionGranted → Ready
- onPermissionDenied → PermissionDenied
- onCaptureStart → Capturing
- onBitmapCaptured with classifier returning 3 preds → Picker
- onBitmapCaptured with classifier returning empty → Error
- Picker.onSelect → Naming
- Naming.onCancel → Picker (predictions preserved)
- Naming.onSave → Saved(plantId); repo upsert called once
