package com.floracare.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.domain.repository.WeatherRepository
import com.floracare.app.domain.usecase.ComputeNextCareTaskUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

/**
 * Runs at 07:00 local (scheduled by the app), uses the adaptive care engine to
 * compute today's top tasks, and enqueues notifications through the system channel.
 *
 * Notification building is delegated to [NotificationDispatcher] in a follow-up ticket;
 * this worker keeps the scheduling contract intact so the scaffold compiles today.
 */
@HiltWorker
class DailyCareScheduler @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val plants: PlantRepository,
    private val weather: WeatherRepository,
    private val computeNextTask: ComputeNextCareTaskUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = Clock.System.now()
        val since = now - 14.days
        val recentWeather = weather.recentWeather(since = now - 7.days)

        plants.observePlants().first().forEach { plant ->
            val species = plant.speciesId?.let { plants.findSpecies(it) } ?: return@forEach
            val logs = plants.recentLogs(plant.id, since)
            val task = computeNextTask(
                plant = plant,
                species = species,
                recentLogs = logs,
                recentWeather = recentWeather,
                now = now,
            )
            plants.upsertTask(task)
        }
        return Result.success()
    }
}
