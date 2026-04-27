package com.floracare.app.data.repository

import com.floracare.app.data.local.WeatherDao
import com.floracare.app.data.local.toDomain
import com.floracare.app.data.local.toEntity
import com.floracare.app.data.remote.perenual.RemoteResult
import com.floracare.app.data.remote.weather.WeatherMapper
import com.floracare.app.data.remote.weather.WeatherRemoteDataSource
import com.floracare.app.domain.model.WeatherSnapshot
import com.floracare.app.domain.repository.WeatherFetchResult
import com.floracare.app.domain.repository.WeatherRepository
import com.floracare.app.domain.repository.WeatherStaleReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * Stale-while-revalidate weather repository over OpenWeather + Room.
 *
 * Cache freshness is keyed by `recordedAt` only — coordinates aren't part
 * of the freshness window because the daily scheduler always refreshes
 * with the device's current location, so a snapshot from any location is
 * either current or doesn't matter (engine filters by `recordedAt` anyway).
 */
@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherDao: WeatherDao,
    private val remote: WeatherRemoteDataSource,
    private val clock: Clock,
) : WeatherRepository {

    override suspend fun recentWeather(since: Instant): List<WeatherSnapshot> =
        weatherDao.findRecent(since).map { it.toDomain() }

    override fun observeRecent(since: Instant): Flow<List<WeatherSnapshot>> =
        weatherDao.observeRecent(since).map { rows -> rows.map { it.toDomain() } }

    override suspend fun cache(snapshot: WeatherSnapshot) =
        weatherDao.insert(snapshot.toEntity())

    override suspend fun refresh(lat: Double, lon: Double): WeatherFetchResult {
        val now = clock.now()
        val latestCached = weatherDao.findRecent(EARLIEST_USABLE)
            .firstOrNull()
            ?.toDomain()

        if (latestCached != null && now - latestCached.recordedAt < FRESH_TTL) {
            return WeatherFetchResult.Fresh(latestCached)
        }

        return when (val response = remote.fetch(lat, lon)) {
            is RemoteResult.Success -> {
                val mapped = WeatherMapper.toSnapshot(
                    response = response.value,
                    lat = lat,
                    lon = lon,
                    fetchedAt = now,
                )
                weatherDao.insert(mapped.toEntity())
                WeatherFetchResult.Fresh(mapped)
            }
            RemoteResult.RateLimited -> staleOr(latestCached, WeatherStaleReason.RATE_LIMITED)
            RemoteResult.Empty,
            is RemoteResult.Network,
            is RemoteResult.Http -> staleOr(latestCached, WeatherStaleReason.REMOTE_UNAVAILABLE)
        }
    }

    private fun staleOr(cached: WeatherSnapshot?, reason: WeatherStaleReason): WeatherFetchResult =
        if (cached != null) WeatherFetchResult.Stale(cached, reason)
        else WeatherFetchResult.Offline(null)

    private companion object {
        val FRESH_TTL = 30.minutes

        // Far-past sentinel — we want the latest row regardless of age for SWR fallback.
        val EARLIEST_USABLE: Instant = Instant.fromEpochMilliseconds(0L)
    }
}
