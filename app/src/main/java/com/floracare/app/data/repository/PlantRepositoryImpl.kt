package com.floracare.app.data.repository

import com.floracare.app.data.local.CareLogDao
import com.floracare.app.data.local.CareTaskDao
import com.floracare.app.data.local.PlantDao
import com.floracare.app.data.local.SpeciesDao
import com.floracare.app.data.local.WeatherDao
import com.floracare.app.data.local.toDomain
import com.floracare.app.data.local.toEntity
import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.WeatherSnapshot
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.domain.repository.WeatherRepository
import com.floracare.app.data.local.CareTaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlantRepositoryImpl @Inject constructor(
    private val plantDao: PlantDao,
    private val speciesDao: SpeciesDao,
    private val taskDao: CareTaskDao,
    private val logDao: CareLogDao,
) : PlantRepository {

    override fun observePlants(): Flow<List<Plant>> =
        plantDao.observeActive().map { list -> list.map { it.toDomain() } }

    override suspend fun findPlant(id: String): Plant? = plantDao.findById(id)?.toDomain()

    override suspend fun upsert(plant: Plant) = plantDao.upsert(plant.toEntity())

    override suspend fun findSpecies(id: String): Species? = speciesDao.findById(id)?.toDomain()

    override suspend fun findSpeciesByScientificName(scientificName: String): Species? =
        speciesDao.findByScientificName(scientificName)?.toDomain()

    override suspend fun upsertSpecies(species: Species) = speciesDao.upsert(species.toEntity())

    override fun observeAllSpecies(): Flow<List<Species>> =
        speciesDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeOpenTasks(plantId: String): Flow<List<CareTask>> =
        taskDao.observeOpenForPlant(plantId).map { list -> list.map { it.toDomain() } }

    override fun observeAllOpenTasks(): Flow<List<CareTask>> =
        taskDao.observeAllOpen().map { list -> list.map { it.toDomain() } }

    override suspend fun logCare(entry: CareLog) = logDao.insert(entry.toEntity())

    override suspend fun recentLogs(plantId: String, since: Instant): List<CareLog> =
        logDao.findRecent(plantId, since).map { it.toDomain() }

    override fun observeLogsSince(since: Instant): Flow<List<CareLog>> =
        logDao.observeSince(since).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertTask(task: CareTask) = taskDao.upsert(
        CareTaskEntity(
            id = task.id,
            plantId = task.plantId,
            type = task.type,
            scheduledAt = task.scheduledAt,
            completedAt = task.completedAt,
            snoozedUntil = task.snoozedUntil,
            source = task.source,
        )
    )

    override suspend fun markTaskComplete(taskId: String, at: Instant) =
        taskDao.markCompleted(taskId, at)

    override suspend fun snoozeTask(taskId: String, until: Instant) = taskDao.snooze(taskId, until)
}

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherDao: WeatherDao,
) : WeatherRepository {
    override suspend fun recentWeather(since: Instant): List<WeatherSnapshot> =
        weatherDao.findRecent(since).map { it.toDomain() }

    override suspend fun cache(snapshot: WeatherSnapshot) = weatherDao.insert(snapshot.toEntity())
}
