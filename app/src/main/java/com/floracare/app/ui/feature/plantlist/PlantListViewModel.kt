package com.floracare.app.ui.feature.plantlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floracare.app.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

sealed interface PlantListUiState {
    data object Loading : PlantListUiState
    data class Success(
        val plants: List<PlantCardUi>,
        val duesToday: Int,
    ) : PlantListUiState
    data class Error(val message: String) : PlantListUiState
}

@HiltViewModel
class PlantListViewModel @Inject constructor(
    private val repo: PlantRepository,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun onRefresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(600)
            _isRefreshing.value = false
        }
    }

    val state: StateFlow<PlantListUiState> =
        combine(
            repo.observePlants(),
            repo.observeAllSpecies(),
            repo.observeAllOpenTasks(),
        ) { plants, species, tasks ->
            val rollup = toPlantCards(
                plants = plants,
                species = species,
                openTasks = tasks,
                now = Clock.System.now(),
            )
            PlantListUiState.Success(
                plants = rollup.cards,
                duesToday = rollup.duesToday,
            ) as PlantListUiState
        }
            .onStart<PlantListUiState> { emit(PlantListUiState.Loading) }
            .catch { t ->
                emit(PlantListUiState.Error(t.message ?: "Failed to load plants"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = PlantListUiState.Loading,
            )
}
