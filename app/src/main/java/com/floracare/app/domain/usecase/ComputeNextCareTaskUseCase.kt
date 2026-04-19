package com.floracare.app.domain.usecase

import com.floracare.app.domain.model.CareLog
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.CareTaskSource
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.WeatherSnapshot
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

/**
 * Computes the next adaptive watering task for a plant.
 *
 * Base interval = species.waterFrequencyDays, modified by recent weather and user logs:
 *   - avg temp last 3 days > 28°C           → shorten by 20%
 *   - total rain last 3 days > 10mm AND outdoor → extend by 50%
 *   - humidity avg < 30%                    → shorten by 15%
 *   - last 2 logs soilMoistureNote = DAMP   → extend by 1 day
 *
 * Final interval clamped to [floor/2, floor*2] where floor = species.waterFrequencyDays.
 */
class ComputeNextCareTaskUseCase {

    operator fun invoke(
        plant: Plant,
        species: Species,
        recentLogs: List<CareLog>,
        recentWeather: List<WeatherSnapshot>,
        now: Instant,
    ): CareTask {
        throw NotImplementedError("RED: not yet implemented")
    }
}
