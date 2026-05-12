package com.floracare.app.ui.feature.dashboard

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.WeatherSnapshot
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Size of the dashboard trend window, in days (inclusive of today). */
const val DASHBOARD_WINDOW_DAYS: Int = 30

/**
 * Snapshots older than this are considered stale and not surfaced as
 * "current weather" on the dashboard.
 */
val DASHBOARD_WEATHER_FRESHNESS: Duration = 24.hours

/** How many of the user's active plants were watered at least once in the last 7 days. */
data class CareCompletion(
    val plantsWateredCount: Int,
    val totalActivePlants: Int,
)

data class DailyCount(val date: LocalDate, val count: Int)

/**
 * Per-week breakdown of care actions for the health-trend card.
 * [weekLabel] is the abbreviated Monday date ("Apr 20"). List is ordered oldest → newest.
 */
data class WeeklyCareSummary(
    val weekLabel: String,
    val waterCount: Int,
    val otherCount: Int,
)

data class PlantOfTheMonth(
    val plantId: String,
    val nickname: String,
    val speciesName: String?,
    val waterCount: Int,
)

data class DashboardSnapshot(
    val dailyWaterCounts: List<DailyCount>,
    val currentStreakDays: Int,
    val totalWatersLast30d: Int,
    val plantOfTheMonth: PlantOfTheMonth?,
    val currentWeather: WeatherSnapshot?,
    /** X-of-Y care completion for the rolling 7-day window. */
    val weekCareCompletion: CareCompletion,
    /**
     * One optional label per entry in [dailyWaterCounts]; non-null only at
     * Monday (ISO day 1) boundaries, formatted as "MMM d" (e.g. "Mar 30").
     */
    val sparklineWeekLabels: List<String?>,
    /** 4-week grouped breakdown (water vs other care) for the health-trend card. */
    val weeklyCareSummary: List<WeeklyCareSummary>,
)

/**
 * Rolls raw care logs + plants/species into everything the dashboard shows.
 * Pure function — no clock, no IO, no side effects. Caller supplies [now].
 */
fun toDashboard(
    logs: List<CareLog>,
    plants: List<Plant>,
    species: List<Species>,
    weather: List<WeatherSnapshot> = emptyList(),
    now: Instant,
    tz: TimeZone = TimeZone.currentSystemDefault(),
): DashboardSnapshot {
    val today = now.toLocalDateTime(tz).date
    val windowStart = today.minus(DatePeriod(days = DASHBOARD_WINDOW_DAYS - 1))
    val waterLogs = logs.filter { it.taskType == CareTaskType.WATER }

    val waterCountsByDate: Map<LocalDate, Int> = waterLogs
        .groupingBy { it.performedAt.toLocalDateTime(tz).date }
        .eachCount()

    val dailyWaterCounts: List<DailyCount> = (0 until DASHBOARD_WINDOW_DAYS).map { offset ->
        val d = windowStart.plus(DatePeriod(days = offset))
        DailyCount(date = d, count = waterCountsByDate[d] ?: 0)
    }

    val totalWatersLast30d = dailyWaterCounts.sumOf { it.count }
    val currentStreakDays = countStreakEndingToday(waterCountsByDate, today)
    val plantOfTheMonth = pickPlantOfTheMonth(
        waterLogs = waterLogs.filter {
            val d = it.performedAt.toLocalDateTime(tz).date
            d >= windowStart && d <= today
        },
        plants = plants,
        species = species,
    )

    val currentWeather = weather
        .maxByOrNull { it.recordedAt }
        ?.takeIf { now - it.recordedAt <= DASHBOARD_WEATHER_FRESHNESS }

    val weekCareCompletion = computeWeekCareCompletion(waterLogs, plants, now)
    val sparklineWeekLabels = buildSparklineWeekLabels(dailyWaterCounts)
    val weeklyCareSummary = buildWeeklyCareSummary(logs = logs, now = now, tz = tz)

    return DashboardSnapshot(
        dailyWaterCounts = dailyWaterCounts,
        currentStreakDays = currentStreakDays,
        totalWatersLast30d = totalWatersLast30d,
        plantOfTheMonth = plantOfTheMonth,
        currentWeather = currentWeather,
        weekCareCompletion = weekCareCompletion,
        sparklineWeekLabels = sparklineWeekLabels,
        weeklyCareSummary = weeklyCareSummary,
    )
}

private fun countStreakEndingToday(
    waterCountsByDate: Map<LocalDate, Int>,
    today: LocalDate,
): Int {
    var streak = 0
    var cursor = today
    while ((waterCountsByDate[cursor] ?: 0) > 0) {
        streak += 1
        cursor = cursor.minus(DatePeriod(days = 1))
    }
    return streak
}

/**
 * Tiny human-friendly age formatter for the dashboard weather tile.
 * Pure function — no clock, no locale-specific resources.
 */
fun formatWeatherAge(now: Instant, recordedAt: Instant): String {
    val deltaMinutes = ((now - recordedAt).inWholeSeconds / 60).coerceAtLeast(0)
    return when {
        deltaMinutes < 1L -> "just now"
        deltaMinutes < 60L -> "${deltaMinutes}m ago"
        deltaMinutes < 24L * 60L -> "${deltaMinutes / 60L}h ago"
        else -> "${deltaMinutes / (24L * 60L)}d ago"
    }
}

private fun computeWeekCareCompletion(
    waterLogs: List<CareLog>,
    plants: List<Plant>,
    now: Instant,
): CareCompletion {
    val activePlants = plants.filter { !it.archived }
    if (activePlants.isEmpty()) return CareCompletion(0, 0)
    val weekStart = now - 7.days
    val wateredIds = waterLogs
        .filter { it.performedAt >= weekStart }
        .map { it.plantId }
        .toSet()
    val activeIds = activePlants.map { it.id }.toSet()
    val wateredActiveCount = wateredIds.count { it in activeIds }
    return CareCompletion(
        plantsWateredCount = wateredActiveCount,
        totalActivePlants = activePlants.size,
    )
}

private val MONTH_ABBREVS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun buildSparklineWeekLabels(days: List<DailyCount>): List<String?> =
    days.map { dc ->
        if (dc.date.dayOfWeek == DayOfWeek.MONDAY) {
            "${MONTH_ABBREVS[dc.date.monthNumber - 1]} ${dc.date.dayOfMonth}"
        } else {
            null
        }
    }

/**
 * Builds a [weeksBack]-entry list (oldest → newest) summarising water vs other-care
 * counts per ISO calendar week. The current partial week ends at [now]'s local date.
 */
fun buildWeeklyCareSummary(
    logs: List<CareLog>,
    now: Instant,
    tz: TimeZone = TimeZone.currentSystemDefault(),
    weeksBack: Int = 4,
): List<WeeklyCareSummary> {
    val today = now.toLocalDateTime(tz).date
    val currentWeekMonday = mostRecentMonday(today)

    return (weeksBack - 1 downTo 0).map { weeksAgo ->
        val weekMonday = currentWeekMonday.minus(DatePeriod(days = weeksAgo * 7))
        val weekSunday = weekMonday.plus(DatePeriod(days = 6))
        val rangeEnd = if (weeksAgo == 0) today else weekSunday

        val weekLogs = logs.filter { log ->
            val logDate = log.performedAt.toLocalDateTime(tz).date
            logDate >= weekMonday && logDate <= rangeEnd
        }

        WeeklyCareSummary(
            weekLabel = "${MONTH_ABBREVS[weekMonday.monthNumber - 1]} ${weekMonday.dayOfMonth}",
            waterCount = weekLogs.count { it.taskType == CareTaskType.WATER },
            otherCount = weekLogs.count { it.taskType != CareTaskType.WATER },
        )
    }
}

private fun mostRecentMonday(date: LocalDate): LocalDate {
    var d = date
    while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.minus(DatePeriod(days = 1))
    return d
}

private fun pickPlantOfTheMonth(
    waterLogs: List<CareLog>,
    plants: List<Plant>,
    species: List<Species>,
): PlantOfTheMonth? {
    if (waterLogs.isEmpty() || plants.isEmpty()) return null
    val plantsById = plants.associateBy { it.id }
    val speciesById = species.associateBy { it.id }

    val logsByPlant = waterLogs
        .filter { plantsById.containsKey(it.plantId) }
        .groupBy { it.plantId }
    if (logsByPlant.isEmpty()) return null

    val (plantId, logsForPlant) = logsByPlant.entries
        .maxWithOrNull(
            compareBy<Map.Entry<String, List<CareLog>>> { it.value.size }
                .thenBy { it.value.maxOf { log -> log.performedAt } },
        ) ?: return null

    val plant = plantsById.getValue(plantId)
    val speciesName = plant.speciesId?.let { speciesById[it]?.commonName }?.takeIf { it.isNotBlank() }
    return PlantOfTheMonth(
        plantId = plantId,
        nickname = plant.nickname,
        speciesName = speciesName,
        waterCount = logsForPlant.size,
    )
}
