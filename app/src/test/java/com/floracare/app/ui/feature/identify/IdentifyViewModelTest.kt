package com.floracare.app.ui.feature.identify

import android.graphics.Bitmap
import com.floracare.app.data.ml.Prediction
import com.floracare.app.data.ml.SpeciesClassifier
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.repository.SpeciesLookupResult
import com.floracare.app.domain.usecase.ResolveOrCreateSpeciesUseCase
import com.floracare.app.domain.usecase.SpeciesLookupUseCase
import com.floracare.app.test.FakePlantRepository
import com.floracare.app.test.FakeSpeciesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
        speciesRepo: FakeSpeciesRepository = FakeSpeciesRepository(),
        idGen: () -> String = { "pl-fixed" },
    ): Triple<IdentifyViewModel, FakePlantRepository, FakeSpeciesRepository> {
        val lookup = SpeciesLookupUseCase(speciesRepo)
        val useCase = ResolveOrCreateSpeciesUseCase(repo, lookup).apply { idGenerator = { "sp-gen" } }
        val vm = IdentifyViewModel(
            classifier = classifier,
            resolveSpecies = useCase,
            speciesLookup = lookup,
            plants = repo,
        ).apply { plantIdGenerator = idGen }
        return Triple(vm, repo, speciesRepo)
    }

    @Test
    fun `initial state is RequestPermission`() {
        val (vm, _, _) = buildVm()
        assertEquals(IdentifyUiState.RequestPermission, vm.state.value)
    }

    @Test
    fun `onPermissionGranted transitions to Ready`() {
        val (vm, _, _) = buildVm()
        vm.onPermissionGranted()
        assertEquals(IdentifyUiState.Ready, vm.state.value)
    }

    @Test
    fun `onPermissionDenied transitions to PermissionDenied`() {
        val (vm, _, _) = buildVm()
        vm.onPermissionDenied()
        assertEquals(IdentifyUiState.PermissionDenied, vm.state.value)
    }

    @Test
    fun `onCaptureStart from Ready transitions to Capturing`() {
        val (vm, _, _) = buildVm()
        vm.onPermissionGranted()
        vm.onCaptureStart()
        assertEquals(IdentifyUiState.Capturing, vm.state.value)
    }

    @Test
    fun `bitmap with predictions leads to Picker state`() = runTest(dispatcher) {
        val (vm, _, _) = buildVm()
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)

        val s = vm.state.value
        assertTrue("expected Picker, got $s", s is IdentifyUiState.Picker)
        assertEquals(3, (s as IdentifyUiState.Picker).predictions.size)
        assertTrue("top1=0.9 should not flag low confidence", !s.lowConfidence)
    }

    @Test
    fun `top1 below threshold flags low confidence`() = runTest(dispatcher) {
        val (vm, _, _) = buildVm(
            classifier = FakeClassifier(
                listOf(
                    Prediction("Maranta leuconeura", 0.08f),
                    Prediction("Calathea orbifolia", 0.05f),
                    Prediction("Stromanthe sanguinea", 0.03f),
                ),
            ),
        )
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)

        val s = vm.state.value as IdentifyUiState.Picker
        assertTrue("top1=0.08 should flag low confidence", s.lowConfidence)
    }

    @Test
    fun `empty predictions lead to Error state`() = runTest(dispatcher) {
        val (vm, _, _) = buildVm(classifier = FakeClassifier(emptyList()))
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
        val (vm, _, _) = buildVm(classifier = boom)
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)

        val s = vm.state.value
        assertTrue(s is IdentifyUiState.Error)
        assertEquals("model broken", (s as IdentifyUiState.Error).message)
    }

    @Test
    fun `Picker onSelect moves to Naming and preserves predictions`() = runTest(dispatcher) {
        val (vm, _, _) = buildVm()
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
        val (vm, _, _) = buildVm()
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
            val (vm, repo, _) = buildVm()
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
        val (vm, repo, _) = buildVm()
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
        val (vm, _, _) = buildVm(classifier = FakeClassifier(emptyList()))
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        assertTrue(vm.state.value is IdentifyUiState.Error)

        vm.onRetake()
        assertEquals(IdentifyUiState.Ready, vm.state.value)
    }

    private val lowConfPreds = listOf(
        Prediction("Maranta leuconeura", 0.08f),
        Prediction("Calathea orbifolia", 0.05f),
        Prediction("Stromanthe sanguinea", 0.03f),
    )

    private fun perenualSpecies(id: String, scientific: String) = Species(
        id = id,
        scientificName = scientific,
        commonName = scientific,
        waterFrequencyDays = 3,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.HIGH,
        temperatureRangeC = 15f..28f,
        toxicity = Toxicity.NONE,
        careNotes = "Fetched from Perenual",
        provider = Species.PROVIDER_PERENUAL,
        providerSpeciesId = id.removePrefix("sp-perenual-"),
    )

    @Test
    fun `low-confidence pick transitions through Enriching into Naming`() = runTest(dispatcher) {
        val enriched = perenualSpecies("sp-perenual-1", "Maranta leuconeura")
        val (vm, _, _) = buildVm(
            classifier = FakeClassifier(lowConfPreds),
            speciesRepo = FakeSpeciesRepository(SpeciesLookupResult.Fresh(enriched)),
        )
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        val preds = (vm.state.value as IdentifyUiState.Picker).predictions

        vm.onPredictionSelected(preds.first())

        // UnconfinedTestDispatcher keeps the coroutine eager; by the time this
        // line runs we're already in Naming.
        val naming = vm.state.value as IdentifyUiState.Naming
        assertEquals("Maranta leuconeura", naming.selectedLabel)
    }

    @Test
    fun `low-confidence pick with Offline lookup still advances to Naming silently`() =
        runTest(dispatcher) {
            val (vm, _, _) = buildVm(
                classifier = FakeClassifier(lowConfPreds),
                speciesRepo = FakeSpeciesRepository(SpeciesLookupResult.Offline(cached = null)),
            )
            vm.onPermissionGranted()
            vm.onBitmapCaptured(bitmap)
            val preds = (vm.state.value as IdentifyUiState.Picker).predictions

            vm.onPredictionSelected(preds.first())

            assertTrue(
                "offline failure must not surface an Error overlay in the picker flow",
                vm.state.value is IdentifyUiState.Naming,
            )
        }

    @Test
    fun `onEnrichingCancelled from Picker is a no-op`() = runTest(dispatcher) {
        val (vm, _, _) = buildVm(
            classifier = FakeClassifier(lowConfPreds),
            speciesRepo = FakeSpeciesRepository(SpeciesLookupResult.NotFound),
        )
        vm.onPermissionGranted()
        vm.onBitmapCaptured(bitmap)
        val before = vm.state.value as IdentifyUiState.Picker
        assertTrue("sanity: low confidence", before.lowConfidence)

        vm.onEnrichingCancelled()
        assertTrue(vm.state.value is IdentifyUiState.Picker)
    }

    @Test
    fun `onEnrichingCancelled with in-flight lookup returns to Picker`() {
        // Uses a StandardTestDispatcher so we can pause between the state
        // transition and the lookup completion.
        val stepping = StandardTestDispatcher()
        Dispatchers.setMain(stepping)
        try {
            runTest(stepping) {
                val gate = CompletableDeferred<Unit>()
                val speciesRepo = FakeSpeciesRepository(SpeciesLookupResult.NotFound).apply {
                    pendingGate = gate
                }
                val repo = FakePlantRepository()
                val lookup = SpeciesLookupUseCase(speciesRepo)
                val resolve = ResolveOrCreateSpeciesUseCase(repo, lookup)
                    .apply { idGenerator = { "sp-gen" } }
                val vm = IdentifyViewModel(
                    classifier = FakeClassifier(lowConfPreds),
                    resolveSpecies = resolve,
                    speciesLookup = lookup,
                    plants = repo,
                ).apply { plantIdGenerator = { "pl-fixed" } }

                vm.onPermissionGranted()
                vm.onBitmapCaptured(bitmap)
                advanceUntilIdle()
                val preds = (vm.state.value as IdentifyUiState.Picker).predictions

                vm.onPredictionSelected(preds.first())
                advanceUntilIdle() // lookup starts, suspends on the gate
                assertTrue(
                    "VM should be Enriching while the lookup is pending",
                    vm.state.value is IdentifyUiState.Enriching,
                )

                vm.onEnrichingCancelled()
                gate.complete(Unit) // unblock the fake so the cancelled job can unwind
                advanceUntilIdle()

                val picker = vm.state.value as IdentifyUiState.Picker
                assertTrue(
                    "cancel must preserve the low-confidence hint",
                    picker.lowConfidence,
                )
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeClassifier(private val preds: List<Prediction>) : SpeciesClassifier {
        override val isReady: Boolean = true
        override suspend fun topK(bitmap: Bitmap, k: Int): List<Prediction> =
            preds.take(k)
    }
}
