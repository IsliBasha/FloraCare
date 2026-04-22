package com.floracare.app.ui.feature.addplant

import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Species
import kotlinx.datetime.Instant

/**
 * Form snapshot for the manual add-plant flow. Kept flat so Compose recomposes
 * predictably; nothing here holds references to Android types so the state is
 * trivially testable on the JVM.
 */
data class AddPlantManualUiState(
    val nickname: String = "",
    val nicknameError: String? = null,
    val speciesQuery: String = "",
    val selectedSpecies: Species? = null,
    val speciesMatches: List<Species> = emptyList(),
    val locationTag: LocationTag = LocationTag.INDOOR,
    val acquiredAt: Instant? = null,
    val notes: String = "",
    val saving: Boolean = false,
    val savedPlantId: String? = null,
    val errorMessage: String? = null,
)
