package com.floracare.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPreferencesTest {

    @Test
    fun `defaults match expectations`() {
        val defaults = AppPreferences()
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
        assertEquals(TemperatureUnit.CELSIUS, defaults.temperatureUnit)
        assertTrue(defaults.notificationsEnabled)
    }

    @Test
    fun `copy supports independent field updates`() {
        val base = AppPreferences()
        val darkOnly = base.copy(themeMode = ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, darkOnly.themeMode)
        assertEquals(base.temperatureUnit, darkOnly.temperatureUnit)
        assertEquals(base.notificationsEnabled, darkOnly.notificationsEnabled)
    }
}
