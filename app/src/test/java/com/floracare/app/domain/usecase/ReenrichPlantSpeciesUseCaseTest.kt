package com.floracare.app.domain.usecase

import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.repository.SpeciesLookupResult
import com.floracare.app.test.FakePlantRepository
import com.floracare.app.test.FakeSpeciesRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReenrichPlantSpeciesUseCaseTest {

    private val now = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    private fun stubSpecies(
        id: String = "sp-local-monstera",
        scientific: String = "Monstera deliciosa",
        commonName: String = "Monstera",
    ) = Species(
        id = id,
        scientificName = scientific,
        commonName = commonName,
        waterFrequencyDays = 7,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.MEDIUM,
        temperatureRangeC = 15f..28f,
        toxicity = Toxicity.NONE,
        careNotes = "synth defaults",
        provider = Species.PROVIDER_LOCAL,
    )

    private fun perenualSpecies(
        id: String = "sp-perenual-42",
        scientific: String = "Monstera deliciosa",
    ) = Species(
        id = id,
        scientificName = scientific,
        commonName = "Swiss cheese plant",
        waterFrequencyDays = 6,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.HIGH,
        temperatureRangeC = 18f..27f,
        toxicity = Toxicity.MILD,
        careNotes = "Perenual notes",
        provider = Species.PROVIDER_PERENUAL,
        providerSpeciesId = "42",
        fetchedAt = now,
        imageUrl = "https://example.test/monstera.jpg",
    )

    private fun plant(id: String = "p1", speciesId: String? = "sp-local-monstera") = Plant(
        id = id,
        nickname = "Mona",
        speciesId = speciesId,
        locationTag = LocationTag.INDOOR,
        acquiredAt = Clock.System.now(),
        coverPhotoUri = null,
        notes = "",
    )

    private fun build(
        plantsRepo: FakePlantRepository = FakePlantRepository(),
        speciesRepo: FakeSpeciesRepository = FakeSpeciesRepository(),
    ): Triple<ReenrichPlantSpeciesUseCase, FakePlantRepository, FakeSpeciesRepository> {
        val useCase = ReenrichPlantSpeciesUseCase(plantsRepo, SpeciesLookupUseCase(speciesRepo))
        return Triple(useCase, plantsRepo, speciesRepo)
    }

    @Test
    fun `local stub plant is upgraded when Perenual returns Fresh`() = runTest {
        val (useCase, plants, species) = build()
        plants.species.value = listOf(stubSpecies())
        plants.plants.value = listOf(plant())
        species.result = SpeciesLookupResult.Fresh(perenualSpecies(id = "sp-perenual-42"))

        val outcome = useCase("p1")

        assertTrue("expected Upgraded, got $outcome", outcome is ReenrichOutcome.Upgraded)
        assertEquals("sp-perenual-42", (outcome as ReenrichOutcome.Upgraded).newSpeciesId)
        assertEquals("sp-perenual-42", plants.upsertedPlants.last().speciesId)
        assertEquals(1, species.lookups.size)
        assertEquals("Monstera deliciosa" to "Monstera", species.lookups.single())
    }

    @Test
    fun `local stub plant is upgraded when Perenual returns Stale cache`() = runTest {
        val (useCase, plants, species) = build()
        plants.species.value = listOf(stubSpecies())
        plants.plants.value = listOf(plant())
        species.result = SpeciesLookupResult.Stale(
            perenualSpecies(id = "sp-perenual-99"),
            com.floracare.app.domain.repository.StaleReason.REMOTE_UNAVAILABLE,
        )

        val outcome = useCase("p1")

        assertTrue(outcome is ReenrichOutcome.Upgraded)
        assertEquals("sp-perenual-99", plants.upsertedPlants.last().speciesId)
    }

    @Test
    fun `Perenual NotFound result leaves the plant unchanged`() = runTest {
        val (useCase, plants, species) = build()
        plants.species.value = listOf(stubSpecies())
        plants.plants.value = listOf(plant())
        species.result = SpeciesLookupResult.NotFound

        val outcome = useCase("p1")

        assertEquals(ReenrichOutcome.NotFound, outcome)
        assertTrue(plants.upsertedPlants.isEmpty())
    }

    @Test
    fun `Perenual Offline result leaves the plant unchanged`() = runTest {
        val (useCase, plants, species) = build()
        plants.species.value = listOf(stubSpecies())
        plants.plants.value = listOf(plant())
        species.result = SpeciesLookupResult.Offline(null)

        val outcome = useCase("p1")

        assertEquals(ReenrichOutcome.Offline, outcome)
        assertTrue(plants.upsertedPlants.isEmpty())
    }

    @Test
    fun `already-enriched Perenual species skips the lookup`() = runTest {
        val (useCase, plants, species) = build()
        plants.species.value = listOf(perenualSpecies(id = "sp-perenual-7"))
        plants.plants.value = listOf(plant(speciesId = "sp-perenual-7"))

        val outcome = useCase("p1")

        assertEquals(ReenrichOutcome.AlreadyEnriched, outcome)
        assertTrue(species.lookups.isEmpty())
        assertTrue(plants.upsertedPlants.isEmpty())
    }

    @Test
    fun `plant without speciesId is skipped`() = runTest {
        val (useCase, plants, species) = build()
        plants.plants.value = listOf(plant(speciesId = null))

        val outcome = useCase("p1")

        assertEquals(ReenrichOutcome.NoSpecies, outcome)
        assertTrue(species.lookups.isEmpty())
    }

    @Test
    fun `missing plant returns NoPlant`() = runTest {
        val (useCase, _, species) = build()

        val outcome = useCase("ghost")

        assertEquals(ReenrichOutcome.NoPlant, outcome)
        assertTrue(species.lookups.isEmpty())
    }

    @Test
    fun `species with blank scientific name is skipped without a network call`() = runTest {
        val (useCase, plants, species) = build()
        plants.species.value = listOf(stubSpecies(scientific = "   "))
        plants.plants.value = listOf(plant())

        val outcome = useCase("p1")

        assertEquals(ReenrichOutcome.SkippedBlank, outcome)
        assertTrue(species.lookups.isEmpty())
    }

    @Test
    fun `lookup returning the same id does not trigger a redundant upsert`() = runTest {
        val (useCase, plants, species) = build()
        val stub = stubSpecies(id = "sp-local-monstera")
        plants.species.value = listOf(stub)
        plants.plants.value = listOf(plant(speciesId = stub.id))
        // Simulate Perenual mapping back to the exact same id (defensive guard)
        species.result = SpeciesLookupResult.Fresh(stub.copy(provider = Species.PROVIDER_PERENUAL))

        val outcome = useCase("p1")

        assertEquals(ReenrichOutcome.AlreadyEnriched, outcome)
        assertTrue(plants.upsertedPlants.isEmpty())
    }
}
