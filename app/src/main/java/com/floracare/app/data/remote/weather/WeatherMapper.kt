package com.floracare.app.data.remote.weather

import com.floracare.app.data.remote.OneCallResponse
import com.floracare.app.domain.model.WeatherSnapshot
import kotlinx.datetime.Instant

object WeatherMapper {

    fun toSnapshot(
        response: OneCallResponse,
        lat: Double,
        lon: Double,
        fetchedAt: Instant,
    ): WeatherSnapshot {
        val current = response.current
        val rainMm = response.hourly.firstOrNull()
            ?.rain
            ?.get(RAIN_HOUR_KEY)
            ?.toFloat()
            ?: 0f
        return WeatherSnapshot(
            id = "wx-$lat-$lon-${current.dt}",
            lat = lat,
            lon = lon,
            recordedAt = Instant.fromEpochSeconds(current.dt),
            tempC = current.temp.toFloat(),
            humidityPct = current.humidity.toFloat(),
            rainMm = rainMm,
            uvIndex = current.uvi.toFloat(),
        )
    }

    private const val RAIN_HOUR_KEY = "1h"
}
