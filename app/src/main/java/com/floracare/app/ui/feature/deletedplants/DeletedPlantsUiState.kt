package com.floracare.app.ui.feature.deletedplants

import com.floracare.app.domain.model.Plant

sealed interface DeletedPlantsUiState {
    data object Loading : DeletedPlantsUiState
    data class Success(val plants: List<Plant>) : DeletedPlantsUiState
}
