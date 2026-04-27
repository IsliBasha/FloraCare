package com.floracare.app.test

import com.floracare.app.domain.model.WeatherSnapshot
import com.floracare.app.domain.repository.WeatherFetchResult
import com.floracare.app.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/**
 * In-memory fake of [WeatherRepository] for VM tests. Tests mutate
 * [snapshots] to drive the observed flow.
 */
class FakeWeatherRepository : WeatherRepository {
    val snapshots: MutableStateFlow<List<WeatherSnapshot>> = MutableStateFlow(emptyList())

    var refreshResult: WeatherFetchResult = WeatherFetchResult.Offline(null)
    var refreshCalls = 0
    var lastRefreshLat: Double? = null
    var lastRefreshLon: Double? = null

    override suspend fun recentWeather(since: Instant): List<WeatherSnapshot> =
        snapshots.value.filter { it.recordedAt >= since }.sortedByDescending { it.recordedAt }

    override fun observeRecent(since: Instant): Flow<List<WeatherSnapshot>> =
        snapshots.map { list ->
            list.filter { it.recordedAt >= since }.sortedByDescending { it.recordedAt }
        }

    override suspend fun cache(snapshot: WeatherSnapshot) {
        snapshots.value = snapshots.value.filterNot { it.id == snapshot.id } + snapshot
    }

    override suspend fun refresh(lat: Double, lon: Double): WeatherFetchResult {
        refreshCalls += 1
        lastRefreshLat = lat
        lastRefreshLon = lon
        return refreshResult
    }
}
