package com.floracare.app.domain.model

import kotlinx.datetime.Instant

data class WeatherSnapshot(
    val id: String,
    val lat: Double,
    val lon: Double,
    val recordedAt: Instant,
    val tempC: Float,
    val humidityPct: Float,
    val rainMm: Float,
    val uvIndex: Float,
)
