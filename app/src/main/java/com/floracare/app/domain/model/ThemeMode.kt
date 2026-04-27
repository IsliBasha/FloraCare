package com.floracare.app.domain.model

/**
 * User's theme preference. SYSTEM defers to the device's dark-mode setting;
 * LIGHT/DARK pin the app regardless of the device.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
