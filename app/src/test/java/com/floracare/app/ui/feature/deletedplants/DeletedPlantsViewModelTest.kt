package com.floracare.app.ui.feature.deletedplants

import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.test.FakePlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
class DeletedPlantsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private suspend fun TestScope.keepSubscribed(vm: DeletedPlantsViewModel, block: suspend () -> Unit) {
        val job = launch { vm.state.collect {} }
        try { block() } finally { job.cancel() }
    }

    private fun plant(id: String, nick: String, archived: Boolean = false) = Plant(
        id = id,
        nickname = nick,
        speciesId = null,
        locationTag = LocationTag.INDOOR,
        acquiredAt = Clock.System.now() - 500.hours,
        coverPhotoUri = null,
        notes = "",
        archived = archived,
    )

    @Test
    fun `initial state is Success with empty list when nothing is deleted`() = runTest(dispatcher) {
        val repo = FakePlantRepository()
        val vm = DeletedPlantsViewModel(repo)

        keepSubscribed(vm) {
            val state = vm.state.value
            assertTrue("expected Success, got $state", state is DeletedPlantsUiState.Success)
            assertEquals(emptyList<Plant>(), (state as DeletedPlantsUiState.Success).plants)
        }
    }

    @Test
    fun `state includes only deleted plants, not active ones`() = runTest(dispatcher) {
        val repo = FakePlantRepository()
        repo.plants.value = listOf(
            plant("p1", "Mona", archived = false),
            plant("p2", "Finn", archived = true),
            plant("p3", "Rex", archived = true),
        )
        val vm = DeletedPlantsViewModel(repo)

        keepSubscribed(vm) {
            val state = vm.state.value as DeletedPlantsUiState.Success
            assertEquals(listOf("p2", "p3"), state.plants.map { it.id })
        }
    }

    @Test
    fun `restore calls archivePlant with archived=false`() = runTest(dispatcher) {
        val repo = FakePlantRepository()
        repo.plants.value = listOf(plant("p1", "Mona", archived = true))
        val vm = DeletedPlantsViewModel(repo)

        keepSubscribed(vm) {
            vm.onRestore("p1")
            assertEquals(listOf("p1" to false), repo.archivedCalls)
        }
    }

    @Test
    fun `restored plant disappears from state live`() = runTest(dispatcher) {
        val repo = FakePlantRepository()
        repo.plants.value = listOf(plant("p1", "Mona", archived = true))
        val vm = DeletedPlantsViewModel(repo)

        keepSubscribed(vm) {
            assertEquals(1, (vm.state.value as DeletedPlantsUiState.Success).plants.size)
            vm.onRestore("p1")
            assertEquals(emptyList<Plant>(), (vm.state.value as DeletedPlantsUiState.Success).plants)
        }
    }

    @Test
    fun `newly deleted plant appears in state live`() = runTest(dispatcher) {
        val repo = FakePlantRepository()
        repo.plants.value = listOf(plant("p1", "Mona", archived = false))
        val vm = DeletedPlantsViewModel(repo)

        keepSubscribed(vm) {
            assertEquals(0, (vm.state.value as DeletedPlantsUiState.Success).plants.size)
            repo.archivePlant("p1", true)
            assertEquals(listOf("p1"), (vm.state.value as DeletedPlantsUiState.Success).plants.map { it.id })
        }
    }
}
