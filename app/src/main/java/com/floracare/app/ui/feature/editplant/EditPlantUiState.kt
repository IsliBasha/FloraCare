package com.floracare.app.ui.feature.editplant

import com.floracare.app.domain.model.LocationTag

sealed interface EditPlantUiState {
    data object Loading : EditPlantUiState
    data object NotFound : EditPlantUiState

    /**
     * Active editing form. `pendingArchive` flips true after an Archive event
     * and stays true until either the screen consumes the Saved one-shot or
     * the user fires UnarchiveLast.
     */
    data class Ready(
        val plantId: String,
        val nickname: String,
        val nicknameError: String? = null,
        val notes: String,
        val locationTag: LocationTag,
        val coverPhotoUri: String?,
        val saving: Boolean = false,
        val pendingArchive: Boolean = false,
    ) : EditPlantUiState

    /** One-shot terminal state. Screen reacts by popping back. */
    data object Saved : EditPlantUiState
}

sealed interface EditPlantEvent {
    data class SetNickname(val value: String) : EditPlantEvent
    data class SetNotes(val value: String) : EditPlantEvent
    data class SetLocation(val tag: LocationTag) : EditPlantEvent
    data class SetCoverPhotoUri(val uri: String?) : EditPlantEvent
    data object Save : EditPlantEvent
    data object Archive : EditPlantEvent
    data object UnarchiveLast : EditPlantEvent
}
