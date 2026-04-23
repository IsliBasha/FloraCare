package com.floracare.app.ui.feature.dashboard

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours

class DashboardMappingTest {

    private val tz = TimeZone.UTC
    private val now = Instant.parse("2026-04-23T12:00:00Z")
    private val today = now.toLocalDateTime(tz).date

    @Test
    fun `empty logs yields 30 zero days with streak and total at zero`() {
        val snap = toDashboard(
            logs = emptyList(),
            plants = emptyList(),
            species = emptyList(),
            now = now,
            tz = tz,
        )

        assertEquals(DASHBOARD_WINDOW_DAYS, snap.dailyWaterCounts.size)
        assertTrue(snap.dailyWaterCounts.all { it.count == 0 })
        assertEquals(today, snap.dailyWaterCounts.last().date)
        assertEquals(today.minus(DatePeriod(days = DASHBOARD_WINDOW_DAYS - 1)), snap.dailyWaterCounts.first().date)
        assertEquals(0, snap.currentStreakDays)
        assertEquals(0, snap.totalWatersLast30d)
        assertNull(snap.plantOfTheMonth)
    }

    @Test
    fun `WATER logs bucket into their local date, other task types excluded`() {
        val plant = plant("p1", "Mona")
        val snap = toDashboard(
            logs = listOf(
                waterLog("l1", "p1", at = now),                  // today, water
                waterLog("l2", "p1", at = now - 6.hours),        // today, water
                waterLog("l3", "p1", at = now - 26.hours),       // yesterday, water
                careLog("l4", "p1", type = CareTaskType.FERTILIZE, at = now),
                careLog("l5", "p1", type = CareTaskType.MIST, at = now - 2.hours),
            ),
            plants = listOf(plant),
            species = emptyList(),
            now = now,
            tz = tz,
        )

        assertEquals(2, snap.dailyWaterCounts.last().count)
        assertEquals(1, snap.dailyWaterCounts[snap.dailyWaterCounts.size - 2].count)
        assertEquals(3, snap.totalWatersLast30d)
    }

    @Test
    fun `current streak counts consecutive days ending today with at least one water`() {
        val logs = listOf(
            waterLog("a", "p1", at = now),                              // today
            waterLog("b", "p1", at = now - 24.hours),                   // -1d
            waterLog("c", "p1", at = now - 48.hours),                   // -2d
            waterLog("d", "p1", at = now - 96.hours),                   // -4d (gap at -3d breaks streak)
        )
        val snap = toDashboard(
            logs = logs,
            plants = listOf(plant("p1", "Mona")),
            species = emptyList(),
            now = now,
            tz = tz,
        )
        assertEquals(3, snap.currentStreakDays)
    }

    @Test
    fun `streak is zero when today has no water log even if yesterday has one`() {
        val snap = toDashboard(
            logs = listOf(waterLog("a", "p1", at = now - 26.hours)),
            plants = listOf(plant("p1", "Mona")),
            species = emptyList(),
            now = now,
            tz = tz,
        )
        assertEquals(0, snap.currentStreakDays)
    }

    @Test
    fun `plant of the month picks the most-watered plant over the window`() {
        val logs = listOf(
            waterLog("a", "p1", at = now - 1.hours),
            waterLog("b", "p1", at = now - 2.hours),
            waterLog("c", "p2", at = now - 3.hours),
        )
        val snap = toDashboard(
            logs = logs,
            plants = listOf(plant("p1", "Mona"), plant("p2", "Finn")),
            species = emptyList(),
            now = now,
            tz = tz,
        )
        val potm = snap.plantOfTheMonth
        assertNotNull(potm)
        assertEquals("p1", potm!!.plantId)
        assertEquals(2, potm.waterCount)
    }

    @Test
    fun `ties on count break in favour of the plant with the most recent water`() {
        val logs = listOf(
            waterLog("a", "p1", at = now - 10.hours),
            waterLog("b", "p1", at = now - 20.hours),
            waterLog("c", "p2", at = now - 1.hours),   // more recent
            waterLog("d", "p2", at = now - 30.hours),
        )
        val snap = toDashboard(
            logs = logs,
            plants = listOf(plant("p1", "Mona"), plant("p2", "Finn")),
            species = emptyList(),
            now = now,
            tz = tz,
        )
        assertEquals("p2", snap.plantOfTheMonth?.plantId)
    }

    @Test
    fun `logs for deleted plants are ignored when picking plant of the month`() {
        val snap = toDashboard(
            logs = listOf(
                waterLog("a", "ghost", at = now - 1.hours),
                waterLog("b", "ghost", at = now - 2.hours),
                waterLog("c", "p1", at = now - 3.hours),
            ),
            plants = listOf(plant("p1", "Mona")),
            species = emptyList(),
            now = now,
            tz = tz,
        )
        assertEquals("p1", snap.plantOfTheMonth?.plantId)
        assertEquals(1, snap.plantOfTheMonth?.waterCount)
    }

    @Test
    fun `plant of the month exposes species common name when available`() {
        val snap = toDashboard(
            logs = listOf(waterLog("a", "p1", at = now - 1.hours)),
            plants = listOf(plant("p1", "Mona", speciesId = "sp-1")),
            species = listOf(species("sp-1", common = "Snake Plant")),
            now = now,
            tz = tz,
        )
        assertEquals("Snake Plant", snap.plantOfTheMonth?.speciesName)
    }

    private fun plant(id: String, nick: String, speciesId: String? = null) = Plant(
        id = id,
        nickname = nick,
        speciesId = speciesId,
        locationTag = LocationTag.INDOOR,
        acquiredAt = now - 500.hours,
        coverPhotoUri = null,
        notes = "",
    )

    private fun species(id: String, common: String) = Species(
        id = id,
        scientificName = "X",
        commonName = common,
        waterFrequencyDays = 7,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.MEDIUM,
        temperatureRangeC = 18f..28f,
        toxicity = Toxicity.NONE,
        careNotes = "",
    )

    private fun waterLog(id: String, plantId: String, at: Instant) =
        careLog(id, plantId, type = CareTaskType.WATER, at = at)

    private fun careLog(id: String, plantId: String, type: CareTaskType, at: Instant) = CareLog(
        id = id,
        plantId = plantId,
        taskType = type,
        performedAt = at,
        soilMoistureNote = null,
        userNote = null,
    )
}
