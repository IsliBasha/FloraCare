package com.floracare.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.floracare.app.data.notification.NotificationDispatcher
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.repository.LocationProvider
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.domain.repository.WeatherRepository
import com.floracare.app.domain.usecase.ComputeNextCareTaskUseCase
import androidx.glance.appwidget.updateAll
import com.floracare.app.widget.TodayTasksWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

/**
 * Runs at 07:00 local (scheduled by [CareScheduleBootstrapper]):
 *  1. Recomputes each plant's next adaptive care task and upserts it.
 *  2. Posts a notification for every task that's due within the next 24 h.
 *
 * Notification building is delegated to [NotificationDispatcher] so this worker
 * stays focused on the scheduling + posting orchestration.
 *
 * The worker also accepts a `force_all` boolean in its input data — when true,
 * it posts notifications for every plant's computed next task regardless of the
 * 24 h due window. Intended for the [DebugTriggerReceiver] demo path and never
 * set by the periodic enqueue.
 */
@HiltWorker
class DailyCareScheduler @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val plants: PlantRepository,
    private val weather: WeatherRepository,
    private val locationProvider: LocationProvider,
    private val computeNextTask: ComputeNextCareTaskUseCase,
    private val notifications: NotificationDispatcher,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = Clock.System.now()
        val since = now - 14.days
        refreshWeatherFromCurrentLocation()
        val recentWeather = weather.recentWeather(since = now - 7.days)
        val forceAll = inputData.getBoolean(KEY_FORCE_ALL, false)

        val livePlants = plants.observePlants().first()
        val speciesCache = mutableMapOf<String, Species?>()

        livePlants.forEach { plant ->
            val speciesId = plant.speciesId ?: return@forEach
            val species = speciesCache.getOrPut(speciesId) { plants.findSpecies(speciesId) }
                ?: return@forEach

            val logs = plants.recentLogs(plant.id, since)
            val task = computeNextTask(
                plant = plant,
                species = species,
                recentLogs = logs,
                recentWeather = recentWeather,
                now = now,
            )
            plants.upsertTask(task)

            val snoozed = task.snoozedUntil?.let { it > now } == true
            val dueWithin24h = task.completedAt == null &&
                !snoozed &&
                task.scheduledAt <= now + 1.days
            if (forceAll || dueWithin24h) {
                notifications.post(task = task, plant = plant, species = species)
            }
        }
        refreshWidgetSafely()
        return Result.success()
    }

    private suspend fun refreshWidgetSafely() {
        try {
            TodayTasksWidget().updateAll(applicationContext)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Throwable) {
            // widget refresh is best-effort; never fail the worker over it
        }
    }

    private suspend fun refreshWeatherFromCurrentLocation() {
        val coords = runCatching { locationProvider.current() }.getOrNull() ?: return
        runCatching { weather.refresh(coords.lat, coords.lon) }
        // Failures (offline, rate-limited, no permission) are absorbed by the
        // repository: callers still see whatever cached snapshot exists.
    }

    companion object {
        const val KEY_FORCE_ALL = "force_all"
    }
}
