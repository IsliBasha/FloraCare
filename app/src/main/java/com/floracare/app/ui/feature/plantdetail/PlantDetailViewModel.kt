package com.floracare.app.ui.feature.plantdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.floracare.app.domain.model.CareAdjustmentReason
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.DiagnosisResult
import com.floracare.app.domain.repository.DiagnosisRepository
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.domain.repository.WeatherRepository
import com.floracare.app.domain.usecase.ComputeNextCareTaskUseCase
import com.floracare.app.domain.usecase.ReenrichPlantSpeciesUseCase
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
import kotlin.time.Duration.Companion.days

sealed interface PlantDetailUiState {
    data object Loading : PlantDetailUiState
    data object NotFound : PlantDetailUiState
    data class Error(val message: String) : PlantDetailUiState
    data class Ready(
        val plant: Plant,
        val species: Species?,
        val upcoming: List<UpcomingTask>,
        val recentDiagnoses: List<DiagnosisResult> = emptyList(),
    ) : PlantDetailUiState
}

data class UpcomingTask(
    val id: String,
    val type: CareTaskType,
    val label: String,
    val reasonLabel: String? = null,
)

/**
 * Drives PlantDetail. Reads the persisted plant + species + open tasks reactively
 * and recomputes the adaptive watering rationale ("delayed by recent rain", etc.)
 * from the live weather snapshots so the badge always reflects the current 3-day
 * window. The persisted [CareTask.scheduledAt] remains canonical — only the
 * reason text adapts.
 *
 * Two-constructor pattern: the primary constructor takes a raw plantId so JVM
 * tests bypass `SavedStateHandle.toRoute(...)`; the [Inject]-annotated secondary
 * constructor decodes the route in production.
 */
@HiltViewModel
class PlantDetailViewModel(
    private val plantId: String,
    private val repo: PlantRepository,
    private val reenrich: ReenrichPlantSpeciesUseCase,
    private val weather: WeatherRepository,
    private val computeNextTask: ComputeNextCareTaskUseCase,
    private val diagnosisRepo: DiagnosisRepository,
) : ViewModel() {

    @Inject constructor(
        savedStateHandle: SavedStateHandle,
        repo: PlantRepository,
        reenrich: ReenrichPlantSpeciesUseCase,
        weather: WeatherRepository,
        computeNextTask: ComputeNextCareTaskUseCase,
        diagnosisRepo: DiagnosisRepository,
    ) : this(
        plantId = savedStateHandle.toRoute<FloraRoute.PlantDetail>().plantId,
        repo = repo,
        reenrich = reenrich,
        weather = weather,
        computeNextTask = computeNextTask,
        diagnosisRepo = diagnosisRepo,
    )

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
            weather.observeRecent(Clock.System.now() - WEATHER_OBSERVATION_DAYS.days),
            diagnosisRepo.observeForPlant(plantId),
        ) { plants, species, openTasks, recentWeather, diagnoses ->
            val plant = plants.firstOrNull { it.id == plantId }
                ?: return@combine PlantDetailUiState.NotFound
            val speciesFor = plant.speciesId?.let { id -> species.firstOrNull { it.id == id } }
            val now = Clock.System.now()
            val recentLogs = repo.recentLogs(plantId, now - LOGS_LOOKBACK_DAYS.days)
            val waterReason = waterReasonLabel(plant, speciesFor, recentLogs, recentWeather, now)
            val upcoming = openTasks
                .filter { it.completedAt == null && !it.isSnoozedAt(now) }
                .sortedBy { it.scheduledAt }
                .take(5)
                .map { it.toUpcoming(now, waterReason) }
            PlantDetailUiState.Ready(
                plant = plant,
                species = speciesFor,
                upcoming = upcoming,
                recentDiagnoses = diagnoses.take(MAX_DIAGNOSES_SHOWN),
            ) as PlantDetailUiState
        }
            .onStart<PlantDetailUiState> { emit(PlantDetailUiState.Loading) }
            .catch { t -> emit(PlantDetailUiState.Error(t.message ?: "Failed to load plant")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = PlantDetailUiState.Loading,
            )

    private fun waterReasonLabel(
        plant: Plant,
        species: Species?,
        recentLogs: List<com.floracare.app.domain.model.CareLog>,
        recentWeather: List<com.floracare.app.domain.model.WeatherSnapshot>,
        now: Instant,
    ): String? {
        if (species == null) return null
        val decision = computeNextTask.decide(
            plant = plant,
            species = species,
            recentLogs = recentLogs,
            recentWeather = recentWeather,
            now = now,
        )
        return decision.reasons.dominantLabel()
    }

    companion object {
        private const val WEATHER_OBSERVATION_DAYS = 7
        private const val LOGS_LOOKBACK_DAYS = 14
        private const val MAX_DIAGNOSES_SHOWN = 3
    }
}

/**
 * Picks one reason to surface in the UI when several modifiers fire at once.
 * Ordering favours the most user-visible weather signals (rain, heat) over
 * derived signals (humidity, soil), matching how a person would explain it
 * out loud: "you got a lot of rain" trumps "the air's been a bit dry".
 */
internal fun Set<CareAdjustmentReason>.dominantLabel(): String? = when {
    contains(CareAdjustmentReason.RAIN) -> "delayed by recent rain"
    contains(CareAdjustmentReason.HEAT) -> "hastened by heat"
    contains(CareAdjustmentReason.LOW_HUMIDITY) -> "hastened by dry air"
    contains(CareAdjustmentReason.DAMP_SOIL) -> "delayed by damp soil"
    else -> null
}

internal fun CareTask.isSnoozedAt(now: Instant): Boolean =
    snoozedUntil?.let { it > now } == true

internal fun CareTask.toUpcoming(
    now: Instant,
    waterReason: String? = null,
    tz: TimeZone = TimeZone.currentSystemDefault(),
): UpcomingTask {
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
    return UpcomingTask(
        id = id,
        type = type,
        label = "$phrase · $verb",
        reasonLabel = if (type == CareTaskType.WATER) waterReason else null,
    )
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
