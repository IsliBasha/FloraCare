package com.floracare.app.domain.usecase

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.SoilMoistureNote
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.model.WeatherSnapshot
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class ComputeNextCareTaskUseCaseTest {

    private val useCase = ComputeNextCareTaskUseCase()
    private val now = Instant.parse("2026-04-19T07:00:00Z")

    private fun species(waterFrequencyDays: Int = 7): Species = Species(
        id = "sp-monstera",
        scientificName = "Monstera deliciosa",
        commonName = "Swiss Cheese Plant",
        waterFrequencyDays = waterFrequencyDays,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.MEDIUM,
        temperatureRangeC = 18f..28f,
        toxicity = Toxicity.MILD,
        careNotes = "",
    )

    private fun plant(location: LocationTag = LocationTag.INDOOR): Plant = Plant(
        id = "pl-1",
        nickname = "Mona",
        speciesId = "sp-monstera",
        locationTag = location,
        acquiredAt = now - 30.days,
        coverPhotoUri = null,
        notes = "",
    )

    private fun weather(
        hoursAgo: Int,
        temp: Float = 22f,
        humidity: Float = 50f,
        rain: Float = 0f,
    ): WeatherSnapshot = WeatherSnapshot(
        id = "w-$hoursAgo",
        lat = 0.0,
        lon = 0.0,
        recordedAt = now - hoursAgo.hours,
        tempC = temp,
        humidityPct = humidity,
        rainMm = rain,
        uvIndex = 3f,
    )

    private fun log(daysAgo: Int, moisture: SoilMoistureNote?): CareLog = CareLog(
        id = "l-$daysAgo",
        plantId = "pl-1",
        taskType = CareTaskType.WATER,
        performedAt = now - daysAgo.days,
        soilMoistureNote = moisture,
    )

    // Rule: avg temp last 3 days > 28°C → shorten by 20%.
    // Base 10 days * 0.8 = 8 days.
    @Test
    fun `hot weather shortens interval by 20 percent`() {
        val result = useCase(
            plant = plant(),
            species = species(waterFrequencyDays = 10),
            recentLogs = emptyList(),
            recentWeather = listOf(
                weather(hoursAgo = 4, temp = 30f),
                weather(hoursAgo = 28, temp = 31f),
                weather(hoursAgo = 52, temp = 29f),
            ),
            now = now,
        )

        assertEquals(CareTaskType.WATER, result.type)
        assertEquals(now + 8.days, result.scheduledAt)
    }

    // Rule: rain last 3 days > 10mm AND outdoor → extend by 50%.
    // Base 10 days * 1.5 = 15, clamped to 10 * 2 = 20. Here 15 fits, so 15 days.
    @Test
    fun `heavy rain extends interval by 50 percent for outdoor plants`() {
        val result = useCase(
            plant = plant(location = LocationTag.OUTDOOR),
            species = species(waterFrequencyDays = 10),
            recentLogs = emptyList(),
            recentWeather = listOf(
                weather(hoursAgo = 4, rain = 8f),
                weather(hoursAgo = 28, rain = 5f),
                weather(hoursAgo = 52, rain = 0f),
            ),
            now = now,
        )

        assertEquals(now + 15.days, result.scheduledAt)
    }

    // Rule: humidity avg < 30% → shorten by 15%.
    // Base 10 * 0.85 = 8.5 → rounded to 9 days (half-up).
    @Test
    fun `low humidity shortens interval by 15 percent`() {
        val result = useCase(
            plant = plant(),
            species = species(waterFrequencyDays = 10),
            recentLogs = emptyList(),
            recentWeather = listOf(
                weather(hoursAgo = 4, humidity = 25f),
                weather(hoursAgo = 28, humidity = 20f),
                weather(hoursAgo = 52, humidity = 28f),
            ),
            now = now,
        )

        assertEquals(now + 9.days, result.scheduledAt)
    }

    // Rule: last 2 care logs have DAMP soil → extend by 1 day. Also verify clamp.
    // Combined: outdoor heavy rain (*1.5) + damp-soil (+1d) = 10*1.5 + 1 = 16,
    // still within [5, 20], so 16 days.
    @Test
    fun `damp soil streak extends by one day and interval stays clamped`() {
        val result = useCase(
            plant = plant(location = LocationTag.OUTDOOR),
            species = species(waterFrequencyDays = 10),
            recentLogs = listOf(
                log(daysAgo = 1, moisture = SoilMoistureNote.DAMP),
                log(daysAgo = 3, moisture = SoilMoistureNote.DAMP),
            ),
            recentWeather = listOf(
                weather(hoursAgo = 4, rain = 8f),
                weather(hoursAgo = 28, rain = 6f),
                weather(hoursAgo = 52, rain = 0f),
            ),
            now = now,
        )

        assertEquals(now + 16.days, result.scheduledAt)

        // Sanity: clamp upper bound is waterFrequencyDays * 2
        val extreme = useCase(
            plant = plant(location = LocationTag.OUTDOOR),
            species = species(waterFrequencyDays = 4),
            recentLogs = listOf(
                log(daysAgo = 1, moisture = SoilMoistureNote.DAMP),
                log(daysAgo = 2, moisture = SoilMoistureNote.DAMP),
            ),
            recentWeather = listOf(
                weather(hoursAgo = 4, rain = 20f),
                weather(hoursAgo = 28, rain = 20f),
                weather(hoursAgo = 52, rain = 20f),
            ),
            now = now,
        )
        assertTrue(
            "expected clamped to base*2 (${4 * 2}), got ${extreme.scheduledAt - now}",
            extreme.scheduledAt == now + (4 * 2).days,
        )
    }
}
