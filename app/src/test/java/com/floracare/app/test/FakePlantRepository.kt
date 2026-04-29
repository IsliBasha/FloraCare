package com.floracare.app.test

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.repository.PlantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/**
 * Hand-rolled in-memory fake. Instance-isolated; safe to share across tests
 * that each construct their own.
 */
class FakePlantRepository : PlantRepository {
    val plants = MutableStateFlow<List<Plant>>(emptyList())
    val species = MutableStateFlow<List<Species>>(emptyList())
    val openTasks = MutableStateFlow<List<CareTask>>(emptyList())
    val logs = MutableStateFlow<List<CareLog>>(emptyList())

    val upsertedPlants = mutableListOf<Plant>()
    val upsertedSpecies = mutableListOf<Species>()
    val upsertedTasks = mutableListOf<CareTask>()
    val loggedCare = mutableListOf<CareLog>()
    val completedTasks = mutableListOf<Pair<String, Instant>>()
    val snoozedTasks = mutableListOf<Pair<String, Instant>>()
    val archivedCalls = mutableListOf<Pair<String, Boolean>>()

    override fun observePlants(): Flow<List<Plant>> = plants

    override suspend fun findPlant(id: String): Plant? =
        plants.value.firstOrNull { it.id == id }

    override suspend fun upsert(plant: Plant) {
        upsertedPlants += plant
        plants.value = plants.value.filterNot { it.id == plant.id } + plant
    }

    override suspend fun archivePlant(id: String, archived: Boolean) {
        archivedCalls += id to archived
        plants.value = plants.value.map { if (it.id == id) it.copy(archived = archived) else it }
    }

    override suspend fun findSpecies(id: String): Species? =
        species.value.firstOrNull { it.id == id }

    override suspend fun findSpeciesByScientificName(scientificName: String): Species? =
        species.value.firstOrNull {
            it.scientificName.trim().equals(scientificName.trim(), ignoreCase = true)
        }

    override suspend fun upsertSpecies(species: Species) {
        upsertedSpecies += species
        this.species.value =
            this.species.value.filterNot { it.id == species.id } + species
    }

    override fun observeAllSpecies(): Flow<List<Species>> = species

    override fun observeOpenTasks(plantId: String): Flow<List<CareTask>> = openTasks

    override fun observeAllOpenTasks(): Flow<List<CareTask>> = openTasks

    override suspend fun logCare(entry: CareLog) {
        loggedCare += entry
        logs.value = logs.value + entry
    }

    override suspend fun recentLogs(plantId: String, since: Instant): List<CareLog> =
        loggedCare.filter { it.plantId == plantId && it.performedAt >= since }

    override fun observeLogsSince(since: Instant): Flow<List<CareLog>> =
        logs.map { all -> all.filter { it.performedAt >= since } }

    override suspend fun upsertTask(task: CareTask) {
        upsertedTasks += task
        openTasks.value = openTasks.value.filterNot { it.id == task.id } + task
    }

    override suspend fun markTaskComplete(taskId: String, at: Instant) {
        completedTasks += taskId to at
    }

    override suspend fun snoozeTask(taskId: String, until: Instant) {
        snoozedTasks += taskId to until
    }
}
