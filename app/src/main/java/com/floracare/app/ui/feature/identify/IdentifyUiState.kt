package com.floracare.app.ui.feature.identify

import com.floracare.app.data.ml.Prediction

sealed interface IdentifyUiState {
    data object RequestPermission : IdentifyUiState
    data object PermissionDenied : IdentifyUiState
    data object Ready : IdentifyUiState
    data object Capturing : IdentifyUiState
    data object Classifying : IdentifyUiState
    data class Picker(val predictions: List<Prediction>) : IdentifyUiState
    data class Naming(
        val selectedLabel: String,
        val predictions: List<Prediction>,
    ) : IdentifyUiState
    data class Saved(val plantId: String) : IdentifyUiState
    data class Error(val message: String) : IdentifyUiState
}
