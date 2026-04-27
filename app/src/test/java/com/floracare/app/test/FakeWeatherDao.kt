package com.floracare.app.test

import com.floracare.app.data.local.WeatherDao
import com.floracare.app.data.local.WeatherSnapshotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/**
 * In-memory [WeatherDao] for JVM tests. Stores rows by id with last-write-wins
 * and exposes a flow that recomposes whenever rows change.
 */
class FakeWeatherDao : WeatherDao {
    private val rows = MutableStateFlow<Map<String, WeatherSnapshotEntity>>(emptyMap())

    override suspend fun findRecent(since: Instant): List<WeatherSnapshotEntity> =
        rows.value.values
            .filter { it.recordedAt >= since }
            .sortedByDescending { it.recordedAt }

    override fun observeRecent(since: Instant): Flow<List<WeatherSnapshotEntity>> =
        rows.map { current ->
            current.values
                .filter { it.recordedAt >= since }
                .sortedByDescending { it.recordedAt }
        }

    override suspend fun insert(snapshot: WeatherSnapshotEntity) {
        rows.value = rows.value + (snapshot.id to snapshot)
    }

    fun all(): List<WeatherSnapshotEntity> =
        rows.value.values.sortedByDescending { it.recordedAt }
}
