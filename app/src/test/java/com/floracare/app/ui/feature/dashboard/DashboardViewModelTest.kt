package com.floracare.app.ui.feature.dashboard

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTaskType
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
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.hours

/**
 * Shape-only VM tests — correctness of the roll-up is already covered by
 * [DashboardMappingTest]. Log timestamps are computed from the live
 * [Clock.System.now] so they land inside the VM's 30-day window regardless
 * of when the test actually runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.keepSubscribed(vm: DashboardViewModel, block: () -> Unit) {
        val job = launch { vm.state.collect { /* drain */ } }
        try {
            block()
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `initial emission is Success with empty snapshot when repo is empty`() = runTest(dispatcher) {
        val repo = FakePlantRepository()
        val vm = DashboardViewModel(repo)

        keepSubscribed(vm) {
            val state = vm.state.value
            assertTrue("expected Success, got $state", state is DashboardUiState.Success)
            val snap = (state as DashboardUiState.Success).snapshot
            assertEquals(DASHBOARD_WINDOW_DAYS, snap.dailyWaterCounts.size)
            assertEquals(0, snap.totalWatersLast30d)
            assertEquals(0, snap.currentStreakDays)
        }
    }

    @Test
    fun `adding a water log live-updates totals and plant of the month`() = runTest(dispatcher) {
        val repo = FakePlantRepository()
        repo.plants.value = listOf(plant("p1", "Mona"))
        val vm = DashboardViewModel(repo)

        keepSubscribed(vm) {
            val before = (vm.state.value as DashboardUiState.Success).snapshot
            assertEquals(0, before.totalWatersLast30d)

            val now = Clock.System.now()
            repo.logs.value = listOf(
                waterLog("l1", "p1", at = now),
                waterLog("l2", "p1", at = now - 24.hours),
            )

            val after = (vm.state.value as DashboardUiState.Success).snapshot
            assertEquals(2, after.totalWatersLast30d)
            assertEquals("p1", after.plantOfTheMonth?.plantId)
            assertEquals(2, after.plantOfTheMonth?.waterCount)
        }
    }

    private fun plant(id: String, nick: String) = Plant(
        id = id,
        nickname = nick,
        speciesId = null,
        locationTag = LocationTag.INDOOR,
        acquiredAt = Clock.System.now() - 500.hours,
        coverPhotoUri = null,
        notes = "",
    )

    private fun waterLog(id: String, plantId: String, at: Instant) = CareLog(
        id = id,
        plantId = plantId,
        taskType = CareTaskType.WATER,
        performedAt = at,
        soilMoistureNote = null,
        userNote = null,
    )
}
