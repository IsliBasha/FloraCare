package com.floracare.app.ui.feature.plantdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.domain.usecase.ReenrichPlantSpeciesUseCase
import androidx.navigation.toRoute
import com.floracare.app.ui.navigation.FloraRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

sealed interface PlantDetailUiState {
    data object Loading : PlantDetailUiState
    data object NotFound : PlantDetailUiState
    data class Error(val message: String) : PlantDetailUiState
    data class Ready(
        val plant: Plant,
        val species: Species?,
        val upcoming: List<UpcomingTask>,
    ) : PlantDetailUiState
}

data class UpcomingTask(
    val id: String,
    val type: CareTaskType,
    val label: String,
)

@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PlantRepository,
    private val reenrich: ReenrichPlantSpeciesUseCase,
) : ViewModel() {

    private val plantId: String =
        savedStateHandle.toRoute<FloraRoute.PlantDetail>().plantId

    init {
        // Fire-and-forget — succeeds silently for already-enriched plants
        // and absorbs offline / not-found / lookup errors without surfacing
        // them to the UI. The next combine emission picks up the upgraded
        // species automatically once the plant.speciesId is rewritten.
        viewModelScope.launch {
            runCatching { reenrich(plantId) }
        }
    }

    val state: StateFlow<PlantDetailUiState> =
        combine(
            repo.observePlants(),
            repo.observeAllSpecies(),
            repo.observeOpenTasks(plantId),
        ) { plants, species, openTasks ->
            val plant = plants.firstOrNull { it.id == plantId }
                ?: return@combine PlantDetailUiState.NotFound
            val speciesFor = plant.speciesId?.let { id -> species.firstOrNull { it.id == id } }
            val now = Clock.System.now()
            val upcoming = openTasks
                .filter { it.completedAt == null && !it.isSnoozedAt(now) }
                .sortedBy { it.scheduledAt }
                .take(5)
                .map { it.toUpcoming(now) }
            PlantDetailUiState.Ready(
                plant = plant,
                species = speciesFor,
                upcoming = upcoming,
            ) as PlantDetailUiState
        }
            .onStart<PlantDetailUiState> { emit(PlantDetailUiState.Loading) }
            .catch { t -> emit(PlantDetailUiState.Error(t.message ?: "Failed to load plant")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = PlantDetailUiState.Loading,
            )
}

internal fun CareTask.isSnoozedAt(now: Instant): Boolean =
    snoozedUntil?.let { it > now } == true

internal fun CareTask.toUpcoming(now: Instant, tz: TimeZone = TimeZone.currentSystemDefault()): UpcomingTask {
    val verb = type.verb()
    val today = now.toLocalDateTime(tz).date
    val scheduledDay = scheduledAt.toLocalDateTime(tz).date
    val deltaDays = scheduledDay.toEpochDays() - today.toEpochDays()
    val phrase = when {
        deltaDays == 0 -> "Today"
        deltaDays == 1 -> "Tomorrow"
        deltaDays == -1 -> "1d ago"
        deltaDays < 0 -> "${-deltaDays}d ago"
        else -> "in ${deltaDays}d"
    }
    return UpcomingTask(id = id, type = type, label = "$phrase · $verb")
}

internal fun CareTaskType.verb(): String = when (this) {
    CareTaskType.WATER -> "Water"
    CareTaskType.FERTILIZE -> "Fertilize"
    CareTaskType.MIST -> "Mist"
    CareTaskType.ROTATE -> "Rotate"
    CareTaskType.REPOT -> "Repot"
    CareTaskType.PRUNE -> "Prune"
}

internal fun LocationTag.display(): String = when (this) {
    LocationTag.INDOOR -> "indoor"
    LocationTag.OUTDOOR -> "outdoor"
    LocationTag.GREENHOUSE -> "greenhouse"
}

internal fun LightNeed.display(): String = when (this) {
    LightNeed.LOW -> "low"
    LightNeed.MEDIUM -> "medium"
    LightNeed.HIGH -> "high"
    LightNeed.DIRECT_SUN -> "direct sun"
}

internal fun HumidityNeed.display(): String = when (this) {
    HumidityNeed.LOW -> "low"
    HumidityNeed.MEDIUM -> "moderate"
    HumidityNeed.HIGH -> "high"
}

