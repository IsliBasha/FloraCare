package com.floracare.app.ui.feature.plantlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.math.max
import kotlin.time.Duration.Companion.days

sealed interface PlantListUiState {
    data object Loading : PlantListUiState
    data class Success(val plants: List<PlantCardUi>) : PlantListUiState
    data class Error(val message: String) : PlantListUiState
}

@HiltViewModel
class PlantListViewModel @Inject constructor(
    private val repo: PlantRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PlantListUiState>(PlantListUiState.Loading)
    val state: StateFlow<PlantListUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun refresh() {
        viewModelScope.launch {
            runCatching {
                val plants = repo.observePlants().first()
                val now = Clock.System.now()
                val cards = plants.map { plant ->
                    val species = plant.speciesId?.let { repo.findSpecies(it) }
                    val next = repo.observeOpenTasks(plant.id).first().minByOrNull { it.scheduledAt }
                    plant.toCard(species, next, now)
                }
                _state.value = PlantListUiState.Success(cards)
            }.onFailure { t ->
                _state.value = PlantListUiState.Error(t.message ?: "Failed to load plants")
            }
        }
    }
}

private fun Plant.toCard(species: Species?, next: CareTask?, now: Instant): PlantCardUi {
    val label = next?.let {
        val days = max(0, ((it.scheduledAt - now).inWholeHours / 24).toInt())
        when (days) {
            0 -> "${it.type.display()} today"
            1 -> "${it.type.display()} tomorrow"
            else -> "${it.type.display()} in ${days}d"
        }
    } ?: "No scheduled care"
    return PlantCardUi(
        id = id,
        nickname = nickname,
        speciesName = species?.commonName ?: "Unknown species",
        nextTaskLabel = label,
        accent = androidx.compose.ui.graphics.Color(0xFF8BA888),
    )
}

private fun CareTaskType.display(): String = when (this) {
    CareTaskType.WATER -> "Water"
    CareTaskType.FERTILIZE -> "Fertilize"
    CareTaskType.MIST -> "Mist"
    CareTaskType.ROTATE -> "Rotate"
    CareTaskType.REPOT -> "Repot"
    CareTaskType.PRUNE -> "Prune"
}
