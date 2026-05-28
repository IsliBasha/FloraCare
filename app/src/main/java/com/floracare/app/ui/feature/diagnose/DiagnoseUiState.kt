package com.floracare.app.ui.feature.diagnose

import com.floracare.app.data.ml.Prediction

sealed interface DiagnoseUiState {
    data object RequestPermission : DiagnoseUiState
    data object PermissionDenied : DiagnoseUiState
    data object Ready : DiagnoseUiState
    data object Capturing : DiagnoseUiState
    data object Classifying : DiagnoseUiState

    /**
     * [lowConfidence] is true when the top-1 score falls below
     * [com.floracare.app.domain.model.ConfidenceThresholds.LOW_CONFIDENCE] — the
     * UI surfaces a "try a clearer photo" hint without blocking the picker.
     */
    data class Picker(
        val predictions: List<Prediction>,
        val lowConfidence: Boolean = false,
    ) : DiagnoseUiState

    /**
     * User picked a disease from the picker. [treatmentSuggestion] is resolved
     * locally via [com.floracare.app.data.ml.TreatmentProvider].
     */
    data class Result(
        val diagnosisLabel: String,
        val confidence: Float,
        val treatmentSuggestion: String,
        val plantId: String?,
    ) : DiagnoseUiState

    /** Diagnosis persisted to Room successfully. */
    data class Saved(val diagnosisId: String) : DiagnoseUiState

    data class Error(val message: String) : DiagnoseUiState
}
