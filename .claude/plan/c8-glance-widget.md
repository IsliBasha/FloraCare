# C-8 — Glance widget content (2026-04-28)

Replaces the header-only widget with the next 3 due care tasks plus deep-links.

## Approach: Option A — EntryPoint + explicit updateAll

Glance widget reads the repo via Hilt `EntryPointAccessors`, renders up to 3 rows. Workers call `TodayTasksWidget().updateAll(context)` after writes so changes propagate without a reactive in-Glance state plumbing.

## Cycles

### Cycle 1 — pure mapping
**New**: `widget/WidgetMapping.kt`
- `data class WidgetRow(plantId, plantNickname, taskType, scheduledAt, dueLabel)`
- `fun toWidgetRows(now, tasks, plants): List<WidgetRow>` — filter completed/snoozed, sort by scheduledAt asc, take 3, look up plant nickname, derive label "Today" / "Tomorrow" / "in Xd" / "Xd ago"
- `WIDGET_MAX_ROWS = 3`

**Tests**: `WidgetMappingTest.kt`
- empty input → empty
- skips completed
- skips snoozed
- sorts by scheduledAt asc
- caps at 3 rows
- plant nickname falls back to "Plant" when lookup fails
- relative-time labels match for today / tomorrow / future / past

### Cycle 2 — Glance UI
**New**: `widget/WidgetDataAccess.kt`
- `@EntryPoint` interface exposing `PlantRepository`

**Modified**: `widget/TodayTasksWidget.kt`
- `provideGlance` reads tasks + plants via runBlocking `.first()` snapshots, calls `toWidgetRows`
- Renders header (`Today's care · N`), then up to 3 rows (or empty-state line)
- Each row uses `actionStartActivity(Intent(context, MainActivity::class.java).putExtra(EXTRA_PLANT_ID, plantId))`
- Reuses Flora palette inline (Glance can't reach `MaterialTheme`)

### Cycle 3 — refresh hooks
**Modified**:
- `data/worker/DailyCareScheduler.kt` — call `TodayTasksWidget().updateAll(applicationContext)` after the doWork loop
- `data/worker/CareActionWorker.kt` — call `updateAll` after MARK_DONE / SNOOZE persists
- (Optional) Add to `BootReceiver` if desired — skip for V1

## Acceptance
- Drop widget on home screen → shows up to 3 next tasks ordered by due time
- Tap a row → opens FloraCare deep-linked into PlantDetail for that plant
- MARK_DONE from notification → widget refreshes (row drops or shifts within ~1s)
- 07:00 daily run → widget shows the new computed schedule
- No tasks → "All caught up" empty state
- Existing 165 unit tests still green; +6-8 new mapping tests

## Out of scope (deferred)
- Multiple sizes / responsive layout
- Per-row icon glyph (just emoji label for V1)
- "Reschedule" / "Mark done" actions inside the widget — separate ticket
