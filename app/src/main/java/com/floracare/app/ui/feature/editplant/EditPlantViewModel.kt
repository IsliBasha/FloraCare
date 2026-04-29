package com.floracare.app.ui.feature.editplant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.ui.navigation.FloraRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the edit-plant form. Reads the persisted plant once on init and
 * mirrors the editable fields into a [EditPlantUiState.Ready] draft. Saves
 * call [PlantRepository.upsert] with the original [Plant] template so any
 * fields outside the form (acquiredAt, speciesId) are preserved untouched.
 *
 * The primary constructor takes a raw plantId so JVM unit tests can drive
 * the VM without standing up a navigation SavedStateHandle. Production code
 * goes through the [Inject]-annotated secondary constructor which decodes
 * the route from the handle.
 */
@HiltViewModel
class EditPlantViewModel(
    private val plantId: String,
    private val plants: PlantRepository,
) : ViewModel() {

    @Inject constructor(
        savedStateHandle: SavedStateHandle,
        plants: PlantRepository,
    ) : this(
        plantId = savedStateHandle.toRoute<FloraRoute.EditPlant>().plantId,
        plants = plants,
    )

    private var original: Plant? = null

    private val _state = MutableStateFlow<EditPlantUiState>(EditPlantUiState.Loading)
    val state: StateFlow<EditPlantUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val plant = plants.findPlant(plantId)
        if (plant == null) {
            _state.value = EditPlantUiState.NotFound
            return
        }
        original = plant
        _state.value = EditPlantUiState.Ready(
            plantId = plant.id,
            nickname = plant.nickname,
            notes = plant.notes,
            locationTag = plant.locationTag,
            coverPhotoUri = plant.coverPhotoUri,
        )
    }

    fun onEvent(event: EditPlantEvent) {
        when (event) {
            is EditPlantEvent.SetNickname -> updateReady { it.copy(nickname = event.value, nicknameError = null) }
            is EditPlantEvent.SetNotes -> updateReady { it.copy(notes = event.value) }
            is EditPlantEvent.SetLocation -> updateReady { it.copy(locationTag = event.tag) }
            is EditPlantEvent.SetCoverPhotoUri -> updateReady { it.copy(coverPhotoUri = event.uri) }
            EditPlantEvent.Save -> save()
            EditPlantEvent.Archive -> archive()
            EditPlantEvent.UnarchiveLast -> unarchive()
        }
    }

    private fun updateReady(transform: (EditPlantUiState.Ready) -> EditPlantUiState.Ready) {
        _state.update { current ->
            if (current is EditPlantUiState.Ready) transform(current) else current
        }
    }

    private fun save() {
        val ready = _state.value as? EditPlantUiState.Ready ?: return
        val template = original ?: return
        val trimmed = ready.nickname.trim()
        if (trimmed.isEmpty()) {
            _state.value = ready.copy(nicknameError = "Give your plant a name")
            return
        }
        if (ready.saving) return
        _state.value = ready.copy(saving = true, nicknameError = null)
        viewModelScope.launch {
            val updated = template.copy(
                nickname = trimmed,
                notes = ready.notes.trim(),
                locationTag = ready.locationTag,
                coverPhotoUri = ready.coverPhotoUri,
            )
            plants.upsert(updated)
            original = updated
            _state.value = EditPlantUiState.Saved
        }
    }

    private fun archive() {
        viewModelScope.launch {
            plants.archivePlant(plantId, archived = true)
            _state.value = EditPlantUiState.Saved
        }
    }

    private fun unarchive() {
        viewModelScope.launch {
            plants.archivePlant(plantId, archived = false)
        }
    }
}
