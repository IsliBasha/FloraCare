package com.floracare.app.ui.feature.identify

import android.graphics.Bitmap
import com.floracare.app.data.ml.Prediction
import com.floracare.app.data.ml.SpeciesClassifier
import com.floracare.app.domain.usecase.ResolveOrCreateSpeciesUseCase
import com.floracare.app.test.FakePlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import io.mockk.mockk
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IdentifyViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val bitmap: Bitmap = mockk(relaxed = true)

    private fun buildVm(
        classifier: SpeciesClassifier = FakeClassifier(
            listOf(
                Prediction("Monstera deliciosa", 0.9f),
                Prediction("Philodendron hederaceum", 0.05f),
                Prediction("Epipremnum aureum", 0.03f),
            ),
        ),
        repo: FakePlantRepository = FakePlantRepository(),
        idGen: () -> String = { "pl-fixed" },
    ): Pair<IdentifyViewModel, FakePlantRepository> {
        val useCase = ResolveOrCreateSpeciesUseCase(repo).apply { idGenerator = { "sp-gen" } }
        val vm = IdentifyViewModel(
            classifier = classifier,
            resolveSpecies = useCase,
            plants = repo,
        ).apply { plantIdGenerator = idGen }
        return vm to repo
    }

    @Test
    fun `initial state is RequestPermission`() {
        val (vm, _) = buildVm()
        assertEquals(IdentifyUiState.RequestPermission, vm.state.value)
    }

    @Test
    fun `onPermissionGranted transitions to Ready`() {
        val (vm, _) = buildVm()
        vm.onPermissionGranted()
        assertEquals(IdentifyUiState.Ready, vm.state.value)
    }

    @Test
    fun `onPermissionDenied transitions to PermissionDenied`() {
        val (vm, _) = buildVm()
        vm.onPermissionDenied()
        assertEquals(IdentifyUiState.PermissionDenied, vm.state.value)
    }

    @Test
    fun `onCaptureStart from Ready transitions to Capturing`() {
        val (vm, _) = buildVm()
        vm.onPermissionGranted()
        vm.onCaptureStart()
        assertEquals(IdentifyUiState.Capturing, vm.state.value)
    }

    @Test
    fun `bitmap with predictions leads to Picker state`() = runTest(dispatcher) {
        val (vm, _) = buildVm()
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)

        val s = vm.state.value
        assertTrue("expected Picker, got $s", s is IdentifyUiState.Picker)
        assertEquals(3, (s as IdentifyUiState.Picker).predictions.size)
    }

    @Test
    fun `empty predictions lead to Error state`() = runTest(dispatcher) {
        val (vm, _) = buildVm(classifier = FakeClassifier(emptyList()))
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)

        assertTrue(vm.state.value is IdentifyUiState.Error)
    }

    @Test
    fun `classifier failure surfaces as Error`() = runTest(dispatcher) {
        val boom = object : SpeciesClassifier {
            override val isReady: Boolean = false
            override suspend fun topK(bitmap: Bitmap, k: Int): List<Prediction> =
                throw IllegalStateException("model broken")
        }
        val (vm, _) = buildVm(classifier = boom)
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)

        val s = vm.state.value
        assertTrue(s is IdentifyUiState.Error)
        assertEquals("model broken", (s as IdentifyUiState.Error).message)
    }

    @Test
    fun `Picker onSelect moves to Naming and preserves predictions`() = runTest(dispatcher) {
        val (vm, _) = buildVm()
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        val preds = (vm.state.value as IdentifyUiState.Picker).predictions

        vm.onPredictionSelected(preds[1])

        val s = vm.state.value as IdentifyUiState.Naming
        assertEquals("Philodendron hederaceum", s.selectedLabel)
        assertEquals(preds, s.predictions)
    }

    @Test
    fun `Naming onCancel returns to Picker with the same predictions`() = runTest(dispatcher) {
        val (vm, _) = buildVm()
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        val preds = (vm.state.value as IdentifyUiState.Picker).predictions
        vm.onPredictionSelected(preds[0])

        vm.onNamingCancelled()

        val picker = vm.state.value as IdentifyUiState.Picker
        assertEquals(preds, picker.predictions)
    }

    @Test
    fun `onSave with valid nickname creates species + plant and reaches Saved`() =
        runTest(dispatcher) {
            val (vm, repo) = buildVm()
            vm.onPermissionGranted()
            vm.onBitmapCaptured(bitmap)
            val preds = (vm.state.value as IdentifyUiState.Picker).predictions
            vm.onPredictionSelected(preds.first())

            vm.onSave("Mona")

            val saved = vm.state.value as IdentifyUiState.Saved
            assertEquals("pl-fixed", saved.plantId)
            assertEquals(1, repo.upsertedPlants.size)
            assertEquals("Mona", repo.upsertedPlants.single().nickname)
            assertEquals("sp-gen", repo.upsertedPlants.single().speciesId)
            assertEquals(1, repo.upsertedSpecies.size)
        }

    @Test
    fun `onSave with blank nickname sets Error and does not write`() = runTest(dispatcher) {
        val (vm, repo) = buildVm()
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        val preds = (vm.state.value as IdentifyUiState.Picker).predictions
        vm.onPredictionSelected(preds.first())

        vm.onSave("   ")

        assertTrue(vm.state.value is IdentifyUiState.Error)
        assertEquals(0, repo.upsertedPlants.size)
    }

    @Test
    fun `onRetake from Error returns to Ready`() = runTest(dispatcher) {
        val (vm, _) = buildVm(classifier = FakeClassifier(emptyList()))
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        assertTrue(vm.state.value is IdentifyUiState.Error)

        vm.onRetake()
        assertEquals(IdentifyUiState.Ready, vm.state.value)
    }

    private class FakeClassifier(private val preds: List<Prediction>) : SpeciesClassifier {
        override val isReady: Boolean = true
        override suspend fun topK(bitmap: Bitmap, k: Int): List<Prediction> =
            preds.take(k)
    }
}
