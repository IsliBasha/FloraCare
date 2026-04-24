package com.floracare.app.ui.feature.identify

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floracare.app.data.ml.Prediction
import com.floracare.app.data.ml.SpeciesClassifier
import com.floracare.app.domain.model.ConfidenceThresholds
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.domain.usecase.ResolveOrCreateSpeciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class IdentifyViewModel @Inject constructor(
    private val classifier: SpeciesClassifier,
    private val resolveSpecies: ResolveOrCreateSpeciesUseCase,
    private val plants: PlantRepository,
) : ViewModel() {

    /** Test seam — replace to inject a deterministic id in unit tests. */
    internal var plantIdGenerator: () -> String = { "pl-${UUID.randomUUID()}" }

    private val _state = MutableStateFlow<IdentifyUiState>(IdentifyUiState.RequestPermission)
    val state: StateFlow<IdentifyUiState> = _state.asStateFlow()

    fun onPermissionGranted() {
        _state.value = IdentifyUiState.Ready
    }

    fun onPermissionDenied() {
        _state.value = IdentifyUiState.PermissionDenied
    }

    /** Caller tells us a capture is in flight so the UI can show a shutter state. */
    fun onCaptureStart() {
        if (_state.value == IdentifyUiState.Ready) {
            _state.value = IdentifyUiState.Capturing
        }
    }

    fun onCaptureFailed(message: String) {
        _state.value = IdentifyUiState.Error(message)
    }

    /** The camera layer hands us a finished bitmap. Runs inference in the VM scope. */
    fun onBitmapCaptured(bitmap: Bitmap) {
        _state.value = IdentifyUiState.Classifying
        viewModelScope.launch {
            runCatching { classifier.topK(bitmap, k = 3) }
                .onSuccess { preds ->
                    _state.value = if (preds.isEmpty()) {
                        IdentifyUiState.Error("No species predictions returned")
                    } else {
                        val lowConfidence =
                            (preds.firstOrNull()?.confidence ?: 0f) < LOW_CONFIDENCE_THRESHOLD
                        IdentifyUiState.Picker(preds, lowConfidence = lowConfidence)
                    }
                }
                .onFailure { t ->
                    _state.value = IdentifyUiState.Error(t.message ?: "Classification failed")
                }
        }
    }

    fun onPredictionSelected(prediction: Prediction) {
        val current = _state.value
        if (current is IdentifyUiState.Picker) {
            _state.value = IdentifyUiState.Naming(
                selectedLabel = prediction.label,
                predictions = current.predictions,
            )
        }
    }

    fun onNamingCancelled() {
        val current = _state.value
        if (current is IdentifyUiState.Naming) {
            _state.value = IdentifyUiState.Picker(current.predictions)
        }
    }

    fun onRetake() {
        _state.value = IdentifyUiState.Ready
    }

    fun onSave(nickname: String) {
        val current = _state.value
        if (current !is IdentifyUiState.Naming) return
        val trimmed = nickname.trim()
        if (trimmed.isEmpty()) {
            _state.value = IdentifyUiState.Error("Please enter a nickname")
            return
        }
        viewModelScope.launch {
            runCatching {
                val speciesId = resolveSpecies(current.selectedLabel)
                val plantId = plantIdGenerator()
                plants.upsert(
                    Plant(
                        id = plantId,
                        nickname = trimmed,
                        speciesId = speciesId,
                        locationTag = LocationTag.INDOOR,
                        acquiredAt = Clock.System.now(),
                        coverPhotoUri = null,
                        notes = "",
                    ),
                )
                plantId
            }
                .onSuccess { _state.value = IdentifyUiState.Saved(it) }
                .onFailure {
                    _state.value = IdentifyUiState.Error(it.message ?: "Save failed")
                }
        }
    }

    companion object {
        /**
         * Alias for [ConfidenceThresholds.LOW_CONFIDENCE] kept so androidTest
         * references (`AiyCalibrationTest`) compile without churn. The canonical
         * value lives in the domain layer.
         */
        const val LOW_CONFIDENCE_THRESHOLD: Float = ConfidenceThresholds.LOW_CONFIDENCE
    }
}
