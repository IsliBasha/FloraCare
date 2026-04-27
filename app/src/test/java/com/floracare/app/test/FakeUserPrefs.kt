package com.floracare.app.test

import com.floracare.app.data.prefs.UserPrefs
import com.floracare.app.domain.model.AppPreferences
import com.floracare.app.domain.model.TemperatureUnit
import com.floracare.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory UserPrefs substitute. Lets tests control the initial values and
 * observe writes without depending on DataStore file I/O.
 */
class FakeUserPrefs(
    initial: Boolean = false,
    initialAppPrefs: AppPreferences = AppPreferences(),
) : UserPrefs {
    private val completed = MutableStateFlow(initial)
    private val appPrefs = MutableStateFlow(initialAppPrefs)

    override fun hasCompletedOnboarding(): Flow<Boolean> = completed

    override suspend fun markOnboardingComplete() {
        completed.value = true
    }

    override fun appPreferences(): Flow<AppPreferences> = appPrefs

    override suspend fun setThemeMode(mode: ThemeMode) {
        appPrefs.value = appPrefs.value.copy(themeMode = mode)
    }

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        appPrefs.value = appPrefs.value.copy(temperatureUnit = unit)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        appPrefs.value = appPrefs.value.copy(notificationsEnabled = enabled)
    }

    val currentValue: Boolean get() = completed.value
    val currentAppPrefs: AppPreferences get() = appPrefs.value
}
