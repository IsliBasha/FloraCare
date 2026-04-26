package com.floracare.app.data.remote

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OpenWeather v2.5 free-tier weather endpoints. We deliberately avoid OneCall
 * 3.0 because it requires a separate "One Call by Call" subscription with a
 * payment method on file even though usage up to 1k/day is free. The /data/2.5
 * endpoints work with any free API key, no subscription, no card.
 *
 * Tradeoff: no UV index, no native daily aggregate. The care engine only
 * consumes recent snapshots (`recordedAt` within the last 3 days), so a
 * single current-weather call per scheduler run is sufficient.
 */
interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun current(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") key: String,
    ): CurrentWeatherResponse
}

data class CurrentWeatherResponse(
    @Json(name = "dt") val dt: Long,
    @Json(name = "main") val main: WeatherMainDto,
    @Json(name = "rain") val rain: Map<String, Double> = emptyMap(),
)

data class WeatherMainDto(
    @Json(name = "temp") val temp: Double,
    @Json(name = "humidity") val humidity: Int,
)
