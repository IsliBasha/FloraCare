package com.floracare.app.ui.feature.diagnose

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.floracare.app.data.ml.DiseaseClassifier
import com.floracare.app.data.ml.Prediction
import com.floracare.app.data.ml.TreatmentProvider
import com.floracare.app.domain.model.ConfidenceThresholds
import com.floracare.app.domain.model.DiagnosisResult
import com.floracare.app.domain.repository.DiagnosisRepository
import com.floracare.app.ui.navigation.FloraRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Drives [DiagnoseScreen].
 *
 * Two-constructor pattern: the primary constructor takes a raw [plantId] so
 * JVM unit tests bypass [SavedStateHandle]. The [@Inject]-annotated secondary
 * constructor decodes the route in production.
 */
@HiltViewModel
class DiagnoseViewModel(
    private val plantId: String?,
    private val classifier: DiseaseClassifier,
    private val repo: DiagnosisRepository,
) : ViewModel() {

    @Inject constructor(
        savedStateHandle: SavedStateHandle,
        classifier: DiseaseClassifier,
        repo: DiagnosisRepository,
    ) : this(
        plantId = savedStateHandle.toRoute<FloraRoute.Diagnose>().plantId,
        classifier = classifier,
        repo = repo,
    )

    /** Test seam — replace to inject a deterministic id in unit tests. */
    internal var diagnosisIdGenerator: () -> String = { "dg-${UUID.randomUUID()}" }

    private val _state = MutableStateFlow<DiagnoseUiState>(DiagnoseUiState.RequestPermission)
    val state: StateFlow<DiagnoseUiState> = _state.asStateFlow()

    fun onPermissionGranted() {
        _state.value = DiagnoseUiState.Ready
    }

    fun onPermissionDenied() {
        _state.value = DiagnoseUiState.PermissionDenied
    }

    fun onCaptureStart() {
        if (_state.value == DiagnoseUiState.Ready) {
            _state.value = DiagnoseUiState.Capturing
        }
    }

    fun onCaptureFailed(message: String) {
        _state.value = DiagnoseUiState.Error(message)
    }

    /** Camera layer hands us a finished bitmap. Runs inference in the VM scope. */
    fun onBitmapCaptured(bitmap: Bitmap) {
        _state.value = DiagnoseUiState.Classifying
        viewModelScope.launch {
            runCatching { classifier.topK(bitmap, k = 3) }
                .onSuccess { preds ->
                    _state.value = if (preds.isEmpty()) {
                        DiagnoseUiState.Error("No disease predictions returned")
                    } else {
                        val lowConfidence =
                            (preds.firstOrNull()?.confidence ?: 0f) < ConfidenceThresholds.LOW_CONFIDENCE
                        DiagnoseUiState.Picker(preds, lowConfidence = lowConfidence)
                    }
                }
                .onFailure { t ->
                    _state.value = DiagnoseUiState.Error(t.message ?: "Classification failed")
                }
        }
    }

    fun onPredictionSelected(prediction: Prediction) {
        if (_state.value !is DiagnoseUiState.Picker) return
        _state.value = DiagnoseUiState.Result(
            diagnosisLabel = prediction.label,
            confidence = prediction.confidence,
            treatmentSuggestion = TreatmentProvider.suggest(prediction.label),
            plantId = plantId,
        )
    }

    fun onRetake() {
        _state.value = DiagnoseUiState.Ready
    }

    fun onSave() {
        val current = _state.value
        if (current !is DiagnoseUiState.Result) return
        viewModelScope.launch {
            runCatching {
                val id = diagnosisIdGenerator()
                repo.insert(
                    DiagnosisResult(
                        id = id,
                        plantId = current.plantId,
                        photoUri = "",
                        diagnosisLabel = current.diagnosisLabel,
                        confidence = current.confidence,
                        treatmentSuggestion = current.treatmentSuggestion,
                        createdAt = Clock.System.now(),
                    ),
                )
                id
            }
                .onSuccess { id -> _state.value = DiagnoseUiState.Saved(id) }
                .onFailure { t -> _state.value = DiagnoseUiState.Error(t.message ?: "Save failed") }
        }
    }
}
