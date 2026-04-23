package com.floracare.app.ui.feature.dashboard

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Size of the dashboard trend window, in days (inclusive of today). */
const val DASHBOARD_WINDOW_DAYS: Int = 30

data class DailyCount(val date: LocalDate, val count: Int)

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
)

/**
 * Rolls raw care logs + plants/species into everything the dashboard shows.
 * Pure function — no clock, no IO, no side effects. Caller supplies [now].
 */
fun toDashboard(
    logs: List<CareLog>,
    plants: List<Plant>,
    species: List<Species>,
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

    return DashboardSnapshot(
        dailyWaterCounts = dailyWaterCounts,
        currentStreakDays = currentStreakDays,
        totalWatersLast30d = totalWatersLast30d,
        plantOfTheMonth = plantOfTheMonth,
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
