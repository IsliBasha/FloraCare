package com.floracare.app.ui.feature.diagnose

import android.graphics.Bitmap
import com.floracare.app.data.ml.DiseaseClassifier
import com.floracare.app.data.ml.Prediction
import com.floracare.app.data.ml.TreatmentProvider
import com.floracare.app.test.FakeDiagnosisRepository
import io.mockk.mockk
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
class DiagnoseViewModelTest {

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

    private val healthyPreds = listOf(
        Prediction("Healthy", 0.87f),
        Prediction("Leaf spot (fungal)", 0.08f),
        Prediction("Nutrient deficiency", 0.05f),
    )

    private val lowConfPreds = listOf(
        Prediction("Leaf spot (fungal)", 0.08f),
        Prediction("Bacterial blight", 0.05f),
        Prediction("Nutrient deficiency", 0.04f),
    )

    private fun buildVm(
        plantId: String? = "plant-1",
        classifier: DiseaseClassifier = FakeClassifier(healthyPreds),
        repo: FakeDiagnosisRepository = FakeDiagnosisRepository(),
        idGen: () -> String = { "dg-fixed" },
    ): Pair<DiagnoseViewModel, FakeDiagnosisRepository> {
        val vm = DiagnoseViewModel(plantId, classifier, repo)
            .apply { diagnosisIdGenerator = idGen }
        return vm to repo
    }

    // ── Permission ──────────────────────────────────────────────────────────

    @Test
    fun `initial state is RequestPermission`() {
        val (vm, _) = buildVm()
        assertEquals(DiagnoseUiState.RequestPermission, vm.state.value)
    }

    @Test
    fun `onPermissionGranted transitions to Ready`() {
        val (vm, _) = buildVm()
        vm.onPermissionGranted()
        assertEquals(DiagnoseUiState.Ready, vm.state.value)
    }

    @Test
    fun `onPermissionDenied transitions to PermissionDenied`() {
        val (vm, _) = buildVm()
        vm.onPermissionDenied()
        assertEquals(DiagnoseUiState.PermissionDenied, vm.state.value)
    }

    // ── Capture ─────────────────────────────────────────────────────────────

    @Test
    fun `onCaptureStart from Ready transitions to Capturing`() {
        val (vm, _) = buildVm()
        vm.onPermissionGranted()
        vm.onCaptureStart()
        assertEquals(DiagnoseUiState.Capturing, vm.state.value)
    }

    @Test
    fun `onCaptureStart from non-Ready is no-op`() {
        val (vm, _) = buildVm()
        vm.onCaptureStart()
        assertEquals(DiagnoseUiState.RequestPermission, vm.state.value)
    }

    // ── Classification ───────────────────────────────────────────────────────

    @Test
    fun `bitmap with high-confidence predictions leads to Picker without lowConfidence flag`() =
        runTest(dispatcher) {
            val (vm, _) = buildVm()
            vm.onPermissionGranted()
            vm.onBitmapCaptured(bitmap)

            val s = vm.state.value
            assertTrue("expected Picker, got $s", s is DiagnoseUiState.Picker)
            s as DiagnoseUiState.Picker
            assertEquals(3, s.predictions.size)
            assertFalse("top1=0.87 should not be low-confidence", s.lowConfidence)
        }

    @Test
    fun `top1 below threshold flags lowConfidence`() = runTest(dispatcher) {
        val (vm, _) = buildVm(classifier = FakeClassifier(lowConfPreds))
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)

        val s = vm.state.value as DiagnoseUiState.Picker
        assertTrue("top1=0.08 should flag low confidence", s.lowConfidence)
    }

    @Test
    fun `empty predictions lead to Error state`() = runTest(dispatcher) {
        val (vm, _) = buildVm(classifier = FakeClassifier(emptyList()))
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        assertTrue(vm.state.value is DiagnoseUiState.Error)
    }

    @Test
    fun `classifier failure surfaces as Error with message`() = runTest(dispatcher) {
        val boom = object : DiseaseClassifier {
            override val isReady = false
            override suspend fun topK(bitmap: Bitmap, k: Int): List<Prediction> =
                throw IllegalStateException("model exploded")
        }
        val (vm, _) = buildVm(classifier = boom)
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)

        val s = vm.state.value as DiagnoseUiState.Error
        assertEquals("model exploded", s.message)
    }

    // ── Selection ────────────────────────────────────────────────────────────

    @Test
    fun `onPredictionSelected moves to Result with correct label and treatment`() =
        runTest(dispatcher) {
            val (vm, _) = buildVm()
            vm.onPermissionGranted()
            vm.onBitmapCaptured(bitmap)
            val preds = (vm.state.value as DiagnoseUiState.Picker).predictions

            vm.onPredictionSelected(preds[1]) // "Leaf spot (fungal)"

            val r = vm.state.value as DiagnoseUiState.Result
            assertEquals("Leaf spot (fungal)", r.diagnosisLabel)
            assertEquals(preds[1].confidence, r.confidence)
            assertEquals(
                TreatmentProvider.suggest("Leaf spot (fungal)"),
                r.treatmentSuggestion,
            )
            assertEquals("plant-1", r.plantId)
        }

    @Test
    fun `onPredictionSelected from non-Picker state is a no-op`() = runTest(dispatcher) {
        val (vm, _) = buildVm()
        vm.onPredictionSelected(healthyPreds.first())
        assertEquals(DiagnoseUiState.RequestPermission, vm.state.value)
    }

    // ── Retake ───────────────────────────────────────────────────────────────

    @Test
    fun `onRetake from Result returns to Ready`() = runTest(dispatcher) {
        val (vm, _) = buildVm()
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        vm.onPredictionSelected((vm.state.value as DiagnoseUiState.Picker).predictions.first())
        assertTrue(vm.state.value is DiagnoseUiState.Result)

        vm.onRetake()
        assertEquals(DiagnoseUiState.Ready, vm.state.value)
    }

    @Test
    fun `onRetake from Error returns to Ready`() = runTest(dispatcher) {
        val (vm, _) = buildVm(classifier = FakeClassifier(emptyList()))
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        assertTrue(vm.state.value is DiagnoseUiState.Error)

        vm.onRetake()
        assertEquals(DiagnoseUiState.Ready, vm.state.value)
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    @Test
    fun `onSave persists DiagnosisResult to repo and reaches Saved`() = runTest(dispatcher) {
        val (vm, repo) = buildVm()
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        vm.onPredictionSelected((vm.state.value as DiagnoseUiState.Picker).predictions.first())

        vm.onSave()

        val saved = vm.state.value as DiagnoseUiState.Saved
        assertEquals("dg-fixed", saved.diagnosisId)
        assertEquals(1, repo.insertedResults.size)
        val persisted = repo.insertedResults.single()
        assertEquals("Healthy", persisted.diagnosisLabel)
        assertEquals("plant-1", persisted.plantId)
        assertEquals(0.87f, persisted.confidence)
    }

    @Test
    fun `onSave with null plantId persists with null plantId`() = runTest(dispatcher) {
        val (vm, repo) = buildVm(plantId = null)
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        vm.onPredictionSelected((vm.state.value as DiagnoseUiState.Picker).predictions.first())

        vm.onSave()

        assertTrue(vm.state.value is DiagnoseUiState.Saved)
        assertTrue(repo.insertedResults.single().plantId == null)
    }

    @Test
    fun `onSave from non-Result state is a no-op`() = runTest(dispatcher) {
        val (vm, repo) = buildVm()
        vm.onPermissionGranted()
        vm.onSave()
        assertEquals(0, repo.insertedResults.size)
        assertEquals(DiagnoseUiState.Ready, vm.state.value)
    }

    @Test
    fun `onSave repo failure surfaces Error`() = runTest(dispatcher) {
        val repo = FakeDiagnosisRepository().apply {
            throwOnInsert = RuntimeException("disk full")
        }
        val (vm, _) = buildVm(repo = repo)
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        vm.onPredictionSelected((vm.state.value as DiagnoseUiState.Picker).predictions.first())

        vm.onSave()

        val err = vm.state.value as DiagnoseUiState.Error
        assertEquals("disk full", err.message)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private class FakeClassifier(private val preds: List<Prediction>) : DiseaseClassifier {
        override val isReady = true
        override suspend fun topK(bitmap: Bitmap, k: Int): List<Prediction> = preds.take(k)
    }
}
