package com.floracare.app.ui.feature.dashboard

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.model.WeatherSnapshot
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

    private fun plant(id: String, nick: String, speciesId: String? = null, archived: Boolean = false) = Plant(
        id = id,
        nickname = nick,
        speciesId = speciesId,
        locationTag = LocationTag.INDOOR,
        acquiredAt = now - 500.hours,
        coverPhotoUri = null,
        notes = "",
        archived = archived,
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

    private fun weather(id: String, recordedAt: Instant, tempC: Float = 20f) = WeatherSnapshot(
        id = id,
        lat = 41.32,
        lon = 19.82,
        recordedAt = recordedAt,
        tempC = tempC,
        humidityPct = 60f,
        rainMm = 0f,
        uvIndex = 0f,
    )

    @Test
    fun `currentWeather is null when no snapshots are supplied`() {
        val snap = toDashboard(
            logs = emptyList(),
            plants = emptyList(),
            species = emptyList(),
            weather = emptyList(),
            now = now,
            tz = tz,
        )
        assertNull(snap.currentWeather)
    }

    @Test
    fun `currentWeather picks the most recent snapshot regardless of input order`() {
        val older = weather("w-old", recordedAt = now - 4.hours, tempC = 17f)
        val newest = weather("w-new", recordedAt = now - 30.minutes, tempC = 22f)
        val mid = weather("w-mid", recordedAt = now - 2.hours, tempC = 19f)

        val snap = toDashboard(
            logs = emptyList(),
            plants = emptyList(),
            species = emptyList(),
            weather = listOf(older, newest, mid),
            now = now,
            tz = tz,
        )

        assertNotNull(snap.currentWeather)
        assertEquals("w-new", snap.currentWeather?.id)
        assertEquals(22f, snap.currentWeather?.tempC)
    }

    @Test
    fun `currentWeather is null when the newest snapshot is older than the freshness window`() {
        val ancient = weather("w-ancient", recordedAt = now - 25.hours, tempC = 17f)
        val snap = toDashboard(
            logs = emptyList(),
            plants = emptyList(),
            species = emptyList(),
            weather = listOf(ancient),
            now = now,
            tz = tz,
        )
        assertNull(snap.currentWeather)
    }

    @Test
    fun `formatWeatherAge produces expected human-friendly strings`() {
        assertEquals("just now", formatWeatherAge(now, now))
        assertEquals("just now", formatWeatherAge(now, now - 30.seconds))
        assertEquals("5m ago", formatWeatherAge(now, now - 5.minutes))
        assertEquals("59m ago", formatWeatherAge(now, now - 59.minutes))
        assertEquals("1h ago", formatWeatherAge(now, now - 60.minutes))
        assertEquals("3h ago", formatWeatherAge(now, now - 3.hours))
        assertEquals("23h ago", formatWeatherAge(now, now - 23.hours))
        assertEquals("2d ago", formatWeatherAge(now, now - 48.hours))
        // future timestamps clamp to "just now" rather than negative
        assertEquals("just now", formatWeatherAge(now, now + 5.minutes))
    }

    @Test
    fun `currentWeather returns the snapshot when it sits exactly at the freshness boundary`() {
        val edge = weather("w-edge", recordedAt = now - 24.hours, tempC = 12f)
        val snap = toDashboard(
            logs = emptyList(),
            plants = emptyList(),
            species = emptyList(),
            weather = listOf(edge),
            now = now,
            tz = tz,
        )
        assertNotNull(snap.currentWeather)
        assertEquals("w-edge", snap.currentWeather?.id)
    }

    // ── CareCompletion ring ────────────────────────────────────────────────────

    @Test
    fun `weekCareCompletion is zero-zero when there are no plants`() {
        val snap = toDashboard(
            logs = emptyList(),
            plants = emptyList(),
            species = emptyList(),
            now = now,
            tz = tz,
        )
        assertEquals(0, snap.weekCareCompletion.plantsWateredCount)
        assertEquals(0, snap.weekCareCompletion.totalActivePlants)
    }

    @Test
    fun `weekCareCompletion counts active plants and those watered in the last 7 days`() {
        val plants = listOf(
            plant("p1", "Mona"),
            plant("p2", "Finn"),
            plant("p3", "Rex"),
        )
        val logs = listOf(
            waterLog("l1", "p1", at = now - 2.hours),
            waterLog("l2", "p2", at = now - 48.hours),
        )
        val snap = toDashboard(logs = logs, plants = plants, species = emptyList(), now = now, tz = tz)
        assertEquals(2, snap.weekCareCompletion.plantsWateredCount)
        assertEquals(3, snap.weekCareCompletion.totalActivePlants)
    }

    @Test
    fun `weekCareCompletion does not count a plant watered outside the 7-day window`() {
        val plants = listOf(plant("p1", "Mona"))
        val logs = listOf(
            waterLog("l1", "p1", at = now - (7 * 24 + 1).hours),
        )
        val snap = toDashboard(logs = logs, plants = plants, species = emptyList(), now = now, tz = tz)
        assertEquals(0, snap.weekCareCompletion.plantsWateredCount)
        assertEquals(1, snap.weekCareCompletion.totalActivePlants)
    }

    @Test
    fun `weekCareCompletion counts a plant only once even if watered multiple times in the window`() {
        val plants = listOf(plant("p1", "Mona"))
        val logs = listOf(
            waterLog("l1", "p1", at = now - 1.hours),
            waterLog("l2", "p1", at = now - 24.hours),
            waterLog("l3", "p1", at = now - 48.hours),
        )
        val snap = toDashboard(logs = logs, plants = plants, species = emptyList(), now = now, tz = tz)
        assertEquals(1, snap.weekCareCompletion.plantsWateredCount)
        assertEquals(1, snap.weekCareCompletion.totalActivePlants)
    }

    @Test
    fun `weekCareCompletion excludes non-WATER care log types`() {
        val plants = listOf(plant("p1", "Mona"))
        val logs = listOf(
            careLog("l1", "p1", type = CareTaskType.FERTILIZE, at = now - 1.hours),
            careLog("l2", "p1", type = CareTaskType.MIST, at = now - 2.hours),
        )
        val snap = toDashboard(logs = logs, plants = plants, species = emptyList(), now = now, tz = tz)
        assertEquals(0, snap.weekCareCompletion.plantsWateredCount)
        assertEquals(1, snap.weekCareCompletion.totalActivePlants)
    }

    @Test
    fun `weekCareCompletion excludes archived plants from both counts`() {
        val plants = listOf(
            plant("p1", "Mona"),
            plant("p2", "Archived", archived = true),
        )
        val logs = listOf(
            waterLog("l1", "p1", at = now - 1.hours),
            waterLog("l2", "p2", at = now - 2.hours),
        )
        val snap = toDashboard(logs = logs, plants = plants, species = emptyList(), now = now, tz = tz)
        assertEquals(1, snap.weekCareCompletion.plantsWateredCount)
        assertEquals(1, snap.weekCareCompletion.totalActivePlants)
    }

    @Test
    fun `weekCareCompletion includes log at exactly the 7-day boundary`() {
        val plants = listOf(plant("p1", "Mona"))
        val logs = listOf(
            waterLog("l1", "p1", at = now - (7 * 24).hours),
        )
        val snap = toDashboard(logs = logs, plants = plants, species = emptyList(), now = now, tz = tz)
        assertEquals(1, snap.weekCareCompletion.plantsWateredCount)
    }

    // ── Sparkline week labels ──────────────────────────────────────────────────

    @Test
    fun `sparklineWeekLabels has one entry per day matching dailyWaterCounts size`() {
        val snap = toDashboard(
            logs = emptyList(),
            plants = emptyList(),
            species = emptyList(),
            now = now,
            tz = tz,
        )
        assertEquals(snap.dailyWaterCounts.size, snap.sparklineWeekLabels.size)
    }

    @Test
    fun `sparklineWeekLabels is non-null only at Monday boundaries`() {
        val snap = toDashboard(
            logs = emptyList(),
            plants = emptyList(),
            species = emptyList(),
            now = now,
            tz = tz,
        )
        val nonNullLabels = snap.sparklineWeekLabels.filterNotNull()
        assertTrue("expected at least one week label over 30 days", nonNullLabels.isNotEmpty())
        snap.sparklineWeekLabels.forEachIndexed { i, label ->
            if (label != null) {
                val date = snap.dailyWaterCounts[i].date
                assertEquals(
                    "label at index $i ($date) is not Monday",
                    DayOfWeek.MONDAY,
                    date.dayOfWeek,
                )
            }
        }
    }

    @Test
    fun `sparklineWeekLabels format is abbreviated month and day`() {
        // now = 2026-04-23 (Thursday). Window starts 2026-03-25.
        // First Monday in window = 2026-03-30.
        val snap = toDashboard(
            logs = emptyList(),
            plants = emptyList(),
            species = emptyList(),
            now = now,
            tz = tz,
        )
        val firstLabel = snap.sparklineWeekLabels.first { it != null }
        assertEquals("Mar 30", firstLabel)
    }

}
