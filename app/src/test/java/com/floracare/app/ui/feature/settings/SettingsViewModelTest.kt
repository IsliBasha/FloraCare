package com.floracare.app.ui.feature.settings

import com.floracare.app.domain.model.AppPreferences
import com.floracare.app.domain.model.TemperatureUnit
import com.floracare.app.domain.model.ThemeMode
import com.floracare.app.test.FakeUserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.keepSubscribed(
        vm: SettingsViewModel,
        block: suspend () -> Unit,
    ) {
        val job = launch { vm.state.collect { /* drain */ } }
        try {
            block()
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `initial state mirrors UserPrefs defaults`() = runTest(dispatcher) {
        val prefs = FakeUserPrefs()
        val vm = SettingsViewModel(prefs, appVersion = "1.0.0")

        keepSubscribed(vm) {
            val state = vm.state.value
            assertEquals(ThemeMode.SYSTEM, state.preferences.themeMode)
            assertEquals(TemperatureUnit.CELSIUS, state.preferences.temperatureUnit)
            assertTrue(state.preferences.notificationsEnabled)
            assertEquals("1.0.0", state.appVersion)
        }
    }

    @Test
    fun `setting theme writes through to UserPrefs and updates state`() = runTest(dispatcher) {
        val prefs = FakeUserPrefs()
        val vm = SettingsViewModel(prefs, appVersion = "1.0.0")

        keepSubscribed(vm) {
            vm.onEvent(SettingsEvent.SetThemeMode(ThemeMode.DARK))

            assertEquals(ThemeMode.DARK, prefs.currentAppPrefs.themeMode)
            assertEquals(ThemeMode.DARK, vm.state.value.preferences.themeMode)
        }
    }

    @Test
    fun `setting temperature unit writes through and updates state`() = runTest(dispatcher) {
        val prefs = FakeUserPrefs()
        val vm = SettingsViewModel(prefs, appVersion = "1.0.0")

        keepSubscribed(vm) {
            vm.onEvent(SettingsEvent.SetTemperatureUnit(TemperatureUnit.FAHRENHEIT))

            assertEquals(TemperatureUnit.FAHRENHEIT, prefs.currentAppPrefs.temperatureUnit)
            assertEquals(TemperatureUnit.FAHRENHEIT, vm.state.value.preferences.temperatureUnit)
        }
    }

    @Test
    fun `toggling notifications writes through and updates state`() = runTest(dispatcher) {
        val prefs = FakeUserPrefs()
        val vm = SettingsViewModel(prefs, appVersion = "1.0.0")

        keepSubscribed(vm) {
            vm.onEvent(SettingsEvent.SetNotificationsEnabled(false))

            assertFalse(prefs.currentAppPrefs.notificationsEnabled)
            assertFalse(vm.state.value.preferences.notificationsEnabled)
        }
    }

    @Test
    fun `state reflects upstream pref changes from outside the VM`() = runTest(dispatcher) {
        val prefs = FakeUserPrefs(
            initialAppPrefs = AppPreferences(themeMode = ThemeMode.LIGHT),
        )
        val vm = SettingsViewModel(prefs, appVersion = "1.0.0")

        keepSubscribed(vm) {
            // initial reads LIGHT
            assertEquals(ThemeMode.LIGHT, vm.state.value.preferences.themeMode)

            // a different surface (e.g. quick-settings tile) flips it
            prefs.setThemeMode(ThemeMode.DARK)

            assertEquals(ThemeMode.DARK, vm.state.value.preferences.themeMode)
        }
    }
}
