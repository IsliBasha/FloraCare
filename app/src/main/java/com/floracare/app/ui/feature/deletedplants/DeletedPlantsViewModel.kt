package com.floracare.app.ui.feature.deletedplants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floracare.app.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeletedPlantsViewModel @Inject constructor(
    private val repo: PlantRepository,
) : ViewModel() {

    val state = repo.observeDeletedPlants()
        .map { DeletedPlantsUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeletedPlantsUiState.Loading,
        )

    fun onRestore(plantId: String) {
        viewModelScope.launch { repo.archivePlant(plantId, archived = false) }
    }
}
