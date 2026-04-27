package com.floracare.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.floracare.app.domain.model.AppPreferences
import com.floracare.app.domain.model.TemperatureUnit
import com.floracare.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * User-preferences surface backed by DataStore. Kept as an interface so tests
 * can substitute an in-memory fake without touching DataStore file I/O.
 */
interface UserPrefs {
    fun hasCompletedOnboarding(): Flow<Boolean>
    suspend fun markOnboardingComplete()

    fun appPreferences(): Flow<AppPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setTemperatureUnit(unit: TemperatureUnit)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}

class UserPrefsDataStore(
    private val dataStore: DataStore<Preferences>,
) : UserPrefs {

    override fun hasCompletedOnboarding(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_ONBOARDING_COMPLETE] ?: false }

    override suspend fun markOnboardingComplete() {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = true }
    }

    override fun appPreferences(): Flow<AppPreferences> =
        dataStore.data.map { prefs ->
            AppPreferences(
                themeMode = prefs[KEY_THEME_MODE]?.let(::parseThemeMode) ?: ThemeMode.SYSTEM,
                temperatureUnit = prefs[KEY_TEMP_UNIT]?.let(::parseTemperatureUnit)
                    ?: TemperatureUnit.CELSIUS,
                notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        dataStore.edit { it[KEY_TEMP_UNIT] = unit.name }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    private fun parseThemeMode(value: String): ThemeMode? =
        runCatching { ThemeMode.valueOf(value) }.getOrNull()

    private fun parseTemperatureUnit(value: String): TemperatureUnit? =
        runCatching { TemperatureUnit.valueOf(value) }.getOrNull()

    companion object {
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_TEMP_UNIT = stringPreferencesKey("temp_unit")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }
}
