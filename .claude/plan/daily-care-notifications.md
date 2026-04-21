# Plan — Daily care notifications (HANDOFF task #1)

## Goal
Turn the adaptive care engine into visible OS-level behaviour: at 07:00 local,
post a notification per due task with **Mark Done** / **Snooze 2d** actions and
a deep-link to `PlantDetail`. Tapping an action persists state without opening
the app.

## Architecture (Option A — single worker + extracted dispatcher)

```
DailyCareScheduler (@HiltWorker, periodic 24h)
    │
    ├─► computes & upserts CareTasks (already implemented)
    └─► queries CareTaskDao.findDueBefore(now + 24h)
            │
            └─► NotificationDispatcher.postDueTasks(tasks, plants, species)
                    │
                    └─► NotificationCompat.Builder
                          ├─ channel: by task.type → CareChannel
                          ├─ content PI: MainActivity + EXTRA_PLANT_ID
                          ├─ action 1: NotificationActionReceiver + ACTION_MARK_DONE
                          └─ action 2: NotificationActionReceiver + ACTION_SNOOZE_2D

NotificationActionReceiver
    └─► WorkManager.enqueue(OneTimeWorkRequest<CareActionWorker>)
          │
          └─► CareActionWorker (@HiltWorker)
                └─► PlantRepository.markTaskComplete(taskId, now)
                    PlantRepository.snoozeTask(taskId, now + 2d)

BootReceiver (BOOT_COMPLETED)
    └─► CareScheduleBootstrapper.enqueue(context)
```

## Files

| File | Status |
| --- | --- |
| `data/notification/NotificationDispatcher.kt` | **new** |
| `data/notification/NotificationIds.kt` | **new** (id factory) |
| `data/worker/CareActionWorker.kt` | **new** |
| `data/worker/BootReceiver.kt` | **new** |
| `data/worker/DailyCareScheduler.kt` | modify — inject dispatcher, post notifications |
| `data/worker/NotificationActionReceiver.kt` | modify — enqueue `CareActionWorker` |
| `MainActivity.kt` | modify — read `EXTRA_PLANT_ID`, navigate to `PlantDetail` |
| `ui/navigation/FloraCareNavHost.kt` | modify — accept optional `initialPlantId` |
| `AndroidManifest.xml` | modify — add `BootReceiver` with `BOOT_COMPLETED` filter |
| `di/DispatcherModule.kt` | **new** (Hilt binding for dispatcher) — or `@Singleton` ctor inject |
| `test/.../CareActionWorkerTest.kt` | **new** |
| `test/.../NotificationDispatcherContentTest.kt` | **new** |

## Data flow details

**`CareActionWorker` input data keys:**
- `taskId: String` (required)
- `actionType: String` — `"MARK_DONE"` or `"SNOOZE_2D"`

**Notification ID strategy:** stable per task — `task.id.hashCode()`. Re-posting replaces the same notification.

**Deep-link intent:**
```kotlin
Intent(context, MainActivity::class.java).apply {
    putExtra(EXTRA_PLANT_ID, task.plantId)
    flags = FLAG_ACTIVITY_SINGLE_TOP or FLAG_ACTIVITY_CLEAR_TOP
}
```
`MainActivity.onCreate` / `onNewIntent` passes `plantId` into `FloraCareNavHost` which, if non-null, starts at `PlantList` and pushes `PlantDetail(id)` on top so back-stack behaves naturally.

**PendingIntent flags:** `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`. Each action uses a unique request code derived from `(task.id.hashCode() xor action.hashCode())` to avoid PendingIntent re-use collisions.

## Channel routing

| `CareTaskType` | `CareChannel` |
| --- | --- |
| WATER | WATER |
| FERTILIZE | FERTILIZE |
| MIST, ROTATE, REPOT, PRUNE | OTHER |

## Testing

- `CareActionWorkerTest` — Robolectric-free: inject fake `PlantRepository`, call `worker.doWork()` via the `ListenableWorker.doWork` test harness (`TestListenableWorkerBuilder`).
- `NotificationDispatcherContentTest` — pure-JVM test on a small extracted `NotificationContent` helper (title/body string formatting + channel mapping), so we don't need Robolectric for strings. The full Notification build is exercised at runtime.

## Out of scope (explicit)
- Runtime `POST_NOTIFICATIONS` permission request — owned by Onboarding ticket.
- Weather fetch — `WeatherRepository.recentWeather` returns `[]` until ticket A5 lands. Adaptive rules gracefully fall back to species base frequency. Acceptable.
- Localisation of strings — English only for now.
