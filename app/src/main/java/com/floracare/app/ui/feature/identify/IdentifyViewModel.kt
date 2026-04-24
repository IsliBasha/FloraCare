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
import com.floracare.app.domain.usecase.SpeciesLookupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private val speciesLookup: SpeciesLookupUseCase,
    private val plants: PlantRepository,
) : ViewModel() {

    private var enrichJob: Job? = null

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
        if (current !is IdentifyUiState.Picker) return

        // High-confidence picks never need a second-tier lookup — advance
        // straight to Naming and let the existing ResolveOrCreateSpeciesUseCase
        // path do its cache hit / synth fallback inside onSave.
        if (!current.lowConfidence) {
            _state.value = IdentifyUiState.Naming(prediction.label, current.predictions)
            return
        }

        enrichJob?.cancel()
        val enrichingState = IdentifyUiState.Enriching(prediction.label, current.predictions)
        _state.value = enrichingState
        enrichJob = viewModelScope.launch {
            try {
                val cached = plants.findSpeciesByScientificName(prediction.label.trim())
                if (cached == null) {
                    // Result is discarded — SpeciesRepository already wrote the
                    // row if the call succeeded, so onSave will find it.
                    speciesLookup(prediction.label)
                }
            } catch (e: CancellationException) {
                // Don't advance if we were cancelled by a newer pick or by
                // onEnrichingCancelled — the VM already moved on.
                throw e
            } catch (_: Throwable) {
                // Silent fallback: Offline/remote failure falls through to the
                // Naming transition so the user can still save with synth care.
            }
            // Gate on the exact state we wrote above; a newer pick will have
            // replaced it, so we must not clobber that with our stale label.
            if (_state.value === enrichingState) {
                _state.value = IdentifyUiState.Naming(prediction.label, current.predictions)
            }
        }
    }

    fun onEnrichingCancelled() {
        val current = _state.value
        if (current is IdentifyUiState.Enriching) {
            enrichJob?.cancel()
            enrichJob = null
            _state.value = IdentifyUiState.Picker(current.predictions, lowConfidence = true)
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
        val topConfidence = current.predictions.firstOrNull { it.label == current.selectedLabel }
            ?.confidence
            ?: current.predictions.firstOrNull()?.confidence
            ?: 0f
        viewModelScope.launch {
            runCatching {
                val speciesId = resolveSpecies(current.selectedLabel, topConfidence)
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
