# FloraCare

An AI-powered plant-care companion for Android. University group project (3 people).

FloraCare is an **adaptive** plant-care journal: schedules aren't static cron jobs —
they shift with local weather, your recent care logs, and species-specific rules.
Photo-based species ID and disease diagnosis run fully on-device via TensorFlow Lite.

## Quick start

```bash
# 1. Install the Android toolchain (JDK 17+, Android SDK 35).
#    Android Studio Ladybug or newer is the easiest path; cmdline-tools works too.

# 2. Put your keys in local.properties (file is gitignored):
echo "OPENWEATHER_KEY=your_key" >> local.properties
echo "PERENUAL_KEY=your_key" >> local.properties

# 3. Build & run.
./gradlew :app:installDebug

# 4. Run unit tests.
./gradlew :app:testDebugUnitTest
```

The app launches straight into the plant list with seeded sample data — no
network and no permissions required on first boot.

## Architecture

Clean architecture in three layers, MVVM inside the UI layer:

```
com.floracare.app/
├── data/                 // Room, Retrofit, TFLite, WorkManager workers, repository impls
├── domain/               // Pure Kotlin models, repository interfaces, use cases
└── ui/                   // Compose + Material 3, navigation, per-feature folders
```

Dependencies point inward only: `ui → domain ← data`. The domain layer has no Android
imports, which is why `ComputeNextCareTaskUseCase` is trivially unit-testable.

### Adaptive care engine

`ComputeNextCareTaskUseCase` is a pure function. Given a plant, its species,
recent logs and weather, it returns the next scheduled watering task.

Modifiers against `species.waterFrequencyDays`:

| Signal                                              | Effect                |
| --------------------------------------------------- | --------------------- |
| avg temp last 3 days > 28°C                         | shorten by 20%        |
| total rain last 3 days > 10 mm AND location outdoor | extend by 50%         |
| avg humidity < 30%                                  | shorten by 15%        |
| last 2 care logs had `soilMoistureNote = DAMP`      | extend by 1 day       |

Final interval is clamped to `[waterFrequencyDays / 2, waterFrequencyDays * 2]`.
Four unit tests in `app/src/test/.../ComputeNextCareTaskUseCaseTest.kt` cover
each rule plus the clamp.

## ML models

TFLite models are **not** shipped. The classifiers fall back to mock predictions
whenever the model files are missing, so the flow stays end-to-end testable
on day one. See `app/src/main/assets/ml/README.md` for the files to drop in.

## API keys

Both OpenWeather and Perenual/Trefle keys load from `local.properties` (or env vars
with the same name) into `BuildConfig`. Never hardcode keys; `local.properties` is
already in `.gitignore`.

## Module division (3-person team)

| Owner     | Focus                                          | Areas                                                               |
| --------- | ---------------------------------------------- | ------------------------------------------------------------------- |
| person-a  | **Data & Intelligence**                        | `data/local`, `data/remote`, `data/worker`, `data/repository`, adaptive engine, notifications |
| person-b  | **ML & Camera**                                | `data/ml`, identify & diagnose flows, CameraX pipeline              |
| person-c  | **UI & Polish**                                | `ui/theme`, `ui/components`, all `ui/feature/*` screens, widget, onboarding |

`TODO(person-a):`, `TODO(person-b):`, `TODO(person-c):` markers in code make
handoffs explicit.

## Testing

```bash
./gradlew :app:testDebugUnitTest          # unit tests (fast)
./gradlew :app:connectedDebugAndroidTest  # needs device/emulator
```

