# FloraCare — scaffold handoff

## What's fully working

- **Gradle project**: KTS + version catalog (`gradle/libs.versions.toml`), Kotlin 2.0, AGP 8.7, KSP, Hilt, Room, Compose BOM.
- **Adaptive care engine**: `ComputeNextCareTaskUseCase` — pure function with heat / rain-outdoor / humidity / damp-streak modifiers and `[base/2, base*2]` clamp. Four RED→GREEN JUnit tests pass.
- **Theme system**: forest/cream/terracotta/sage palette, Fraunces + Plus Jakarta Sans via Google Fonts, CompositionLocals for spacing and accents, dark + light.
- **Room database**: 7 entities (Plant, Species, CareTask, CareLog, JournalEntry, DiagnosisResult, WeatherSnapshot), DAOs, TypeConverters for `Instant` + all enums, `DatabaseSeeder` that inserts 3 plants + species on first launch.
- **Navigation**: type-safe Navigation Compose with `kotlinx.serialization` routes for all 9 screens.
- **Plant list**: grid of cards with next-task accent badge, FAB, hero header with count + dues.
- **Plant detail**: hero surface, vitals chips, upcoming care list, journal strip, Diagnose + Journal CTAs.
- **ML classifier scaffolding**: `SpeciesClassifier` / `DiseaseClassifier` interfaces + TFLite-backed implementations that gracefully fall back to mock predictions when the model assets are missing.
- **Glance widget**: `TodayTasksWidget` stub receiver wired in manifest.
- **Notifications**: per-care-type channels created at startup, `NotificationActionReceiver` skeleton with `ACTION_MARK_DONE` + `ACTION_SNOOZE_2D`.
- **Hilt wiring**: Application class, `DatabaseModule`, `NetworkModule`, `RepositoryBindingModule`, `UseCaseModule`. `DailyCareScheduler` annotated `@HiltWorker`.

## What's stubbed and needs real implementation

- **Onboarding, AddPlant, Identify, Diagnose, Journal, Dashboard, Settings** screens are `PlaceholderScreen` placeholders. Navigation wires them up.
- **Widget** shows a header only — no live tasks yet.
- **Notification dispatch**: channel setup is in, but `DailyCareScheduler` doesn't build notifications yet.
- **WorkManager registration**: the worker is declared but not scheduled at app start.
- **CameraX pipeline**: dependencies wired; capture → bitmap → classifier flow not yet implemented.
- **TFLite inference**: `TfliteModelLoader` detects presence; real `Interpreter` / GPU delegate setup pending.
- **Weather repository**: `OneCall` Retrofit DTOs present; no fetch + cache call site yet.
- **Species lookup**: no Perenual/Trefle client yet.
- **Dashboard charts**: Vico dependency included but no chart yet.

## Suggested ticket breakdown

### Person A — Data & Intelligence

1. Implement `NotificationDispatcher` + notification builders for each `CareChannel`.
2. Flesh out `DailyCareScheduler`: iterate all plants, call use case, build + post notifications, enqueue next run at 07:00 local.
3. Register `DailyCareScheduler` (periodic) on app first launch; on boot if lost.
4. Complete `NotificationActionReceiver`: enqueue a worker that persists `MARK_DONE` / `SNOOZE_2D` to the care task.
5. Weather fetch + cache: repository call site using `WeatherApi.oneCall`, write-through to Room.
6. Perenual/Trefle species lookup; stale-while-revalidate cache in Room.

### Person B — ML & Camera

1. Real TFLite `Interpreter` wiring with GPU delegate fallback in `TfliteModelLoader`.
2. Image pre-processing pipeline: CameraX capture → ImageProxy → `Bitmap` → resize + normalize → classifier call.
3. Identify screen full flow: camera preview → confirm → top-3 species picker → name plant.
4. Diagnose screen full flow: leaf capture → top-1 disease → treatment suggestion lookup.
5. Persist `DiagnosisResult` rows and surface them in plant detail.

### Person C — UI & Polish

1. Real `PlantListViewModel` backed by `PlantRepository.observePlants` + task rollups.
2. Real `PlantDetailViewModel`; replace mock copy in `PlantDetailScreen`.
3. Onboarding 3-pager with permissions ask (`POST_NOTIFICATIONS`, `CAMERA`, `ACCESS_*_LOCATION`).
4. Add-plant flow with Identify vs Manual branches.
5. Dashboard screen: Vico line chart for watering consistency, streak card, plant-of-the-month.
6. Settings screen: theme switcher, notification prefs, unit toggle, about.
7. Shared-element transitions (plant list → detail), stagger animations, leaf pull-to-refresh.
8. Widget content: list today's top 3 tasks, deep-link to plant detail.

## Next 5 highest-value tasks in priority order

1. **Wire `DailyCareScheduler` + notifications end-to-end** (person-a). Turns the adaptive engine into visible behaviour.
2. **PlantListViewModel + PlantRepository integration** (person-c). Swap mock data for real DB observation.
3. **Real TFLite inference + Identify flow** (person-b). Makes the core differentiator real.
4. **Notification action receivers persist state** (person-a). Enables "Watered" / "Snooze" without opening the app.
5. **Dashboard Vico charts + streaks** (person-c). Visible value from the logs already captured.
