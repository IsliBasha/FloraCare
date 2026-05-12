package com.floracare.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface FloraRoute {
    @Serializable data object Onboarding : FloraRoute
    @Serializable data object PlantList : FloraRoute
    @Serializable data class PlantDetail(val plantId: String) : FloraRoute
    @Serializable data object AddPlant : FloraRoute
    @Serializable data object AddPlantManual : FloraRoute
    @Serializable data object Identify : FloraRoute
    @Serializable data class Diagnose(val plantId: String? = null) : FloraRoute
    @Serializable data class Journal(val plantId: String) : FloraRoute
    @Serializable data object Dashboard : FloraRoute
    @Serializable data object Settings : FloraRoute
    @Serializable data object DeletedPlants : FloraRoute
    @Serializable data class EditPlant(val plantId: String) : FloraRoute
}
