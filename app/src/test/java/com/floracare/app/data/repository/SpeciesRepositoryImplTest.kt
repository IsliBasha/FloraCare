package com.floracare.app.data.repository

import com.floracare.app.data.remote.perenual.Hardiness
import com.floracare.app.data.remote.perenual.PerenualImage
import com.floracare.app.data.remote.perenual.RemoteResult
import com.floracare.app.data.remote.perenual.SpeciesDetailsResponse
import com.floracare.app.data.remote.perenual.SpeciesSearchItem
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.repository.SpeciesLookupResult
import com.floracare.app.domain.repository.StaleReason
import com.floracare.app.test.FakePerenualRemoteDataSource
import com.floracare.app.test.FakeSpeciesDao
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SpeciesRepositoryImplTest {

    private val now = Instant.fromEpochMilliseconds(1_700_000_000_000L)
    private val fixedClock = object : Clock { override fun now(): Instant = now }

    private fun species(
        id: String,
        scientific: String,
        provider: String = Species.PROVIDER_LOCAL,
        fetchedAt: Instant? = null,
    ) = Species(
        id = id,
        scientificName = scientific,
        commonName = scientific,
        waterFrequencyDays = 7,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.MEDIUM,
        temperatureRangeC = 15f..28f,
        toxicity = Toxicity.NONE,
        careNotes = "",
        provider = provider,
        providerSpeciesId = if (provider == Species.PROVIDER_PERENUAL) "42" else null,
        fetchedAt = fetchedAt,
    )

    private fun details(id: Long = 42L) = SpeciesDetailsResponse(
        id = id,
        commonName = "Monstera",
        scientificName = listOf("Monstera deliciosa"),
        family = "Araceae",
        genus = "Monstera",
        watering = "Average",
        sunlight = listOf("part_shade"),
        hardiness = Hardiness("10", "12"),
        poisonousToHumans = 0,
        poisonousToPets = 1,
        careLevel = "Easy",
        description = "Tropical vine",
        image = PerenualImage(thumbnail = null, mediumUrl = "https://example.test/m.jpg"),
    )

    private fun build(
        dao: FakeSpeciesDao = FakeSpeciesDao(),
        remote: FakePerenualRemoteDataSource = FakePerenualRemoteDataSource(),
    ): Triple<SpeciesRepositoryImpl, FakeSpeciesDao, FakePerenualRemoteDataSource> {
        val repo = SpeciesRepositoryImpl(dao, remote, fixedClock)
        return Triple(repo, dao, remote)
    }

    @Test
    fun `fresh Perenual cache short-circuits the network`() = runTest {
        val (repo, dao, remote) = build()
        val cached = species(
            id = "sp-perenual-42",
            scientific = "Monstera deliciosa",
            provider = Species.PROVIDER_PERENUAL,
            fetchedAt = now,
        )
        dao.upsert(cached.let { s ->
            com.floracare.app.data.local.SpeciesEntity(
                id = s.id,
                scientificName = s.scientificName,
                commonName = s.commonName,
                waterFrequencyDays = s.waterFrequencyDays,
                lightNeed = s.lightNeed,
                humidityNeed = s.humidityNeed,
                tempMinC = s.temperatureRangeC.start,
                tempMaxC = s.temperatureRangeC.endInclusive,
                toxicity = s.toxicity,
                careNotes = s.careNotes,
                provider = s.provider,
                providerSpeciesId = s.providerSpeciesId,
                fetchedAt = s.fetchedAt,
                family = null, genus = null, imageUrl = null,
            )
        })

        val result = repo.lookup("Monstera deliciosa")

        assertTrue(result is SpeciesLookupResult.Fresh)
        assertEquals("sp-perenual-42", (result as SpeciesLookupResult.Fresh).species.id)
        assertTrue("no remote call when cache is fresh", remote.searchQueries.isEmpty())
    }

    @Test
    fun `successful remote fetch persists and returns Fresh`() = runTest {
        val (repo, dao, remote) = build()
        remote.searchResult = RemoteResult.Success(
            SpeciesSearchItem(
                id = 42L,
                commonName = "Monstera",
                scientificName = listOf("Monstera deliciosa"),
                watering = "Average",
                sunlight = listOf("part_shade"),
                image = null,
            ),
        )
        remote.detailsResult = RemoteResult.Success(details())

        val result = repo.lookup("Monstera deliciosa")

        assertTrue(result is SpeciesLookupResult.Fresh)
        val species = (result as SpeciesLookupResult.Fresh).species
        assertEquals("sp-perenual-42", species.id)
        assertEquals(HumidityNeed.HIGH, species.humidityNeed)
        assertEquals(listOf("Monstera deliciosa"), remote.searchQueries)
        assertEquals(listOf(42L), remote.detailsIds)
        assertEquals("row persisted to cache", "sp-perenual-42", dao.findById("sp-perenual-42")?.id)
    }

    @Test
    fun `search returns Empty with no cache yields NotFound`() = runTest {
        val (repo, _, remote) = build()
        remote.searchResult = RemoteResult.Empty

        val result = repo.lookup("Madeup plantus")

        assertEquals(SpeciesLookupResult.NotFound, result)
        assertEquals(listOf("Madeup plantus"), remote.searchQueries)
        assertTrue("never reached details without a hit", remote.detailsIds.isEmpty())
    }

    @Test
    fun `rate-limited with cached row returns Stale RATE_LIMITED`() = runTest {
        val (repo, dao, remote) = build()
        val cached = species(id = "sp-local-1", scientific = "Ficus lyrata")
        dao.upsert(cached.toEntityForTest())
        remote.searchResult = RemoteResult.RateLimited

        val result = repo.lookup("Ficus lyrata")

        assertTrue(result is SpeciesLookupResult.Stale)
        val stale = result as SpeciesLookupResult.Stale
        assertEquals(StaleReason.RATE_LIMITED, stale.reason)
        assertEquals("sp-local-1", stale.species.id)
    }

    @Test
    fun `network failure without cache returns Offline(null)`() = runTest {
        val (repo, _, remote) = build()
        remote.searchResult = RemoteResult.Network(IOException("boom"))

        val result = repo.lookup("Unknown plantus")

        assertTrue(result is SpeciesLookupResult.Offline)
        assertNull((result as SpeciesLookupResult.Offline).cached)
    }

    @Test
    fun `network failure with cached row returns Stale REMOTE_UNAVAILABLE`() = runTest {
        val (repo, dao, remote) = build()
        val cached = species(id = "sp-local-2", scientific = "Sansevieria")
        dao.upsert(cached.toEntityForTest())
        remote.searchResult = RemoteResult.Network(IOException("boom"))

        val result = repo.lookup("Sansevieria")

        assertTrue(result is SpeciesLookupResult.Stale)
        assertEquals(StaleReason.REMOTE_UNAVAILABLE, (result as SpeciesLookupResult.Stale).reason)
    }

    @Test
    fun `common name hint drives remote search query over scientific name`() = runTest {
        val (repo, _, remote) = build()
        remote.searchResult = RemoteResult.Empty

        repo.lookup(scientificName = "Monstera deliciosa", commonNameHint = "Swiss cheese plant")

        assertEquals(listOf("Swiss cheese plant"), remote.searchQueries)
    }

    private fun Species.toEntityForTest() = com.floracare.app.data.local.SpeciesEntity(
        id = id,
        scientificName = scientificName,
        commonName = commonName,
        waterFrequencyDays = waterFrequencyDays,
        lightNeed = lightNeed,
        humidityNeed = humidityNeed,
        tempMinC = temperatureRangeC.start,
        tempMaxC = temperatureRangeC.endInclusive,
        toxicity = toxicity,
        careNotes = careNotes,
        provider = provider,
        providerSpeciesId = providerSpeciesId,
        fetchedAt = fetchedAt,
        family = family, genus = genus, imageUrl = imageUrl,
    )
}
