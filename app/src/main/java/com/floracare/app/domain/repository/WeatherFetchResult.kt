package com.floracare.app.domain.repository

import com.floracare.app.domain.model.WeatherSnapshot

/**
 * Outcome of a [WeatherRepository.refresh] call. Mirrors the shape used by
 * `SpeciesLookupResult` so callers in the data + worker layers can reason
 * about freshness uniformly.
 */
sealed interface WeatherFetchResult {
    data class Fresh(val snapshot: WeatherSnapshot) : WeatherFetchResult
    data class Stale(val snapshot: WeatherSnapshot, val reason: WeatherStaleReason) : WeatherFetchResult
    data class Offline(val snapshot: WeatherSnapshot?) : WeatherFetchResult
}

enum class WeatherStaleReason {
    REMOTE_UNAVAILABLE,
    RATE_LIMITED,
}
