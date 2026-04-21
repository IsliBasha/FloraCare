package com.floracare.app.ui.feature.onboarding

import com.floracare.app.test.FakeUserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class OnboardingViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm(initialCompleted: Boolean = false): Pair<OnboardingViewModel, FakeUserPrefs> {
        val prefs = FakeUserPrefs(initial = initialCompleted)
        return OnboardingViewModel(prefs) to prefs
    }

    @Test
    fun `initial state is Welcome with no permissions granted`() {
        val (vm, _) = vm()
        val s = vm.state.value
        assertEquals(OnboardingStep.Welcome, s.step)
        assertFalse(s.notificationsGranted)
        assertFalse(s.cameraGranted)
        assertFalse(s.locationGranted)
        assertFalse(s.finished)
    }

    @Test
    fun `onNext walks Welcome to Permissions to Ready and terminates at Ready`() {
        val (vm, _) = vm()
        vm.onNext(); assertEquals(OnboardingStep.Permissions, vm.state.value.step)
        vm.onNext(); assertEquals(OnboardingStep.Ready, vm.state.value.step)
        vm.onNext(); assertEquals(OnboardingStep.Ready, vm.state.value.step)
    }

    @Test
    fun `onBack from Welcome stays on Welcome`() {
        val (vm, _) = vm()
        vm.onBack()
        assertEquals(OnboardingStep.Welcome, vm.state.value.step)
    }

    @Test
    fun `onBack from Permissions returns to Welcome`() {
        val (vm, _) = vm()
        vm.onNext()
        vm.onBack()
        assertEquals(OnboardingStep.Welcome, vm.state.value.step)
    }

    @Test
    fun `onPermissionResult grants tracks each permission independently`() {
        val (vm, _) = vm()
        vm.onPermissionResult(OnboardingPermission.Notifications, true)
        vm.onPermissionResult(OnboardingPermission.Camera, false)
        vm.onPermissionResult(OnboardingPermission.Location, true)

        val s = vm.state.value
        assertTrue(s.notificationsGranted)
        assertTrue(s.notificationsAsked)
        assertFalse(s.cameraGranted)
        assertTrue(s.cameraAsked)
        assertTrue(s.locationGranted)
        assertTrue(s.locationAsked)
    }

    @Test
    fun `permission denial still marks the permission as asked`() {
        val (vm, _) = vm()
        vm.onPermissionResult(OnboardingPermission.Camera, false)
        val s = vm.state.value
        assertFalse(s.cameraGranted)
        assertTrue(s.cameraAsked)
    }

    @Test
    fun `onPermissionResult does not advance the step`() {
        val (vm, _) = vm()
        vm.onPermissionResult(OnboardingPermission.Notifications, true)
        assertEquals(OnboardingStep.Welcome, vm.state.value.step)
    }

    @Test
    fun `onFinish persists to UserPrefs and sets finished`() = runTest(dispatcher) {
        val (vm, prefs) = vm()
        vm.onFinish()
        assertTrue(prefs.currentValue)
        assertTrue(vm.state.value.finished)
    }

    @Test
    fun `onFinish is idempotent`() = runTest(dispatcher) {
        val (vm, prefs) = vm()
        vm.onFinish()
        vm.onFinish()
        assertTrue(prefs.currentValue)
        assertTrue(vm.state.value.finished)
    }
}
