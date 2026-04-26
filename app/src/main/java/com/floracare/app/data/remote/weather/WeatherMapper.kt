package com.floracare.app.data.remote.weather

import com.floracare.app.data.remote.CurrentWeatherResponse
import com.floracare.app.domain.model.WeatherSnapshot
import kotlinx.datetime.Instant

object WeatherMapper {

    fun toSnapshot(
        response: CurrentWeatherResponse,
        lat: Double,
        lon: Double,
        fetchedAt: Instant,
    ): WeatherSnapshot {
        val rainMm = response.rain[RAIN_HOUR_KEY]?.toFloat()
            ?: response.rain[RAIN_3H_KEY]?.div(3.0)?.toFloat()
            ?: 0f
        return WeatherSnapshot(
            id = "wx-$lat-$lon-${response.dt}",
            lat = lat,
            lon = lon,
            recordedAt = Instant.fromEpochSeconds(response.dt),
            tempC = response.main.temp.toFloat(),
            humidityPct = response.main.humidity.toFloat(),
            rainMm = rainMm,
            uvIndex = 0f,
        )
    }

    private const val RAIN_HOUR_KEY = "1h"
    private const val RAIN_3H_KEY = "3h"
}
