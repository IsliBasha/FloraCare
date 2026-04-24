package com.floracare.app.ui.feature.addplant

import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.usecase.ResolveOrCreateSpeciesUseCase
import com.floracare.app.domain.usecase.SpeciesLookupUseCase
import com.floracare.app.test.FakePlantRepository
import com.floracare.app.test.FakeSpeciesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddPlantManualViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val fixedNow = Instant.fromEpochMilliseconds(1_700_000_000_000L)
    private val fixedClock = object : Clock { override fun now(): Instant = fixedNow }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun species(
        id: String,
        common: String,
        scientific: String,
    ) = Species(
        id = id,
        scientificName = scientific,
        commonName = common,
        waterFrequencyDays = 7,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.MEDIUM,
        temperatureRangeC = 15f..28f,
        toxicity = Toxicity.NONE,
        careNotes = "",
    )

    private fun vm(
        seededSpecies: List<Species> = emptyList(),
    ): Pair<AddPlantManualViewModel, FakePlantRepository> {
        val repo = FakePlantRepository().apply { species.value = seededSpecies }
        val lookup = SpeciesLookupUseCase(FakeSpeciesRepository())
        val useCase = ResolveOrCreateSpeciesUseCase(repo, lookup).apply { idGenerator = { "sp-gen" } }
        val vm = AddPlantManualViewModel(repo, useCase).apply {
            plantIdGenerator = { "pl-fixed" }
            clock = fixedClock
        }
        // Re-emit acquiredAt since init ran with the default Clock before we swapped it.
        vm.onAcquiredAtChange(fixedNow)
        return vm to repo
    }

    @Test
    fun `initial state has clean defaults and acquiredAt set to now`() {
        val (vm, _) = vm()
        val s = vm.state.value
        assertEquals("", s.nickname)
        assertEquals("", s.speciesQuery)
        assertEquals(LocationTag.INDOOR, s.locationTag)
        assertEquals(fixedNow, s.acquiredAt)
        assertNull(s.savedPlantId)
        assertNull(s.errorMessage)
    }

    @Test
    fun `onSubmit with blank nickname sets error and does not persist`() = runTest(dispatcher) {
        val (vm, repo) = vm()
        vm.onSubmit()
        assertEquals("Give your plant a name", vm.state.value.nicknameError)
        assertTrue(repo.upsertedPlants.isEmpty())
    }

    @Test
    fun `typing nickname clears the error`() {
        val (vm, _) = vm()
        vm.onSubmit() // sets error
        vm.onNicknameChange("Ivy")
        assertNull(vm.state.value.nicknameError)
    }

    @Test
    fun `species query filters matches case-insensitively by common and scientific name`() {
        val (vm, _) = vm(
            seededSpecies = listOf(
                species("sp-1", "Monstera", "Monstera deliciosa"),
                species("sp-2", "Pothos", "Epipremnum aureum"),
                species("sp-3", "Calathea", "Calathea orbifolia"),
            ),
        )
        vm.onSpeciesQueryChange("mon")
        assertEquals(1, vm.state.value.speciesMatches.size)
        assertEquals("sp-1", vm.state.value.speciesMatches[0].id)

        vm.onSpeciesQueryChange("aur")
        assertEquals("sp-2", vm.state.value.speciesMatches[0].id)
    }

    @Test
    fun `empty species query produces no matches`() {
        val (vm, _) = vm(seededSpecies = listOf(species("sp-1", "Monstera", "Monstera deliciosa")))
        vm.onSpeciesQueryChange("")
        assertTrue(vm.state.value.speciesMatches.isEmpty())
    }

    @Test
    fun `manual picker hides the AIY background species even when the query matches`() {
        val (vm, _) = vm(
            seededSpecies = listOf(
                species("sp-bg", common = "background", scientific = "background"),
                species("sp-1", common = "Monstera", scientific = "Monstera deliciosa"),
            ),
        )
        // Substring search for "back" would otherwise surface the background row.
        vm.onSpeciesQueryChange("back")
        assertTrue(
            "background species must be filtered out of the manual picker",
            vm.state.value.speciesMatches.isEmpty(),
        )
        // Normal species lookup still works alongside the filter.
        vm.onSpeciesQueryChange("mon")
        assertEquals(1, vm.state.value.speciesMatches.size)
        assertEquals("sp-1", vm.state.value.speciesMatches[0].id)
    }

    @Test
    fun `selecting a species clears matches and sets the query to common name`() {
        val (vm, _) = vm(seededSpecies = listOf(species("sp-1", "Monstera", "Monstera deliciosa")))
        vm.onSpeciesQueryChange("mon")
        val target = vm.state.value.speciesMatches.first()
        vm.onSpeciesSelected(target)

        val s = vm.state.value
        assertEquals("Monstera", s.speciesQuery)
        assertEquals(target, s.selectedSpecies)
        assertTrue(s.speciesMatches.isEmpty())
    }

    @Test
    fun `typing after selection clears the selection so user can change their mind`() {
        val (vm, _) = vm(seededSpecies = listOf(species("sp-1", "Monstera", "Monstera deliciosa")))
        vm.onSpeciesQueryChange("mon")
        vm.onSpeciesSelected(vm.state.value.speciesMatches.first())
        vm.onSpeciesQueryChange("cal")
        assertNull(vm.state.value.selectedSpecies)
    }

    @Test
    fun `submit with selected species persists plant with that species id`() = runTest(dispatcher) {
        val (vm, repo) = vm(seededSpecies = listOf(species("sp-1", "Monstera", "Monstera deliciosa")))
        vm.onSpeciesQueryChange("mon")
        vm.onSpeciesSelected(vm.state.value.speciesMatches.first())
        vm.onNicknameChange("Mona")
        vm.onSubmit()

        assertEquals(1, repo.upsertedPlants.size)
        val saved = repo.upsertedPlants.single()
        assertEquals("Mona", saved.nickname)
        assertEquals("sp-1", saved.speciesId)
        assertEquals(LocationTag.INDOOR, saved.locationTag)
        assertEquals(fixedNow, saved.acquiredAt)
        assertEquals("pl-fixed", vm.state.value.savedPlantId)
    }

    @Test
    fun `submit with free-text species resolves a new species row before plant`() =
        runTest(dispatcher) {
            val (vm, repo) = vm()
            vm.onSpeciesQueryChange("Ficus lyrata")
            vm.onNicknameChange("Lyra")
            vm.onSubmit()

            assertEquals(1, repo.upsertedSpecies.size)
            assertEquals("Ficus lyrata", repo.upsertedSpecies.single().scientificName)

            assertEquals(1, repo.upsertedPlants.size)
            assertEquals("sp-gen", repo.upsertedPlants.single().speciesId)
        }

    @Test
    fun `submit with empty species query persists plant with null speciesId`() =
        runTest(dispatcher) {
            val (vm, repo) = vm()
            vm.onNicknameChange("Nameless")
            vm.onSubmit()

            assertTrue(repo.upsertedSpecies.isEmpty())
            assertEquals(1, repo.upsertedPlants.size)
            assertNull(repo.upsertedPlants.single().speciesId)
        }

    @Test
    fun `submit is ignored once plant was saved`() = runTest(dispatcher) {
        val (vm, repo) = vm()
        vm.onNicknameChange("One")
        vm.onSubmit()
        assertNotNull(vm.state.value.savedPlantId)

        vm.onSubmit()
        assertEquals(1, repo.upsertedPlants.size)
    }

    @Test
    fun `location tag change is reflected in saved plant`() = runTest(dispatcher) {
        val (vm, repo) = vm()
        vm.onNicknameChange("Outside Ivy")
        vm.onLocationTagChange(LocationTag.OUTDOOR)
        vm.onSubmit()
        assertEquals(LocationTag.OUTDOOR, repo.upsertedPlants.single().locationTag)
    }
}
