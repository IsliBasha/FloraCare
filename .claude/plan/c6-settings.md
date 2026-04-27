# C-6 — Settings (2026-04-28)

Promoted from #1 in the next-session priority list. Real screen replacing the existing placeholder.

## Existing scaffolding
- `FloraRoute.Settings` route + nav wiring (placeholder body)
- `UserPrefs` interface + `UserPrefsDataStore` impl (currently onboarding flag only)
- `FakeUserPrefs` test fake
- `FloraCareTheme` accepts `darkTheme: Boolean` parameter (currently always `isSystemInDarkTheme()`)
- `NotificationDispatcher.canPost()` gates posting on the OS-level POST_NOTIFICATIONS permission

## Scope locked
1. **Theme switcher** — `ThemeMode { SYSTEM, LIGHT, DARK }`; persisted; live (no restart)
2. **Temperature unit** — `TemperatureUnit { CELSIUS, FAHRENHEIT }`; persisted; wired into Dashboard weather card
3. **Notifications master toggle** — boolean; gates `NotificationDispatcher.canPost()` alongside the OS permission
4. **About section** — version, mentors, GitHub, license (static, no logic)

## Out of scope (deferred)
- Per-channel notification prefs (water vs fertilize vs mist)
- Per-locale date format / first-day-of-week
- Cloud-sync prefs / account
- Reset-all-data action (destructive — separate ticket)

## Cycles

### Cycle 1 — Domain + UserPrefs extension
**New**
- `domain/model/ThemeMode.kt` (enum)
- `domain/model/TemperatureUnit.kt` (enum + `format(celsius: Float): String` extension)
- `domain/model/AppPreferences.kt` (data class)

**Modified**
- `data/prefs/UserPrefs.kt` — interface gains `appPreferences(): Flow<AppPreferences>`, `setThemeMode`, `setTemperatureUnit`, `setNotificationsEnabled`
- `data/prefs/UserPrefs.kt` — `UserPrefsDataStore` impl with new keys (`theme_mode`, `temp_unit`, `notifications_enabled`)
- `test/FakeUserPrefs.kt` — backing MutableStateFlow + the new methods

**Tests**
- `TemperatureUnitTest` — Celsius and Fahrenheit formatting + rounding + negative temps
- `AppPreferencesTest` — defaults match expectations (SYSTEM, CELSIUS, true)

### Cycle 2 — SettingsViewModel
**New**
- `ui/feature/settings/SettingsViewModel.kt` (Hilt VM, `state: StateFlow<SettingsUiState>`, `onEvent(SettingsEvent)`)
- `ui/feature/settings/SettingsUiState.kt` (data class wrapping `AppPreferences` + version string)

**Tests**
- `SettingsViewModelTest`
  - initial state matches stored prefs
  - setting theme writes through to UserPrefs
  - setting unit writes through
  - toggling notifications writes through
  - state reflects new value after write

### Cycle 3 — UI + integrations
**New**
- `ui/feature/settings/SettingsScreen.kt` — replace placeholder with Material 3 sectioned list
  - Section: Appearance (3-way segmented or radio for theme)
  - Section: Units (2-way segmented °C / °F)
  - Section: Notifications (single switch)
  - Section: About (4-line static info card)

**Modified**
- `MainActivity.kt` — collect theme pref, derive `darkTheme: Boolean` from it (SYSTEM → `isSystemInDarkTheme()`, LIGHT → false, DARK → true)
- `ui/feature/dashboard/DashboardScreen.kt` (or VM/mapping) — pass `TemperatureUnit` through; render via `unit.format(weather.tempC)`
- `data/notification/NotificationDispatcher.kt` — also gate on prefs.notificationsEnabled

**Tests**
- Cycle 3 is mostly UI plumbing; rely on Cycle 1+2 unit coverage for behavior. Manual on-device smoke for each toggle.

## Acceptance
- Three prefs persist across app kill + restart
- Theme switcher applies live (no restart needed)
- Dashboard weather card flips °C ↔ °F instantly when toggled
- Notifications stop firing when master toggle is off (verified via `DebugTriggerReceiver`)
- Existing 155 tests still green; new tests bring suite to ~165–170

## Risks
- DataStore I/O is async; first read of `appPreferences()` may emit defaults briefly while disk loads — handled by initial `AppPreferences()` default.
- Theme animation cross-fade isn't built into Material 3 by default; the swap will be instant. Acceptable for V1.
