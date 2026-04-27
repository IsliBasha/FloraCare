package com.floracare.app.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floracare.app.data.prefs.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPrefs,
    @Named(QUALIFIER_APP_VERSION) private val appVersion: String,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        prefs.appPreferences()
            .map { SettingsUiState(preferences = it, appVersion = appVersion) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = SettingsUiState(appVersion = appVersion),
            )

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.SetThemeMode -> prefs.setThemeMode(event.mode)
                is SettingsEvent.SetTemperatureUnit -> prefs.setTemperatureUnit(event.unit)
                is SettingsEvent.SetNotificationsEnabled ->
                    prefs.setNotificationsEnabled(event.enabled)
            }
        }
    }

    companion object {
        const val QUALIFIER_APP_VERSION = "appVersion"
    }
}
