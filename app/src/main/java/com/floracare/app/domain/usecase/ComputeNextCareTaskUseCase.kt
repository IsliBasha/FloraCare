package com.floracare.app.domain.usecase

import com.floracare.app.domain.model.CareAdjustmentReason
import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.CareTaskSource
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.SoilMoistureNote
import com.floracare.app.domain.model.WeatherSnapshot
import kotlinx.datetime.Instant
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days

/**
 * Computes the next adaptive watering task for a plant.
 *
 * Base interval = `species.waterFrequencyDays`, modified by:
 *   - avg temp last 3 days > 28°C           → shorten by 20%      (HEAT)
 *   - total rain last 3 days > 10mm AND plant outdoor → extend by 50%   (RAIN)
 *   - avg humidity last 3 days < 30%        → shorten by 15%      (LOW_HUMIDITY)
 *   - last 2 care logs soilMoistureNote DAMP → extend by 1 day    (DAMP_SOIL)
 *
 * Final interval clamped to `[base / 2, base * 2]`.
 *
 * Pure function: no I/O, no Android imports. Safe to unit test.
 */
class ComputeNextCareTaskUseCase {

    operator fun invoke(
        plant: Plant,
        species: Species,
        recentLogs: List<CareLog>,
        recentWeather: List<WeatherSnapshot>,
        now: Instant,
    ): CareTask = decide(plant, species, recentLogs, recentWeather, now).task

    fun decide(
        plant: Plant,
        species: Species,
        recentLogs: List<CareLog>,
        recentWeather: List<WeatherSnapshot>,
        now: Instant,
    ): CareDecision {
        val base = species.waterFrequencyDays
        val window3d = now - 3.days
        val recent3d = recentWeather.filter { it.recordedAt >= window3d }

        var factor = 1.0
        var extraDays = 0
        val reasons = mutableSetOf<CareAdjustmentReason>()

        if (recent3d.isNotEmpty() && recent3d.avg { it.tempC.toDouble() } > TEMP_THRESHOLD_C) {
            factor *= 0.8
            reasons += CareAdjustmentReason.HEAT
        }
        if (plant.locationTag == LocationTag.OUTDOOR &&
            recent3d.sumOf { it.rainMm.toDouble() } > RAIN_THRESHOLD_MM
        ) {
            factor *= 1.5
            reasons += CareAdjustmentReason.RAIN
        }
        if (recent3d.isNotEmpty() && recent3d.avg { it.humidityPct.toDouble() } < HUMIDITY_THRESHOLD_PCT) {
            factor *= 0.85
            reasons += CareAdjustmentReason.LOW_HUMIDITY
        }
        if (hasTrailingDampStreak(recentLogs)) {
            extraDays += 1
            reasons += CareAdjustmentReason.DAMP_SOIL
        }

        val intervalDays = (base * factor).roundToInt() + extraDays
        val clamped = intervalDays.coerceIn(base / 2, base * 2)

        val task = CareTask(
            id = "task-${plant.id}-${now.toEpochMilliseconds()}",
            plantId = plant.id,
            type = CareTaskType.WATER,
            scheduledAt = now + clamped.days,
            source = CareTaskSource.ADAPTIVE,
        )
        return CareDecision(task = task, reasons = reasons.toSet())
    }

    private fun hasTrailingDampStreak(logs: List<CareLog>): Boolean {
        val lastTwo = logs.sortedByDescending { it.performedAt }.take(2)
        if (lastTwo.size < 2) return false
        return lastTwo.all { it.soilMoistureNote == SoilMoistureNote.DAMP }
    }

    private inline fun <T> List<T>.avg(selector: (T) -> Double): Double =
        sumOf(selector) / size

    companion object {
        private const val TEMP_THRESHOLD_C = 28.0
        private const val RAIN_THRESHOLD_MM = 10.0
        private const val HUMIDITY_THRESHOLD_PCT = 30.0
    }
}
