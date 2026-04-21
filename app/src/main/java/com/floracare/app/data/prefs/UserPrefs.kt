package com.floracare.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Lightweight user-preferences surface. Kept as an interface so tests can
 * substitute an in-memory fake without touching DataStore file I/O.
 */
interface UserPrefs {
    fun hasCompletedOnboarding(): Flow<Boolean>
    suspend fun markOnboardingComplete()
}

class UserPrefsDataStore(
    private val dataStore: DataStore<Preferences>,
) : UserPrefs {

    override fun hasCompletedOnboarding(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_ONBOARDING_COMPLETE] ?: false }

    override suspend fun markOnboardingComplete() {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = true }
    }

    companion object {
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }
}
