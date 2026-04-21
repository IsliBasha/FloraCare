package com.floracare.app.test

import com.floracare.app.data.prefs.UserPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory UserPrefs substitute. Lets tests control the initial value and
 * observe writes without depending on DataStore file I/O.
 */
class FakeUserPrefs(initial: Boolean = false) : UserPrefs {
    private val completed = MutableStateFlow(initial)

    override fun hasCompletedOnboarding(): Flow<Boolean> = completed

    override suspend fun markOnboardingComplete() {
        completed.value = true
    }

    val currentValue: Boolean get() = completed.value
}
