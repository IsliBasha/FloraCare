package com.floracare.app.ui.feature.addplant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.domain.usecase.ResolveOrCreateSpeciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the manual add-plant form. Species lookup is filtered in-memory from
 * the reactive species list — the DB is tiny (seeded ~3 rows, plus anything
 * the user has created via Identify) so this stays cheap and avoids a debounce
 * query layer we don't need yet.
 *
 * The VM owns one authoritative snapshot ([AddPlantManualUiState]) and never
 * mutates Plant/Species instances; submissions produce fresh domain objects.
 */
@HiltViewModel
class AddPlantManualViewModel @Inject constructor(
    private val plants: PlantRepository,
    private val resolveSpecies: ResolveOrCreateSpeciesUseCase,
) : ViewModel() {

    /** Test seam — override to inject a deterministic id. */
    internal var plantIdGenerator: () -> String = { "pl-${UUID.randomUUID()}" }

    /** Test seam — override to inject a fixed Clock in tests. */
    internal var clock: Clock = Clock.System

    private val _state = MutableStateFlow(AddPlantManualUiState())
    val state: StateFlow<AddPlantManualUiState> = _state.asStateFlow()

    private var allSpecies: List<Species> = emptyList()

    init {
        _state.update { it.copy(acquiredAt = clock.now()) }
        viewModelScope.launch {
            plants.observeAllSpecies().collect { list ->
                // The AIY classifier auto-creates a "background" species row whenever the
                // model's top-1 is the catch-all class — it's noise in the manual picker.
                allSpecies = list.filterNot { it.isAiyBackground() }
                refreshMatches()
            }
        }
    }

    fun onNicknameChange(value: String) {
        _state.update { it.copy(nickname = value, nicknameError = null) }
    }

    fun onSpeciesQueryChange(value: String) {
        _state.update {
            it.copy(
                speciesQuery = value,
                // Typing resets an earlier selection so the user can change their mind.
                selectedSpecies = null,
            )
        }
        refreshMatches()
    }

    fun onSpeciesSelected(species: Species) {
        _state.update {
            it.copy(
                selectedSpecies = species,
                speciesQuery = species.commonName,
                speciesMatches = emptyList(),
            )
        }
    }

    fun onSpeciesCleared() {
        _state.update {
            it.copy(selectedSpecies = null, speciesQuery = "", speciesMatches = emptyList())
        }
    }

    fun onLocationTagChange(tag: LocationTag) {
        _state.update { it.copy(locationTag = tag) }
    }

    fun onAcquiredAtChange(instant: Instant) {
        _state.update { it.copy(acquiredAt = instant) }
    }

    fun onNotesChange(value: String) {
        _state.update { it.copy(notes = value) }
    }

    fun onSubmit() {
        val current = _state.value
        val trimmedNickname = current.nickname.trim()
        if (trimmedNickname.isEmpty()) {
            _state.update { it.copy(nicknameError = "Give your plant a name") }
            return
        }
        if (current.saving || current.savedPlantId != null) return

        _state.update { it.copy(saving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { persist(current, trimmedNickname) }
                .onSuccess { plantId ->
                    _state.update { it.copy(saving = false, savedPlantId = plantId) }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            saving = false,
                            errorMessage = t.message ?: "Couldn't save — try again",
                        )
                    }
                }
        }
    }

    private suspend fun persist(snapshot: AddPlantManualUiState, trimmedNickname: String): String {
        val speciesId = resolveSpeciesId(snapshot)
        val plantId = plantIdGenerator()
        val plant = Plant(
            id = plantId,
            nickname = trimmedNickname,
            speciesId = speciesId,
            locationTag = snapshot.locationTag,
            acquiredAt = snapshot.acquiredAt ?: clock.now(),
            coverPhotoUri = null,
            notes = snapshot.notes.trim(),
        )
        plants.upsert(plant)
        return plantId
    }

    /**
     * Resolution priority:
     * 1. Dropdown-selected species wins — user's explicit choice.
     * 2. Non-empty free-text query → resolve or create a species row.
     * 3. Empty query → nickname-only plant (speciesId = null).
     */
    private suspend fun resolveSpeciesId(snapshot: AddPlantManualUiState): String? {
        snapshot.selectedSpecies?.let { return it.id }
        val query = snapshot.speciesQuery.trim()
        if (query.isEmpty()) return null
        return resolveSpecies(query)
    }

    private fun refreshMatches() {
        val query = _state.value.speciesQuery.trim()
        val selected = _state.value.selectedSpecies
        val matches = when {
            selected != null -> emptyList()
            query.isEmpty() -> emptyList()
            else -> allSpecies
                .asSequence()
                .filter { it.matches(query) }
                .take(MAX_MATCHES)
                .toList()
        }
        _state.update { it.copy(speciesMatches = matches) }
    }

    private fun Species.matches(query: String): Boolean =
        commonName.contains(query, ignoreCase = true) ||
            scientificName.contains(query, ignoreCase = true)

    private fun Species.isAiyBackground(): Boolean =
        commonName.equals("background", ignoreCase = true) ||
            scientificName.equals("background", ignoreCase = true)

    companion object {
        private const val MAX_MATCHES = 8
    }
}
