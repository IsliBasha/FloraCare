package com.floracare.app.ui.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.domain.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val snapshot: DashboardSnapshot) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: PlantRepository,
    private val weather: WeatherRepository,
) : ViewModel() {

    val state: StateFlow<DashboardUiState> =
        flow<DashboardUiState> {
            emit(DashboardUiState.Loading)
            val now = Clock.System.now()
            val windowStart = now - DASHBOARD_WINDOW_DAYS.days
            // Look back further than the dashboard window for weather so the
            // freshness check in `toDashboard` (24h) is the single source of
            // truth for what counts as "current". Anything older than 7 days
            // is irrelevant either way.
            val weatherWindowStart = now - 7.days
            emitAll(
                combine(
                    repo.observeLogsSince(windowStart),
                    repo.observePlants(),
                    repo.observeAllSpecies(),
                    weather.observeRecent(weatherWindowStart),
                ) { logs, plants, species, weatherSnapshots ->
                    DashboardUiState.Success(
                        toDashboard(
                            logs = logs,
                            plants = plants,
                            species = species,
                            weather = weatherSnapshots,
                            now = Clock.System.now(),
                        ),
                    ) as DashboardUiState
                },
            )
        }
            .catch { t -> emit(DashboardUiState.Error(t.message ?: "Failed to load dashboard")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = DashboardUiState.Loading,
            )
}
