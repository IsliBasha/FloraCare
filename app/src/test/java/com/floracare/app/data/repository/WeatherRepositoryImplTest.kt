package com.floracare.app.data.repository

import com.floracare.app.data.local.toEntity
import com.floracare.app.data.remote.CurrentWeatherResponse
import com.floracare.app.data.remote.WeatherMainDto
import com.floracare.app.data.remote.perenual.RemoteResult
import com.floracare.app.domain.model.WeatherSnapshot
import com.floracare.app.domain.repository.WeatherFetchResult
import com.floracare.app.domain.repository.WeatherStaleReason
import com.floracare.app.test.FakeWeatherDao
import com.floracare.app.test.FakeWeatherRemoteDataSource
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.minutes

class WeatherRepositoryImplTest {

    private val now = Instant.fromEpochMilliseconds(1_700_000_000_000L)
    private val fixedClock = object : Clock { override fun now(): Instant = now }

    private fun snapshot(
        id: String = "wx-old",
        recordedAt: Instant,
        lat: Double = 41.327,
        lon: Double = 19.819,
        tempC: Float = 18f,
    ) = WeatherSnapshot(
        id = id,
        lat = lat,
        lon = lon,
        recordedAt = recordedAt,
        tempC = tempC,
        humidityPct = 55f,
        rainMm = 0f,
        uvIndex = 1f,
    )

    private fun successResponse(currentDt: Long = 1_700_000_000L) = RemoteResult.Success(
        CurrentWeatherResponse(
            dt = currentDt,
            main = WeatherMainDto(temp = 22.5, humidity = 60),
            rain = emptyMap(),
        ),
    )

    private fun build(
        dao: FakeWeatherDao = FakeWeatherDao(),
        remote: FakeWeatherRemoteDataSource = FakeWeatherRemoteDataSource(),
    ): Triple<WeatherRepositoryImpl, FakeWeatherDao, FakeWeatherRemoteDataSource> {
        val repo = WeatherRepositoryImpl(dao, remote, fixedClock)
        return Triple(repo, dao, remote)
    }

    @Test
    fun `fresh cache short-circuits the network`() = runTest {
        val (repo, dao, remote) = build()
        val cached = snapshot(id = "wx-fresh", recordedAt = now - 5.minutes)
        dao.insert(cached.toEntity())

        val result = repo.refresh(lat = 41.327, lon = 19.819)

        assertTrue("expected Fresh, got $result", result is WeatherFetchResult.Fresh)
        assertEquals(0, remote.fetchCount)
        assertEquals("wx-fresh", (result as WeatherFetchResult.Fresh).snapshot.id)
    }

    @Test
    fun `stale cache triggers a remote refresh and persists the new snapshot`() = runTest {
        val (repo, dao, remote) = build()
        val cached = snapshot(id = "wx-stale", recordedAt = now - 60.minutes)
        dao.insert(cached.toEntity())
        remote.nextResult = successResponse(currentDt = 1_700_000_000L)

        val result = repo.refresh(lat = 41.327, lon = 19.819)

        assertTrue(result is WeatherFetchResult.Fresh)
        assertEquals(1, remote.fetchCount)
        val stored = dao.all().first()
        assertEquals(22.5f, stored.tempC, 0.001f)
        assertEquals("wx-41.327-19.819-1700000000", stored.id)
    }

    @Test
    fun `network failure with stale cache returns Stale carrying the cached snapshot`() = runTest {
        val (repo, dao, remote) = build()
        val cached = snapshot(id = "wx-stale", recordedAt = now - 60.minutes)
        dao.insert(cached.toEntity())
        remote.nextResult = RemoteResult.Network(IOException("offline"))

        val result = repo.refresh(lat = 41.327, lon = 19.819)

        assertTrue("expected Stale, got $result", result is WeatherFetchResult.Stale)
        result as WeatherFetchResult.Stale
        assertEquals("wx-stale", result.snapshot.id)
        assertEquals(WeatherStaleReason.REMOTE_UNAVAILABLE, result.reason)
    }

    @Test
    fun `rate-limited response with stale cache surfaces a RATE_LIMITED reason`() = runTest {
        val (repo, dao, remote) = build()
        val cached = snapshot(id = "wx-stale", recordedAt = now - 60.minutes)
        dao.insert(cached.toEntity())
        remote.nextResult = RemoteResult.RateLimited

        val result = repo.refresh(lat = 41.327, lon = 19.819)

        assertTrue(result is WeatherFetchResult.Stale)
        assertEquals(WeatherStaleReason.RATE_LIMITED, (result as WeatherFetchResult.Stale).reason)
    }

    @Test
    fun `network failure without any cache returns Offline with null snapshot`() = runTest {
        val (repo, _, remote) = build()
        remote.nextResult = RemoteResult.Network(IOException("offline"))

        val result = repo.refresh(lat = 41.327, lon = 19.819)

        assertTrue("expected Offline, got $result", result is WeatherFetchResult.Offline)
        assertNull((result as WeatherFetchResult.Offline).snapshot)
    }

    @Test
    fun `recentWeather still reads from the local cache`() = runTest {
        val (repo, dao, _) = build()
        val cached = snapshot(id = "wx-old", recordedAt = now - 90.minutes)
        dao.insert(cached.toEntity())

        val list = repo.recentWeather(since = now - 1.minutes * 1)
        // since= now-1min: cached at now-90min is older, so excluded
        assertTrue(list.isEmpty())

        val all = repo.recentWeather(since = now - 120.minutes)
        assertEquals(1, all.size)
    }

    @Test
    fun `cache writes through the dao for callers who supply their own snapshot`() = runTest {
        val (repo, dao, _) = build()
        val direct = snapshot(id = "wx-direct", recordedAt = now)

        repo.cache(direct)

        assertNotNull(dao.all().firstOrNull { it.id == "wx-direct" })
    }
}
