package com.floracare.app.ui

import com.floracare.app.test.FakeUserPrefs
import com.floracare.app.ui.navigation.FloraRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `first launch resolves to Onboarding`() = runTest(dispatcher) {
        val prefs = FakeUserPrefs(initial = false)
        val vm = AppStartViewModel(prefs)
        assertEquals(FloraRoute.Onboarding, vm.startDestination.value)
    }

    @Test
    fun `returning user resolves to PlantList`() = runTest(dispatcher) {
        val prefs = FakeUserPrefs(initial = true)
        val vm = AppStartViewModel(prefs)
        assertEquals(FloraRoute.PlantList, vm.startDestination.value)
    }
}
