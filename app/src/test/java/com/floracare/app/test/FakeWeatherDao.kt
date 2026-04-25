package com.floracare.app.test

import com.floracare.app.data.local.WeatherDao
import com.floracare.app.data.local.WeatherSnapshotEntity
import kotlinx.datetime.Instant

/**
 * In-memory [WeatherDao] for JVM tests. Stores rows by id with last-write-wins.
 */
class FakeWeatherDao : WeatherDao {
    private val rows = mutableMapOf<String, WeatherSnapshotEntity>()

    override suspend fun findRecent(since: Instant): List<WeatherSnapshotEntity> =
        rows.values
            .filter { it.recordedAt >= since }
            .sortedByDescending { it.recordedAt }

    override suspend fun insert(snapshot: WeatherSnapshotEntity) {
        rows[snapshot.id] = snapshot
    }

    fun all(): List<WeatherSnapshotEntity> = rows.values.sortedByDescending { it.recordedAt }
}
