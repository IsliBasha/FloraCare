package com.floracare.app.data.remote.weather

import com.floracare.app.data.remote.CurrentDto
import com.floracare.app.data.remote.DailyDto
import com.floracare.app.data.remote.HourlyDto
import com.floracare.app.data.remote.OneCallResponse
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherMapperTest {

    private val fetchedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    private fun response(
        currentDt: Long = 1_700_000_000L,
        temp: Double = 22.5,
        humidity: Int = 60,
        uvi: Double = 4.5,
        hourly: List<HourlyDto> = emptyList(),
        daily: List<DailyDto> = emptyList(),
    ) = OneCallResponse(
        current = CurrentDto(
            dt = currentDt,
            temp = temp,
            humidity = humidity,
            uvi = uvi,
        ),
        hourly = hourly,
        daily = daily,
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
        assertEquals(4.5f, out.uvIndex, 0.001f)
        assertEquals(Instant.fromEpochSeconds(1_700_000_000L), out.recordedAt)
    }

    @Test
    fun `derives stable id from coordinates and current dt`() {
        val out = WeatherMapper.toSnapshot(
            response = response(currentDt = 1_700_000_000L),
            lat = 41.327,
            lon = 19.819,
            fetchedAt = fetchedAt,
        )
        assertEquals("wx-41.327-19.819-1700000000", out.id)
    }

    @Test
    fun `picks rain mm from first hourly entry when present`() {
        val out = WeatherMapper.toSnapshot(
            response = response(
                hourly = listOf(
                    HourlyDto(
                        dt = 1_700_000_000L,
                        temp = 22.0,
                        humidity = 60,
                        rain = mapOf("1h" to 2.3),
                    ),
                ),
            ),
            lat = 0.0, lon = 0.0, fetchedAt = fetchedAt,
        )
        assertEquals(2.3f, out.rainMm, 0.001f)
    }

    @Test
    fun `defaults rainMm to zero when hourly rain map is empty`() {
        val out = WeatherMapper.toSnapshot(
            response = response(
                hourly = listOf(
                    HourlyDto(dt = 1L, temp = 20.0, humidity = 50, rain = emptyMap()),
                ),
            ),
            lat = 0.0, lon = 0.0, fetchedAt = fetchedAt,
        )
        assertEquals(0f, out.rainMm, 0.001f)
    }

    @Test
    fun `defaults rainMm to zero when hourly is empty`() {
        val out = WeatherMapper.toSnapshot(
            response = response(hourly = emptyList()),
            lat = 0.0, lon = 0.0, fetchedAt = fetchedAt,
        )
        assertEquals(0f, out.rainMm, 0.001f)
    }
}
