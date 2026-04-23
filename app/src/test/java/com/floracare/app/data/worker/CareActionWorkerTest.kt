package com.floracare.app.data.worker

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.WeatherSnapshot
import com.floracare.app.domain.repository.PlantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.days

class CareActionWorkerTest {

    private val now = Instant.parse("2026-04-19T07:00:00Z")

    @Test
    fun `mark done calls markTaskComplete with now`() = runTest {
        val repo = FakePlantRepository()

        val outcome = applyCareAction(
            plants = repo,
            taskId = "task-1",
            action = CareActionWorker.ACTION_MARK_DONE,
            now = now,
        )

        assertEquals(CareActionOutcome.Success, outcome)
        assertEquals(listOf("task-1" to now), repo.completions)
        assertEquals(emptyList<Pair<String, Instant>>(), repo.snoozes)
    }

    @Test
    fun `snooze 2d pushes snoozedUntil forward exactly 48h`() = runTest {
        val repo = FakePlantRepository()

        val outcome = applyCareAction(
            plants = repo,
            taskId = "task-7",
            action = CareActionWorker.ACTION_SNOOZE_2D,
            now = now,
        )

        assertEquals(CareActionOutcome.Success, outcome)
        assertEquals(emptyList<Pair<String, Instant>>(), repo.completions)
        assertEquals(listOf("task-7" to now + 2.days), repo.snoozes)
    }

    @Test
    fun `unknown action returns UnknownAction without touching repo`() = runTest {
        val repo = FakePlantRepository()

        val outcome = applyCareAction(
            plants = repo,
            taskId = "task-9",
            action = "SHRUG",
            now = now,
        )

        assertEquals(CareActionOutcome.UnknownAction, outcome)
        assertEquals(0, repo.completions.size)
        assertEquals(0, repo.snoozes.size)
    }

    @Test
    fun `blank action returns UnknownAction`() = runTest {
        val repo = FakePlantRepository()
        val outcome = applyCareAction(repo, "task-0", action = "", now = now)
        assertEquals(CareActionOutcome.UnknownAction, outcome)
    }

    private class FakePlantRepository : PlantRepository {
        val completions = mutableListOf<Pair<String, Instant>>()
        val snoozes = mutableListOf<Pair<String, Instant>>()

        override fun observePlants(): Flow<List<Plant>> = emptyFlow()
        override suspend fun findPlant(id: String): Plant? = null
        override suspend fun upsert(plant: Plant) = Unit
        override suspend fun findSpecies(id: String): Species? = null
        override suspend fun findSpeciesByScientificName(scientificName: String): Species? = null
        override suspend fun upsertSpecies(species: Species) = Unit
        override fun observeAllSpecies(): Flow<List<Species>> = emptyFlow()
        override fun observeOpenTasks(plantId: String): Flow<List<CareTask>> = emptyFlow()
        override fun observeAllOpenTasks(): Flow<List<CareTask>> = emptyFlow()
        override suspend fun logCare(entry: CareLog) = Unit
        override suspend fun recentLogs(plantId: String, since: Instant): List<CareLog> = emptyList()
        override fun observeLogsSince(since: Instant): Flow<List<CareLog>> = emptyFlow()
        override suspend fun upsertTask(task: CareTask) = Unit

        override suspend fun markTaskComplete(taskId: String, at: Instant) {
            completions += taskId to at
        }

        override suspend fun snoozeTask(taskId: String, until: Instant) {
            snoozes += taskId to until
        }
    }
}
