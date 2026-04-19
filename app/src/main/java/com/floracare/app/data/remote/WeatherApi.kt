package com.floracare.app.data.remote

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("data/3.0/onecall")
    suspend fun oneCall(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("exclude") exclude: String = "minutely,alerts",
        @Query("units") units: String = "metric",
        @Query("appid") key: String,
    ): OneCallResponse
}

data class OneCallResponse(
    @Json(name = "current") val current: CurrentDto,
    @Json(name = "hourly") val hourly: List<HourlyDto> = emptyList(),
    @Json(name = "daily") val daily: List<DailyDto> = emptyList(),
)

data class CurrentDto(
    @Json(name = "dt") val dt: Long,
    @Json(name = "temp") val temp: Double,
    @Json(name = "humidity") val humidity: Int,
    @Json(name = "uvi") val uvi: Double = 0.0,
)

data class HourlyDto(
    @Json(name = "dt") val dt: Long,
    @Json(name = "temp") val temp: Double,
    @Json(name = "humidity") val humidity: Int,
    @Json(name = "rain") val rain: Map<String, Double> = emptyMap(),
)

data class DailyDto(
    @Json(name = "dt") val dt: Long,
    @Json(name = "temp") val temp: Map<String, Double>,
    @Json(name = "humidity") val humidity: Int,
    @Json(name = "rain") val rain: Double = 0.0,
    @Json(name = "uvi") val uvi: Double = 0.0,
)
