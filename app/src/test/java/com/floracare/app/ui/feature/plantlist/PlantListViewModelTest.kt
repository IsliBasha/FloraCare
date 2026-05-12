package com.floracare.app.ui.feature.plantlist

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.CareTaskSource
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.repository.PlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
class PlantListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Keeps [vm.state] subscribed for the duration of [block] so the cold
     * upstream behind [SharingStarted.WhileSubscribed] actually runs. The
     * subscriber is cancelled after the block returns.
     */
    private fun TestScope.keepSubscribed(vm: PlantListViewModel, block: () -> Unit) {
        val job = launch { vm.state.collect { /* drain */ } }
        try {
            block()
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `initial emission is Success built from the first combined snapshot`() = runTest(dispatcher) {
        val repo = FakePlantRepository()
        repo.plants.value = listOf(plant(id = "pl-1", nick = "Mona"))
        repo.species.value = listOf(species("sp-mon"))
        val vm = PlantListViewModel(repo)

        keepSubscribed(vm) {
            val state = vm.state.value
            assertTrue("expected Success, got $state", state is PlantListUiState.Success)
            val success = state as PlantListUiState.Success
            assertEquals(1, success.plants.size)
            assertEquals("Mona", success.plants.single().nickname)
        }
    }

    @Test
    fun `adding a plant to the repo updates the state live`() = runTest(dispatcher) {
        val repo = FakePlantRepository()
        repo.plants.value = emptyList()
        repo.species.value = emptyList()
        val vm = PlantListViewModel(repo)

        keepSubscribed(vm) {
            assertEquals(0, (vm.state.value as PlantListUiState.Success).plants.size)
            repo.plants.value =
                listOf(plant(id = "pl-1"), plant(id = "pl-2", nick = "Finn"))
            val after = vm.state.value as PlantListUiState.Success
            assertEquals(2, after.plants.size)
        }
    }

    @Test
    fun `marking a task complete removes it from the card label`() = runTest(dispatcher) {
        val repo = FakePlantRepository()
        repo.plants.value = listOf(plant(id = "pl-1"))
        repo.species.value = listOf(species("sp-mon"))
        val task = openTask(
            id = "t1",
            plantId = "pl-1",
            scheduledAt = Clock.System.now() + 1.days,
        )
        repo.openTasks.value = listOf(task)

        val vm = PlantListViewModel(repo)
        keepSubscribed(vm) {
            val before = (vm.state.value as PlantListUiState.Success).plants.single()
            assertTrue(before.nextTaskLabel.startsWith("Water"))

            repo.openTasks.value = emptyList()
            val after = (vm.state.value as PlantListUiState.Success).plants.single()
            assertEquals("No scheduled care", after.nextTaskLabel)
        }
    }

    @Test
    fun `dues today counts tasks scheduled within today only`() = runTest(dispatcher) {
        val repo = FakePlantRepository()
        val now = Clock.System.now()
        repo.plants.value = listOf(
            plant(id = "p1", nick = "A"),
            plant(id = "p2", nick = "B"),
            plant(id = "p3", nick = "C"),
        )
        repo.species.value = listOf(species("sp-mon"))
        repo.openTasks.value = listOf(
            openTask(id = "t1", plantId = "p1", scheduledAt = now), // today
            openTask(id = "t2", plantId = "p2", scheduledAt = now + 1.days),
            openTask(id = "t3", plantId = "p3", scheduledAt = now - 2.days),
        )

        val vm = PlantListViewModel(repo)
        keepSubscribed(vm) {
            val success = vm.state.value as PlantListUiState.Success
            assertEquals(1, success.duesToday)
        }
    }

    private fun plant(id: String = "pl-1", speciesId: String? = "sp-mon", nick: String = "Mona") =
        Plant(
            id = id,
            nickname = nick,
            speciesId = speciesId,
            locationTag = LocationTag.INDOOR,
            acquiredAt = Clock.System.now() - 30.days,
            coverPhotoUri = null,
            notes = "",
        )

    private fun species(id: String) = Species(
        id = id,
        scientificName = "X",
        commonName = "Common",
        waterFrequencyDays = 7,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.MEDIUM,
        temperatureRangeC = 18f..28f,
        toxicity = Toxicity.MILD,
        careNotes = "",
    )

    private fun openTask(id: String, plantId: String, scheduledAt: Instant) = CareTask(
        id = id,
        plantId = plantId,
        type = CareTaskType.WATER,
        scheduledAt = scheduledAt,
        completedAt = null,
        snoozedUntil = null,
        source = CareTaskSource.ADAPTIVE,
    )

    private class FakePlantRepository : PlantRepository {
        val plants = MutableStateFlow<List<Plant>>(emptyList())
        val species = MutableStateFlow<List<Species>>(emptyList())
        val openTasks = MutableStateFlow<List<CareTask>>(emptyList())

        override fun observePlants(): Flow<List<Plant>> = plants
        override fun observeDeletedPlants(): Flow<List<Plant>> = MutableStateFlow(emptyList())
        override suspend fun findPlant(id: String): Plant? = plants.value.firstOrNull { it.id == id }
        override suspend fun upsert(plant: Plant) = Unit
        override suspend fun archivePlant(id: String, archived: Boolean) = Unit
        override suspend fun findSpecies(id: String): Species? =
            species.value.firstOrNull { it.id == id }

        override suspend fun findSpeciesByScientificName(scientificName: String): Species? =
            species.value.firstOrNull {
                it.scientificName.trim().equals(scientificName.trim(), ignoreCase = true)
            }

        override suspend fun upsertSpecies(species: Species) {
            this.species.value = (this.species.value.filterNot { it.id == species.id } + species)
        }

        override fun observeAllSpecies(): Flow<List<Species>> = species
        override fun observeOpenTasks(plantId: String): Flow<List<CareTask>> = openTasks
        override fun observeAllOpenTasks(): Flow<List<CareTask>> = openTasks
        override suspend fun logCare(entry: CareLog) = Unit
        override suspend fun recentLogs(plantId: String, since: Instant): List<CareLog> =
            emptyList()

        override fun observeLogsSince(since: Instant): Flow<List<CareLog>> =
            MutableStateFlow(emptyList())

        override suspend fun upsertTask(task: CareTask) = Unit
        override suspend fun markTaskComplete(taskId: String, at: Instant) = Unit
        override suspend fun snoozeTask(taskId: String, until: Instant) = Unit
    }
}
