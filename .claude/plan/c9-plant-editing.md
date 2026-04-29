# C-9 — Plant editing (scoped, not started)

Fills a real gap: once a plant is registered there is currently no way to rename it, edit notes, change location, or remove it. A defense panel will ask about this — closing it before defense.

## Existing scaffolding (don't recreate)
- `Plant.archived: Boolean` field already on the domain model + entity
- `PlantDao.observeActive()` already filters `archived = 0` — soft delete is just a boolean flip
- `PlantRepository.upsert(plant: Plant)` rewrites by id — passing the same id with new fields persists in place
- `FloraRoute` is type-safe nav with `@Serializable` data classes; pattern is established
- `AddPlantManualScreen` + `AddPlantManualViewModel` are the closest analogues — same fields surface, can be reused as a visual reference

## Scope locked
1. **Edit nickname** (free text, required, trimmed)
2. **Edit notes** (multi-line, optional)
3. **Edit location tag** (INDOOR / OUTDOOR / GREENHOUSE chips — same `LocationTag` enum)
4. **Edit cover photo URI** (existing `coverPhotoUri` field — reuse the same picker plumbing AddPlantManual uses)
5. **Archive (soft delete)** with **undo via Snackbar** — pressing archive flips `archived = true`, list updates, snackbar offers undo for ~5s
6. **Species change is OUT of scope** — too entangled with ResolveOrCreateSpecies / Perenual SWR; if user wants different species they should re-add. Document in About / future ticket.

## Cycles

### Cycle 1 — Domain + repository hook for archive
**Modified**
- `domain/repository/PlantRepository.kt` — add `suspend fun archivePlant(id: String, archived: Boolean = true)` (default `true` so callers read clean)
- `data/local/PlantDao` — add `@Query("UPDATE plant SET archived = :archived WHERE id = :id") suspend fun setArchived(id, archived)`
- `data/repository/PlantRepositoryImpl.kt` — delegate
- `test/FakePlantRepository.kt` — implement; track `archivedCalls: List<Pair<String, Boolean>>`

**Tests**
- `PlantRepositoryImplTest` doesn't exist; skip (consistent with existing pattern). The Edit VM tests cover the contract.

### Cycle 2 — EditPlantViewModel
**New**
- `ui/feature/editplant/EditPlantViewModel.kt`
  - `state: StateFlow<EditPlantUiState>` — Loading / Ready(plant draft) / Saved / NotFound
  - `onEvent(EditPlantEvent)` — `SetNickname`, `SetNotes`, `SetLocation`, `SetCoverPhotoUri`, `Save`, `Archive`, `UnarchiveLast`
- `ui/feature/editplant/EditPlantUiState.kt`
- Reads via `PlantRepository.findPlant(plantId)` once on init; surfaces a draft `Plant` in state
- `Save` calls `repo.upsert(draft)` and emits a one-shot `Saved` UI signal (collected by screen → popBackStack)
- `Archive` flips archived; `UnarchiveLast` restores

**Tests**
- `EditPlantViewModelTest`
  - initial Loading → Ready with the persisted plant's fields populated
  - SetNickname / SetNotes / SetLocation / SetCoverPhotoUri update the draft (no DB write)
  - Save with blank trimmed nickname → no upsert, surfaces validation error in state
  - Save with valid changes → calls `repo.upsert` exactly once with the updated plant
  - Archive → calls `repo.archivePlant(id, true)`; state transitions to Saved
  - Unarchive after archive → calls `repo.archivePlant(id, false)`
  - Plant id with no row → state becomes NotFound

### Cycle 3 — UI + nav
**New**
- `ui/feature/editplant/EditPlantScreen.kt` — Material 3 form mirroring AddPlantManual layout: text fields + LocationTag chips + photo picker tile + Save / Archive buttons
- `ui/navigation/Routes.kt` — `@Serializable data class EditPlant(val plantId: String) : FloraRoute`
- `ui/navigation/FloraCareNavHost.kt` — `composable<FloraRoute.EditPlant> { ... }`

**Modified**
- `ui/feature/plantdetail/PlantDetailScreen.kt` — overflow menu on TopAppBar with **Edit** + **Archive** items. Edit navigates to `EditPlant(plantId)`. Archive shows confirmation dialog → calls VM (or directly the repo) → Snackbar with Undo.

**No tests** for Cycle 3 (UI only); covered by Cycle 2 VM contract + manual smoke.

## Acceptance
- Open any plant → overflow menu → Edit → form pre-populated → change nickname → Save → screen pops back → list shows new nickname
- Open any plant → overflow menu → Archive → confirm → list no longer shows the plant; Snackbar "Plant archived" with Undo. Tap Undo → plant reappears.
- Cover photo URI editing works the same way as AddPlantManual (re-use the picker)
- Existing 174 unit tests still green; +7-9 new tests bring suite to ~181-183
- assembleDebug clean
- On-device smoke: edit + archive + undo all work

## Out of scope (deferred to future tickets)
- **Bulk multi-select / batch archive** — separate UX concern
- **Restore-archived screen** — undo via Snackbar covers the immediate use case; permanent restore can wait
- **Hard delete** — soft archive is enough for V1; hard delete is destructive and risky pre-defense
- **Species change** — entangled with Perenual / synth flows; let user re-add for now
- **Audit log** — "edited 3 days ago" history; nice but not required
- **Reorder / drag-to-archive** on PlantList — gesture territory, separate ticket

## Risk register
- Multi-step state machine (Loading → Ready → Saved + Archived) — same shape as AddPlantManual; low risk
- Photo URI permissions — re-using AddPlantManual's pattern means no new permission surface
- Snackbar undo + state transitions — Compose `SnackbarHostState` standard pattern
- Concurrent updates (user edits while widget refresh fires) — no risk; `upsert` is REPLACE, last-write-wins is acceptable here
