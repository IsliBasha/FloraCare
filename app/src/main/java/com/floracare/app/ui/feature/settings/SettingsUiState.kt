package com.floracare.app.ui.feature.settings

import com.floracare.app.domain.model.AppPreferences
import com.floracare.app.domain.model.TemperatureUnit
import com.floracare.app.domain.model.ThemeMode

/**
 * Single state object for the Settings screen. Wraps the persisted
 * [AppPreferences] plus static metadata (app version, mentor names) that
 * the screen surfaces in the About section.
 */
data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val appVersion: String = "",
)

/**
 * Closed set of user actions on Settings. The VM is the only writer of
 * UserPrefs from the UI side; events keep that boundary explicit.
 */
sealed interface SettingsEvent {
    data class SetThemeMode(val mode: ThemeMode) : SettingsEvent
    data class SetTemperatureUnit(val unit: TemperatureUnit) : SettingsEvent
    data class SetNotificationsEnabled(val enabled: Boolean) : SettingsEvent
}
