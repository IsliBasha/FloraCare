package com.floracare.app.domain.repository

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface PlantRepository {
    fun observePlants(): Flow<List<Plant>>
    suspend fun findPlant(id: String): Plant?
    suspend fun upsert(plant: Plant)

    suspend fun findSpecies(id: String): Species?
    suspend fun findSpeciesByScientificName(scientificName: String): Species?
    suspend fun upsertSpecies(species: Species)
    fun observeAllSpecies(): Flow<List<Species>>

    fun observeOpenTasks(plantId: String): Flow<List<CareTask>>
    fun observeAllOpenTasks(): Flow<List<CareTask>>
    suspend fun logCare(entry: CareLog)
    suspend fun recentLogs(plantId: String, since: Instant): List<CareLog>
    fun observeLogsSince(since: Instant): Flow<List<CareLog>>
    suspend fun upsertTask(task: CareTask)
    suspend fun markTaskComplete(taskId: String, at: Instant)
    suspend fun snoozeTask(taskId: String, until: Instant)
}

interface WeatherRepository {
    suspend fun recentWeather(since: Instant): List<WeatherSnapshot>
    suspend fun cache(snapshot: WeatherSnapshot)
}
