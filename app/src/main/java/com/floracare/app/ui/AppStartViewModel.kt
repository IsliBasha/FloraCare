package com.floracare.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floracare.app.data.prefs.UserPrefs
import com.floracare.app.ui.navigation.FloraRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Resolves the start destination for MainActivity by reading the persisted
 * onboarding flag. Emits `null` until the first value is observed so the UI
 * can keep a blank splash on screen rather than flashing the wrong route.
 */
@HiltViewModel
class AppStartViewModel @Inject constructor(
    userPrefs: UserPrefs,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<FloraRoute?>(null)
    val startDestination: StateFlow<FloraRoute?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val completed = userPrefs.hasCompletedOnboarding().first()
            _startDestination.value =
                if (completed) FloraRoute.PlantList else FloraRoute.Onboarding
        }
    }
}
