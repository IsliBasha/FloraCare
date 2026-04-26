package com.floracare.app.data.remote.weather

import com.floracare.app.data.remote.CurrentWeatherResponse
import com.floracare.app.data.remote.WeatherMainDto
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherMapperTest {

    private val fetchedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    private fun response(
        dt: Long = 1_700_000_000L,
        temp: Double = 22.5,
        humidity: Int = 60,
        rain: Map<String, Double> = emptyMap(),
    ) = CurrentWeatherResponse(
        dt = dt,
        main = WeatherMainDto(temp = temp, humidity = humidity),
        rain = rain,
    )

    @Test
    fun `maps current snapshot fields into WeatherSnapshot`() {
        val out = WeatherMapper.toSnapshot(
            response = response(),
            lat = 41.327,
            lon = 19.819,
            fetchedAt = fetchedAt,
        )

        assertEquals(41.327, out.lat, 0.0001)
        assertEquals(19.819, out.lon, 0.0001)
        assertEquals(22.5f, out.tempC, 0.001f)
        assertEquals(60f, out.humidityPct, 0.001f)
        assertEquals(Instant.fromEpochSeconds(1_700_000_000L), out.recordedAt)
    }

    @Test
    fun `derives stable id from coordinates and dt`() {
        val out = WeatherMapper.toSnapshot(
            response = response(dt = 1_700_000_000L),
            lat = 41.327,
            lon = 19.819,
            fetchedAt = fetchedAt,
        )
        assertEquals("wx-41.327-19.819-1700000000", out.id)
    }

    @Test
    fun `picks rain mm from the 1h key when present`() {
        val out = WeatherMapper.toSnapshot(
            response = response(rain = mapOf("1h" to 2.3)),
            lat = 0.0, lon = 0.0, fetchedAt = fetchedAt,
        )
        assertEquals(2.3f, out.rainMm, 0.001f)
    }

    @Test
    fun `falls back to the 3h rain key divided by 3 when 1h missing`() {
        val out = WeatherMapper.toSnapshot(
            response = response(rain = mapOf("3h" to 6.0)),
            lat = 0.0, lon = 0.0, fetchedAt = fetchedAt,
        )
        assertEquals(2.0f, out.rainMm, 0.001f)
    }

    @Test
    fun `defaults rainMm to zero when rain map is empty`() {
        val out = WeatherMapper.toSnapshot(
            response = response(rain = emptyMap()),
            lat = 0.0, lon = 0.0, fetchedAt = fetchedAt,
        )
        assertEquals(0f, out.rainMm, 0.001f)
    }

    @Test
    fun `uvIndex is zero — not provided by the v25 weather endpoint`() {
        val out = WeatherMapper.toSnapshot(
            response = response(),
            lat = 0.0, lon = 0.0, fetchedAt = fetchedAt,
        )
        assertEquals(0f, out.uvIndex, 0.001f)
    }
}
