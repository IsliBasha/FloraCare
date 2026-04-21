package com.floracare.app.ui.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floracare.app.data.prefs.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the onboarding flow. Pure in the sense that the VM never touches the
 * Android permission APIs directly — the screen layer owns the launchers and
 * feeds results back through [onPermissionResult]. This keeps the VM unit
 * testable without Robolectric.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onNext() {
        _state.update { current ->
            current.copy(step = nextStep(current.step))
        }
    }

    fun onBack() {
        _state.update { current ->
            current.copy(step = previousStep(current.step))
        }
    }

    /** Record the result of a launcher. Never transitions steps — the user does that via [onNext]. */
    fun onPermissionResult(permission: OnboardingPermission, granted: Boolean) {
        _state.update { current ->
            when (permission) {
                OnboardingPermission.Notifications -> current.copy(
                    notificationsGranted = granted,
                    notificationsAsked = true,
                )
                OnboardingPermission.Camera -> current.copy(
                    cameraGranted = granted,
                    cameraAsked = true,
                )
                OnboardingPermission.Location -> current.copy(
                    locationGranted = granted,
                    locationAsked = true,
                )
            }
        }
    }

    /** Persist completion and flip the terminal flag the screen observes to navigate out. */
    fun onFinish() {
        if (_state.value.finished) return
        viewModelScope.launch {
            userPrefs.markOnboardingComplete()
            _state.update { it.copy(finished = true) }
        }
    }

    private fun nextStep(step: OnboardingStep): OnboardingStep = when (step) {
        OnboardingStep.Welcome -> OnboardingStep.Permissions
        OnboardingStep.Permissions -> OnboardingStep.Ready
        OnboardingStep.Ready -> OnboardingStep.Ready
    }

    private fun previousStep(step: OnboardingStep): OnboardingStep = when (step) {
        OnboardingStep.Welcome -> OnboardingStep.Welcome
        OnboardingStep.Permissions -> OnboardingStep.Welcome
        OnboardingStep.Ready -> OnboardingStep.Permissions
    }
}
