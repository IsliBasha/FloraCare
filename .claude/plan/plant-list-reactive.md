# Plan — PlantListViewModel reactive rewrite (HANDOFF task #2)

## Goal
Replace one-shot `first()` snapshots with a live combine of plants + species +
open tasks. The plant list now updates in real time when notification actions
(task #1) mark tasks complete, when the scheduler upserts new tasks, or when
the user adds a plant. Expose a real "due today" rollup in the header.

## Approach — Option A

```
┌─ PlantRepository.observePlants()          ─┐
│  PlantRepository.observeAllSpecies()       │  combine ─► toPlantCards(...)
└─ PlantRepository.observeAllOpenTasks()    ─┘                    │
                                                                  ▼
                                                        PlantListUiState.Success
                                                        (cards, duesToday)
```

`stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loading)` keeps
the upstream hot across recompositions and brief config changes without leaking.

## Files

| File | Change |
| --- | --- |
| `data/local/Daos.kt` | add `CareTaskDao.observeAllOpenTasks()` |
| `domain/repository/PlantRepository.kt` | add `observeAllOpenTasks()` + `observeAllSpecies()` |
| `data/repository/PlantRepositoryImpl.kt` | implement both |
| `ui/feature/plantlist/PlantListViewModel.kt` | rewrite: combine + stateIn + extract pure rollup |
| `ui/feature/plantlist/PlantListScreen.kt` | subtitle includes dues-today count |
| `ui/feature/plantlist/PlantCardMapping.kt` | **new** — pure `toPlantCards(...)` helper |
| `test/.../PlantListViewModelTest.kt` | **new** — Turbine-style VM tests with FakeRepo |
| `test/.../PlantCardMappingTest.kt` | **new** — pure rollup tests |

## UI state contract

```kotlin
sealed interface PlantListUiState {
    data object Loading : PlantListUiState
    data class Success(
        val plants: List<PlantCardUi>,
        val duesToday: Int,
    ) : PlantListUiState
    data class Error(val message: String) : PlantListUiState
}
```

## Rollup rules (extracted to `toPlantCards`)

For each plant:
1. Look up species in the `speciesById` map (null if missing).
2. Pick `nextTask`: tasks for this plant where `completedAt == null` and `snoozedUntil == null || snoozedUntil <= now`, min by `scheduledAt`.
3. Label: `"${verb} today" | "${verb} tomorrow" | "${verb} in ${n}d" | "${verb} ${n}d ago" | "No scheduled care"`.
4. Accent: palette index = `speciesId.hashCode().absoluteValue % palette.size`. Stable and spread.
5. `duesToday` = count of cards whose `nextTask.scheduledAt` is today in `TimeZone.currentSystemDefault()`.

## Accent palette

Reuse theme colors: `[SageMuted, Terracotta, SageDeep, WarningAmber, ForestDeep]`. All already in `ui/theme/Color.kt`.

## Test matrix

**`PlantCardMappingTest` (pure):**
- empty plants → empty cards, 0 dues
- plant without species → shows "Unknown species"
- plant with next-task today → card label "Water today", counted in dues
- plant with next-task tomorrow → dues=0
- plant whose only task is snoozed → treated as if no task
- completed tasks are ignored
- negative-days task (overdue) → "Water 2d ago"
- stable accent: same species id → same color twice

**`PlantListViewModelTest` (Turbine-style):**
- initial state is `Loading`
- after repo emits plants → `Success` with correct card count
- repo emits new task → VM emits new state with updated label
- repo error (via thrown in flow) → `Error` state

## Out of scope
- Pull-to-refresh, empty-state illustration, shared-element transitions.
- `SavedStateHandle` integration (no screen args).
