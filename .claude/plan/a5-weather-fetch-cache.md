# A-5 — Weather fetch + cache (2026-04-25)

Promoted from #2 in the next-session priority list. Mirrors A-6's SWR architecture so the repository transparently fetches when cache is stale and falls back to last-known on failure.

## Existing scaffolding (don't recreate)
- `data/remote/WeatherApi.kt` — Retrofit interface + DTOs (`OneCallResponse`, `CurrentDto`, `HourlyDto`, `DailyDto`)
- `di/NetworkModule.kt` — `@Named("weatherRetrofit")` already defined
- `domain/repository/PlantRepository.kt` — `WeatherRepository { recentWeather, cache }` interface
- `data/repository/PlantRepositoryImpl.kt` — `WeatherRepositoryImpl` (local-only stub)
- `data/local/Daos.kt` — `WeatherDao { findRecent, insert }`
- `data/local/Entities.kt` + `Mappers.kt` — `WeatherSnapshotEntity` round-trip
- `domain/usecase/ComputeNextCareTaskUseCase.kt` — already accepts `recentWeather` and applies temp/rain/humidity modifiers (no change needed)
- `data/worker/DailyCareScheduler.kt` — already injects `WeatherRepository`; passes `recentWeather` to use case
- `app/build.gradle.kts` — `BuildConfig.OPENWEATHER_KEY` already wired (placeholder in `local.properties`)
- `AndroidManifest.xml` — ACCESS_COARSE_LOCATION + ACCESS_FINE_LOCATION declared
- Onboarding requests COARSE permission already

## What's missing
1. Remote data source — actual call to `WeatherApi`, with `RemoteResult` translation
2. Mapper — `OneCallResponse` → `WeatherSnapshot`
3. SWR-aware `refresh(lat, lon)` on `WeatherRepository`
4. Repository impl that calls remote, persists, returns latest snapshot, falls back to cache
5. Location source — gets the user's coordinates via Android `LocationManager` (no Play Services dep)
6. Wiring in `DailyCareScheduler` so weather actually populates before each daily run
7. DI bindings

## Build steps

### Step 1 — Transport (Mapper + RemoteDataSource)
**Files**
- `data/remote/weather/WeatherMapper.kt` — pure function `OneCallResponse.toSnapshot(lat, lon, fetchedAt)` → `WeatherSnapshot` deriving id from `"wx-${lat}-${lon}-${dt}"`. Uses `current.dt` as `recordedAt`. `rainMm` from `hourly[0].rain["1h"]` if present else 0. `uvIndex` from `current.uvi`.
- `data/remote/weather/WeatherRemoteDataSource.kt` — interface `suspend fun fetch(lat,lon): RemoteResult<OneCallResponse>` + Retrofit-backed impl. Reuse the same internal `safeCall` pattern as Perenual (cancel rethrow, IOException→Network, HttpException→Http/RateLimited). Short-circuit to `Network(IOException("OPENWEATHER_KEY not configured"))` when key blank.

**Tests**
- `WeatherMapperTest` — happy path, missing rain map, empty hourly, zero uv default
- `WeatherRemoteDataSourceImplTest` is skipped — same shape as `PerenualRemoteDataSourceImpl`, low marginal value

### Step 2 — Repository SWR
**Files**
- `domain/repository/PlantRepository.kt` — extend `WeatherRepository` with `suspend fun refresh(lat, lon): WeatherFetchResult`
- `domain/repository/WeatherFetchResult.kt` — sealed: `Fresh(snapshot)`, `Stale(snapshot, reason)`, `Offline(snapshot?)`
- `data/repository/WeatherRepositoryImpl.kt` (split out of `PlantRepositoryImpl.kt`) — accepts `WeatherDao`, `WeatherRemoteDataSource`, `Clock`. Logic:
  - Read latest cached snapshot for the ~lat,lon (within 0.05° radius)
  - If cached and `now - recordedAt < FRESH_TTL (30 min)` → `Fresh`
  - Else call remote
    - Success → map + persist + return `Fresh`
    - Network/RateLimited/Http → `Stale(cached, reason)` if cached, else `Offline(null)`
- Move `WeatherRepositoryImpl` out of `PlantRepositoryImpl.kt` into its own file (clean module boundary)

**Tests**
- `WeatherRepositoryImplTest` — fresh cache short-circuits; stale + remote success persists; remote failure with cache returns Stale; remote failure no cache returns Offline.

### Step 3 — LocationProvider + DailyCareScheduler integration
**Files**
- `domain/repository/LocationProvider.kt` — interface `suspend fun current(): Coordinates?`
- `domain/model/Coordinates.kt` — `data class Coordinates(val lat, val lon)`
- `data/location/AndroidLocationProvider.kt` — uses `LocationManager.getLastKnownLocation` over NETWORK_PROVIDER and PASSIVE_PROVIDER. Hilt-injectable with `@ApplicationContext`. No callbacks/streaming for V1.
- Update `DailyCareScheduler.kt` — inject `LocationProvider` + `WeatherRepository`. Before reading `recentWeather`, call `provider.current()?.let { weather.refresh(it.lat, it.lon) }`. Network/permission failures swallowed.

**Tests**
- `DailyCareSchedulerTest` is non-trivial (Worker DI). Skip; the integration is a 5-line glue fragment. Repo + provider tested separately.

### Step 4 — DI + build
**Files**
- `di/NetworkModule.kt` — add `@Provides WeatherApi(@Named("weatherRetrofit") Retrofit)`
- `di/RepositoryModule.kt` — `@Binds WeatherRemoteDataSourceImpl → WeatherRemoteDataSource`, `@Binds AndroidLocationProvider → LocationProvider`
- Verify: `./gradlew :app:assembleDebug :app:testDebugUnitTest`

## Acceptance
- All new tests green (mapper + repo SWR ≈ 8 cases)
- Existing 127 unit tests still green
- `./gradlew :app:assembleDebug` succeeds
- Local smoke (manual): with real `OPENWEATHER_KEY` set, `DebugTriggerReceiver` run lands one snapshot in `weather_snapshot` table

## Out of scope (deferred)
- Streaming location updates / rationale dialog rework
- Hourly forecast usage in care engine (only `current` for now — accumulates 1 snapshot/day)
- Pruning old snapshots (>30 d) — DAO handles via `since` filter; size impact negligible
- Manual weather refresh UI

## Risk register
- LocationManager `getLastKnownLocation` may return null until any app has used Location. First few runs may have no weather. Acceptable: care engine just falls back to base interval.
- OpenWeather free tier: 1,000 calls/day is plenty (1 call/day per device).
- Coordinates rounding for cache lookup: 0.05° ≈ 5km, fine for plants.
