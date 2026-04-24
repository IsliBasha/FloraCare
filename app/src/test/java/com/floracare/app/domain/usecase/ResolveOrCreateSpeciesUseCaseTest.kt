package com.floracare.app.domain.usecase

import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.repository.SpeciesLookupResult
import com.floracare.app.domain.repository.StaleReason
import com.floracare.app.test.FakePlantRepository
import com.floracare.app.test.FakeSpeciesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveOrCreateSpeciesUseCaseTest {

    private fun species(id: String, scientific: String, common: String = scientific) = Species(
        id = id,
        scientificName = scientific,
        commonName = common,
        waterFrequencyDays = 7,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.MEDIUM,
        temperatureRangeC = 18f..28f,
        toxicity = Toxicity.MILD,
        careNotes = "",
    )

    private fun buildUseCase(
        repo: FakePlantRepository = FakePlantRepository(),
        speciesRepo: FakeSpeciesRepository = FakeSpeciesRepository(),
        idGen: () -> String = { error("idGenerator should not be called") },
    ): Triple<ResolveOrCreateSpeciesUseCase, FakePlantRepository, FakeSpeciesRepository> {
        val lookup = SpeciesLookupUseCase(speciesRepo)
        val useCase = ResolveOrCreateSpeciesUseCase(repo, lookup).apply { idGenerator = idGen }
        return Triple(useCase, repo, speciesRepo)
    }

    @Test
    fun `exact scientific name match returns existing species id`() = runTest {
        val (useCase, repo, species) = buildUseCase(
            repo = FakePlantRepository().apply {
                species.value = listOf(species("sp-1", "Monstera deliciosa"))
            },
        )

        val id = useCase("Monstera deliciosa", topConfidence = 0.9f)

        assertEquals("sp-1", id)
        assertEquals(
            "no new species should have been written",
            0,
            repo.upsertedSpecies.size,
        )
        assertTrue("cache hit must short-circuit the remote lookup", species.lookups.isEmpty())
    }

    @Test
    fun `case-insensitive match returns existing id`() = runTest {
        val (useCase, _, _) = buildUseCase(
            repo = FakePlantRepository().apply {
                species.value = listOf(species("sp-1", "Monstera deliciosa"))
            },
        )

        val id = useCase("monstera DELICIOSA", topConfidence = 0.9f)

        assertEquals("sp-1", id)
    }

    @Test
    fun `whitespace-insensitive match returns existing id`() = runTest {
        val (useCase, _, _) = buildUseCase(
            repo = FakePlantRepository().apply {
                species.value = listOf(species("sp-1", "Monstera deliciosa"))
            },
        )

        val id = useCase("   Monstera deliciosa   ", topConfidence = 0.9f)

        assertEquals("sp-1", id)
    }

    @Test
    fun `high confidence miss synthesises a new species without calling Perenual`() = runTest {
        val (useCase, repo, lookup) = buildUseCase(idGen = { "sp-generated" })

        val id = useCase("Sansevieria trifasciata", topConfidence = 0.9f)

        assertEquals("sp-generated", id)
        assertEquals(1, repo.upsertedSpecies.size)
        val written = repo.upsertedSpecies.single()
        assertEquals("sp-generated", written.id)
        assertEquals("Sansevieria trifasciata", written.scientificName)
        assertEquals("Sansevieria trifasciata", written.commonName)
        assertTrue(
            "high confidence keeps the remote path untouched",
            lookup.lookups.isEmpty(),
        )
    }

    @Test
    fun `low confidence + Fresh remote hit returns the Perenual id`() = runTest {
        val remote = species("sp-perenual-42", "Monstera deliciosa")
        val (useCase, repo, lookup) = buildUseCase(
            speciesRepo = FakeSpeciesRepository(SpeciesLookupResult.Fresh(remote)),
            idGen = { error("synth fallback should not be reached") },
        )

        val id = useCase("Monstera deliciosa", topConfidence = 0.1f)

        assertEquals("sp-perenual-42", id)
        assertEquals(listOf("Monstera deliciosa" to null), lookup.lookups)
        assertTrue(
            "no synth row should have been written when Perenual served the result",
            repo.upsertedSpecies.isEmpty(),
        )
    }

    @Test
    fun `low confidence + Stale remote hit returns the cached id`() = runTest {
        val cached = species("sp-perenual-7", "Ficus lyrata")
        val (useCase, _, lookup) = buildUseCase(
            speciesRepo = FakeSpeciesRepository(
                SpeciesLookupResult.Stale(cached, StaleReason.RATE_LIMITED),
            ),
            idGen = { error("synth fallback should not be reached") },
        )

        val id = useCase("Ficus lyrata", topConfidence = 0.1f)

        assertEquals("sp-perenual-7", id)
        assertEquals(1, lookup.lookups.size)
    }

    @Test
    fun `low confidence + NotFound falls through to synth`() = runTest {
        val (useCase, repo, lookup) = buildUseCase(
            speciesRepo = FakeSpeciesRepository(SpeciesLookupResult.NotFound),
            idGen = { "sp-synth" },
        )

        val id = useCase("Madeup plantus", topConfidence = 0.1f)

        assertEquals("sp-synth", id)
        assertEquals(1, lookup.lookups.size)
        assertEquals(1, repo.upsertedSpecies.size)
    }

    @Test
    fun `low confidence + Offline falls through to synth`() = runTest {
        val (useCase, repo, _) = buildUseCase(
            speciesRepo = FakeSpeciesRepository(SpeciesLookupResult.Offline(cached = null)),
            idGen = { "sp-synth" },
        )

        val id = useCase("Sansevieria trifasciata", topConfidence = 0.05f)

        assertEquals("sp-synth", id)
        assertEquals(1, repo.upsertedSpecies.size)
    }

    @Test
    fun `second call with same label reuses the previously written row`() = runTest {
        var counter = 0
        val (useCase, repo, _) = buildUseCase(idGen = { "sp-gen-${counter++}" })

        val first = useCase("Zamioculcas zamiifolia", topConfidence = 0.9f)
        val second = useCase("Zamioculcas zamiifolia", topConfidence = 0.9f)

        assertEquals(first, second)
        assertEquals(
            "second call must not insert a duplicate species",
            1,
            repo.upsertedSpecies.size,
        )
        assertNotNull(repo.findSpeciesByScientificName("Zamioculcas zamiifolia"))
    }

    @Test
    fun `blank label throws`() = runTest {
        val (useCase, _, _) = buildUseCase(idGen = { "sp-x" })

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase("   ", topConfidence = 0.9f) }
        }
    }
}
