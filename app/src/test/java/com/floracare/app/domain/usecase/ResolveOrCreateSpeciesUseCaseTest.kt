package com.floracare.app.domain.usecase

import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.test.FakePlantRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
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
        idGen: () -> String = { error("idGenerator should not be called") },
    ): Pair<ResolveOrCreateSpeciesUseCase, FakePlantRepository> =
        ResolveOrCreateSpeciesUseCase(repo).apply { idGenerator = idGen } to repo

    @Test
    fun `exact scientific name match returns existing species id`() = runTest {
        val (useCase, repo) = buildUseCase(
            repo = FakePlantRepository().apply {
                species.value = listOf(species("sp-1", "Monstera deliciosa"))
            },
        )

        val id = useCase("Monstera deliciosa")

        assertEquals("sp-1", id)
        assertEquals(
            "no new species should have been written",
            0,
            repo.upsertedSpecies.size,
        )
    }

    @Test
    fun `case-insensitive match returns existing id`() = runTest {
        val (useCase, _) = buildUseCase(
            repo = FakePlantRepository().apply {
                species.value = listOf(species("sp-1", "Monstera deliciosa"))
            },
        )

        val id = useCase("monstera DELICIOSA")

        assertEquals("sp-1", id)
    }

    @Test
    fun `whitespace-insensitive match returns existing id`() = runTest {
        val (useCase, _) = buildUseCase(
            repo = FakePlantRepository().apply {
                species.value = listOf(species("sp-1", "Monstera deliciosa"))
            },
        )

        val id = useCase("   Monstera deliciosa   ")

        assertEquals("sp-1", id)
    }

    @Test
    fun `no match synthesises a new species with the predicted name`() = runTest {
        val (useCase, repo) = buildUseCase(idGen = { "sp-generated" })

        val id = useCase("Sansevieria trifasciata")

        assertEquals("sp-generated", id)
        assertEquals(1, repo.upsertedSpecies.size)
        val written = repo.upsertedSpecies.single()
        assertEquals("sp-generated", written.id)
        assertEquals("Sansevieria trifasciata", written.scientificName)
        assertEquals("Sansevieria trifasciata", written.commonName)
    }

    @Test
    fun `second call with same label finds the synthesised row and does not duplicate`() = runTest {
        var counter = 0
        val (useCase, repo) = buildUseCase(idGen = { "sp-gen-${counter++}" })

        val first = useCase("Zamioculcas zamiifolia")
        val second = useCase("Zamioculcas zamiifolia")

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
        val (useCase, _) = buildUseCase(idGen = { "sp-x" })

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase("   ") }
        }
    }
}
