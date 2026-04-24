package com.floracare.app.ui.feature.identify

import com.floracare.app.data.ml.Prediction

sealed interface IdentifyUiState {
    data object RequestPermission : IdentifyUiState
    data object PermissionDenied : IdentifyUiState
    data object Ready : IdentifyUiState
    data object Capturing : IdentifyUiState
    data object Classifying : IdentifyUiState
    /**
     * [lowConfidence] is true when the top-1 score falls below an empirical
     * threshold — the UI surfaces a "try another angle" hint without blocking
     * the picker so the user can still proceed if a result looks right.
     */
    data class Picker(
        val predictions: List<Prediction>,
        val lowConfidence: Boolean = false,
    ) : IdentifyUiState
    /**
     * User picked a low-confidence prediction — we're calling Perenual before
     * moving on so [Naming] is populated with real taxonomy. Cancel returns
     * to the [Picker]; success or silent fallback advances to [Naming].
     */
    data class Enriching(
        val selectedLabel: String,
        val predictions: List<Prediction>,
    ) : IdentifyUiState
    data class Naming(
        val selectedLabel: String,
        val predictions: List<Prediction>,
    ) : IdentifyUiState
    data class Saved(val plantId: String) : IdentifyUiState
    data class Error(val message: String) : IdentifyUiState
}
