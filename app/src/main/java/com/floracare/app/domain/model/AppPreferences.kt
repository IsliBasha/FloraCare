package com.floracare.app.domain.model

/**
 * Aggregate of user-controlled app preferences. Defaults match expected
 * first-run behaviour (system theme, metric, notifications on).
 */
data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val notificationsEnabled: Boolean = true,
)
