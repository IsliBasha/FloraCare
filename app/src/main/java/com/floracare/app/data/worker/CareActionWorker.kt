package com.floracare.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.floracare.app.domain.repository.PlantRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

/**
 * Persists notification-action results (mark done or snooze 2 days). Lives as a
 * worker so the broadcast receiver can return quickly and heavy DB writes survive
 * process death.
 */
@HiltWorker
class CareActionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val plants: PlantRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val action = inputData.getString(KEY_ACTION) ?: return Result.failure()
        val now = Clock.System.now()
        return when (applyCareAction(plants, taskId, action, now)) {
            CareActionOutcome.Success -> Result.success()
            CareActionOutcome.UnknownAction -> Result.failure()
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_ACTION = "action"
        const val ACTION_MARK_DONE = "MARK_DONE"
        const val ACTION_SNOOZE_2D = "SNOOZE_2D"

        fun enqueue(context: Context, taskId: String, action: String) {
            val request = OneTimeWorkRequestBuilder<CareActionWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_TASK_ID, taskId)
                        .putString(KEY_ACTION, action)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}

enum class CareActionOutcome { Success, UnknownAction }

/**
 * Pure orchestration of a notification action against the repository. Extracted
 * from [CareActionWorker] so the decision can be unit-tested without Android.
 */
suspend fun applyCareAction(
    plants: PlantRepository,
    taskId: String,
    action: String,
    now: Instant,
): CareActionOutcome = when (action) {
    CareActionWorker.ACTION_MARK_DONE -> {
        plants.markTaskComplete(taskId, now)
        CareActionOutcome.Success
    }
    CareActionWorker.ACTION_SNOOZE_2D -> {
        plants.snoozeTask(taskId, now + 2.days)
        CareActionOutcome.Success
    }
    else -> CareActionOutcome.UnknownAction
}
