package com.floracare.app.domain.usecase

import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.repository.SpeciesLookupResult
import com.floracare.app.domain.repository.SpeciesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SpeciesLookupUseCaseTest {

    private class RecordingRepo(
        private val result: SpeciesLookupResult = SpeciesLookupResult.NotFound,
    ) : SpeciesRepository {
        val calls = mutableListOf<Pair<String, String?>>()
        override suspend fun lookup(scientificName: String, commonNameHint: String?): SpeciesLookupResult {
            calls += scientificName to commonNameHint
            return result
        }
    }

    private val species = Species(
        id = "sp-perenual-1",
        scientificName = "Monstera deliciosa",
        commonName = "Monstera",
        waterFrequencyDays = 7,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.HIGH,
        temperatureRangeC = 15f..28f,
        toxicity = Toxicity.TOXIC,
        careNotes = "",
        provider = Species.PROVIDER_PERENUAL,
        providerSpeciesId = "1",
    )

    @Test
    fun `blank scientific name throws IllegalArgumentException`() = runTest {
        val useCase = SpeciesLookupUseCase(RecordingRepo())
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase("   ") }
        }
    }

    @Test
    fun `delegates normalised arguments to the repository`() = runTest {
        val repo = RecordingRepo(SpeciesLookupResult.Fresh(species))
        val useCase = SpeciesLookupUseCase(repo)

        val result = useCase("  Monstera deliciosa  ", commonNameHint = "  Swiss cheese plant  ")

        assertEquals(listOf("Monstera deliciosa" to "Swiss cheese plant"), repo.calls)
        assertEquals(SpeciesLookupResult.Fresh(species), result)
    }

    @Test
    fun `blank common-name hint is coerced to null`() = runTest {
        val repo = RecordingRepo(SpeciesLookupResult.NotFound)
        val useCase = SpeciesLookupUseCase(repo)

        useCase("Monstera deliciosa", commonNameHint = "   ")

        assertEquals(listOf("Monstera deliciosa" to null), repo.calls)
    }
}
