# A-6 — Perenual Species Lookup (Plan)

Status: approved 2026-04-24
Branch: `main` (single-commit-per-step sequencing; no feature branch yet)
Decisions locked: Solution A · D1=Option A (Toxicity NONE + disclose) · D2=Option B (domain `ConfidenceThresholds`) · D3=every low-confidence pick · D4=new `species.imageUrl`, Hero prefers plant photo · D5=deferred

---

## 1. Goal

When AIY Vision Plants V1 top-1 confidence < `0.25`, enrich the selected prediction with real taxonomy + care fields from the Perenual API before writing the plant row. Cache under a stale-while-revalidate policy (7 d fresh / 90 d usable). Fail gracefully offline — silent fallback to the existing synth path.

## 2. Non-Goals

- No weather integration (A-5, next ticket).
- No re-enrichment of already-saved stub plants on PlantDetail open (D5 deferred).
- No UI polish on PlantDetail beyond surfacing fields that the enriched `Species` now carries.
- No Trefle or multi-provider plumbing.

## 3. State machine (identify flow)

```
Ready → Capturing → Classifying → Picker(lowConfidence?) → [user taps Choose]
    ├─ high-confidence or cached Perenual hit → Naming(selectedLabel, predictions, selectedSpeciesId) → Saved
    └─ low-confidence, no cached hit → Enriching(selectedLabel, predictions)
           ├─ Fresh/Stale           → Naming(…, selectedSpeciesId = perenual.id)
           ├─ NotFound/Offline      → Naming(…, selectedSpeciesId = synth.id)  (silent)
```

UI: `Enriching` renders the sheet with a dimmed top-3 list and a centred "Looking up care info…" spinner + compact text. No toast, no full-screen overlay. Cancel button dismisses back to `Picker` (cancels the coroutine).

## 4. Build order (one commit per step, tests green between each)

### Step 1 — Domain: `ConfidenceThresholds` + `Species` extension
- New file `domain/model/ConfidenceThresholds.kt` with `const val LOW_CONFIDENCE = 0.25f`.
- Extend `Species` with nullable `provider: String = "local"`, `providerSpeciesId: String?`, `fetchedAt: Instant?`, `family: String?`, `genus: String?`, `imageUrl: String?`.
- Update `IdentifyViewModel.LOW_CONFIDENCE_THRESHOLD` and `ResolveOrCreateSpeciesUseCase` to import from the new file.
- No behavior change yet. All existing tests compile + pass.

### Step 2 — Room schema v1→v2
- Extend `SpeciesEntity` to match the new `Species` fields (nullable defaults).
- New `data/local/migrations/Migrations.kt` with `MIGRATION_1_2` (ALTER TABLE + index on `providerSpeciesId`).
- Bump `FloraCareDatabase` to `version = 2`, add `.addMigrations(MIGRATION_1_2)` in `DatabaseModule`.
- Update `Mappers.kt` to round-trip new fields.
- Add `SpeciesDao.findByProviderId(provider, pid)` and `markFetched(id, at)` query methods.
- Commit generated `app/schemas/com.floracare.app.data.local.FloraCareDatabase/2.json`.
- Existing tests pass (fields default null).

### Step 3 — Perenual transport layer
Files (all new, under `data/remote/perenual/`):
- `PerenualApi.kt` — Retrofit interface with `GET species-list?q=` and `GET species/details/{id}`.
- `PerenualDto.kt` — `@JsonClass(generateAdapter=true)` DTOs (search item, details response, image, hardiness).
- `PerenualAuthInterceptor.kt` — appends `?key=` from `@Named("perenualKey")`.
- `RedactingHttpLogger.kt` — `HttpLoggingInterceptor` subclass that regex-replaces `key=[^&]+` → `key=***` in logs. Debug-only.
- `PerenualRemoteDataSource.kt` — suspend wrappers returning `RemoteResult<T>` (Success/Empty/RateLimited/Network/Http). Internal to this package.
- `PerenualMapper.kt` — DTO → domain `Species` with fixed tables (watering 3/7/14/21 days, sunlight ordinal → LightNeed, USDA → min temp with 10 °C indoor floor, toxicity NONE unless explicit 1). HTML strip on description. Stable id `sp-perenual-${providerSpeciesId}`.

DI changes in `NetworkModule.kt`:
- New `@Provides @Singleton @Named("perenualClient") OkHttpClient` with `PerenualAuthInterceptor` + redacting logger.
- New `@Provides @Singleton @Named("perenualRetrofit") Retrofit` at `https://perenual.com/api/`.
- `@Provides @Named("perenualKey") fun perenualKey(): String = BuildConfig.PERENUAL_KEY`.
- Split existing unnamed `provideWeatherRetrofit` into `@Named("weatherRetrofit")` to avoid DI clash.
- Update `WeatherApi` provider (and any consumer) to use the weather-named Retrofit — verify the sole consumer is our not-yet-wired weather code, otherwise rename minimally.

Unit test: `PerenualMapperTest` — covers watering/sunlight/hardiness/toxicity/HTML-strip/fallbacks (6 cases).

### Step 4 — `SpeciesRepository` + impl
- New `domain/repository/SpeciesRepository.kt`:
  ```kotlin
  interface SpeciesRepository {
      suspend fun lookup(scientificName: String, commonNameHint: String? = null): SpeciesLookupResult
  }
  sealed interface SpeciesLookupResult {
      data class Fresh(val species: Species) : SpeciesLookupResult
      data class Stale(val species: Species, val reason: StaleReason) : SpeciesLookupResult
      data object NotFound : SpeciesLookupResult
      data class Offline(val cached: Species?) : SpeciesLookupResult
  }
  enum class StaleReason { REMOTE_UNAVAILABLE, RATE_LIMITED, TIMEOUT }
  ```
- New `data/repository/SpeciesRepositoryImpl.kt` — SWR logic using `SpeciesDao` + `PerenualRemoteDataSource`. TTL constants `FRESH_DAYS = 7`, `USABLE_DAYS = 90`. `runCatching` wrapper rethrows `CancellationException`.
- Bind in `RepositoryModule`.
- JVM unit test `SpeciesRepositoryImplTest` with `FakePerenualRemoteDataSource` + in-memory `FakeSpeciesDao`. Cases: fresh cache hit (no remote call), stale → refresh, search Empty → NotFound, 429 → RateLimited/Stale, IOException → Offline.

### Step 5 — `SpeciesLookupUseCase`
- New `domain/usecase/SpeciesLookupUseCase.kt`: thin delegate with blank-guard.
- Unit test `SpeciesLookupUseCaseTest` — 3 cases (blank throws, delegates to repo, returns result unchanged).

### Step 6 — Wire `ResolveOrCreateSpeciesUseCase`
- Add `lookup: SpeciesLookupUseCase` constructor param.
- New signature: `suspend operator fun invoke(predictedLabel, topConfidence, commonNameHint)`. Old callers pass `topConfidence` from `predictions.first().confidence`.
- Logic (from architect §9): cache-hit first, then if `topConfidence < LOW_CONFIDENCE` try `lookup()`, then synth fallback.
- Idempotency preserved via stable `sp-perenual-${pid}` id and existing scientific-name cache lookup.
- Update `ResolveOrCreateSpeciesUseCaseTest` — migrate existing 5 tests to the new signature (no default param), add 4 new: low-conf + Fresh → perenual id; low-conf + NotFound → synth; low-conf + Offline → synth; high-conf → no remote call (assert fake `SpeciesLookupUseCase` was not invoked).

### Step 7 — Identify flow wiring (`Enriching` state)
- Extend `IdentifyUiState`: `data class Enriching(val selectedLabel: String, val predictions: List<Prediction>) : IdentifyUiState`.
- `IdentifyViewModel.onPredictionSelected` — if `current.lowConfidence` AND the prediction has no cached species → transition to `Enriching`, kick off `viewModelScope.launch { lookup(prediction.label) }`, then hand off to `Naming` regardless of outcome (Fresh/Stale vs silent fallback to synth). Expose cancel via a new `onEnrichingCancelled()` that restores `Picker`.
- Call site passes `topConfidence = prediction.confidence`.
- `IdentifyScreen` — add `when` branch for `Enriching` rendering the sheet with a dimmed list + "Looking up care info…" row + Cancel button.
- `IdentifyViewModelTest` — 2 new cases: low-conf pick → Enriching → Naming with Perenual id; low-conf pick + Offline → Enriching → Naming with synth id (no error shown).
- Also add `AddPlantManualViewModel` minor tweak: when `ResolveOrCreateSpeciesUseCase` is called from the submit path, pass `topConfidence = 0f` (manual entry always treated as low-confidence, triggers Perenual enrichment). No UI state change needed — existing `saving=true` spinner covers the latency.

### Step 8 — Hero uses species imageUrl fallback
- `PlantDetailScreen.Hero` reads `plant.coverPhotoUri ?: species.imageUrl`; renders a Coil `AsyncImage` when non-null, falls back to the existing accent Box otherwise.
- No new VM plumbing — `PlantDetailUiState.Ready` already carries `species`.
- Add trivial Coil `ImageLoader` reuse from `coil-compose`.

### Step 9 — Security polish
- Add `app/src/main/res/xml/network_security_config.xml` with `cleartextTrafficPermitted="false"` base-config.
- Reference from `AndroidManifest.xml` `<application android:networkSecurityConfig="@xml/network_security_config">`.
- In release builds require `BuildConfig.PERENUAL_KEY.isNotBlank()`; in debug allow blank and have `PerenualRemoteDataSource` short-circuit to `RemoteResult.Network` so the app runs without a key.

### Step 10 — Review + build gate
- Run `:app:testDebugUnitTest` — expect all suites green, new tests added.
- Run `kotlin-reviewer` agent over the diff (scoped to new + modified files).
- Run `code-reviewer` agent for cross-cutting concerns (DI wiring, migration, security).
- Update `~/.claude/projects/-home-lugat/memory/floracare_progress.md` with shipped state.

## 5. Files touched (summary)

**Created (13)**
- `domain/model/ConfidenceThresholds.kt`
- `domain/repository/SpeciesRepository.kt`
- `domain/usecase/SpeciesLookupUseCase.kt`
- `data/local/migrations/Migrations.kt`
- `data/remote/perenual/PerenualApi.kt`
- `data/remote/perenual/PerenualDto.kt`
- `data/remote/perenual/PerenualAuthInterceptor.kt`
- `data/remote/perenual/RedactingHttpLogger.kt`
- `data/remote/perenual/PerenualRemoteDataSource.kt`
- `data/remote/perenual/PerenualMapper.kt`
- `data/repository/SpeciesRepositoryImpl.kt`
- `res/xml/network_security_config.xml`
- `app/schemas/com.floracare.app.data.local.FloraCareDatabase/2.json` (generated)

**Modified (10)**
- `domain/model/Species.kt`
- `data/local/Entities.kt` · `Daos.kt` · `Mappers.kt` · `FloraCareDatabase.kt`
- `data/repository/PlantRepositoryImpl.kt` (mapper signatures only)
- `domain/usecase/ResolveOrCreateSpeciesUseCase.kt`
- `di/NetworkModule.kt` · `di/RepositoryModule.kt` · `di/DatabaseModule.kt`
- `ui/feature/identify/IdentifyUiState.kt` · `IdentifyViewModel.kt` · `IdentifyScreen.kt`
- `ui/feature/addplant/AddPlantManualViewModel.kt`
- `ui/feature/plantdetail/PlantDetailScreen.kt`
- `AndroidManifest.xml`

**Test files (4 new + 3 updated)**
- New: `PerenualMapperTest`, `SpeciesRepositoryImplTest`, `SpeciesLookupUseCaseTest`, `FakeSpeciesRepository` + `FakePerenualRemoteDataSource`
- Updated: `ResolveOrCreateSpeciesUseCaseTest`, `IdentifyViewModelTest`, `FakePlantRepository` (mapper-only)

## 6. Risks / open-after-ship

- **Perenual free-tier rate cap (100/day)** — real cap only matters under rapid repeated captures of distinct species; 7 d fresh TTL means common houseplants collapse to cache after first hit. Mitigation sufficient.
- **`hardiness` USDA → °C mapping is indoor-clamped at 10 °C.** Acceptable for v1; outdoor-first plants show a conservative range.
- **Toxicity `null` → NONE default** (D1-A) is a deliberate product call. Revisit if users report a scare.
- **No in-app API-key entry UI.** Dev-only via `local.properties`. Future ticket if we ever want user-supplied keys.

## 7. Exit criteria

- `./gradlew :app:testDebugUnitTest` — 100 % pass, new tests included.
- Manual smoke on device: snake_plant fixture (low-confidence) → Enriching → Naming with Perenual-enriched species; conifer fixture (high-confidence) → Naming directly, no network call.
- Airplane-mode smoke: snake_plant → Enriching → Naming with synth fallback, no error UI.
- Memory progress note updated with shipped state.
