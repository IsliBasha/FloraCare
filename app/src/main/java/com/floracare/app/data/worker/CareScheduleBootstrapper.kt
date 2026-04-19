package com.floracare.app.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules [DailyCareScheduler] as a periodic worker. Idempotent via
 * [ExistingPeriodicWorkPolicy.KEEP] so app relaunches don't create duplicates.
 */
object CareScheduleBootstrapper {
    private const val UNIQUE_NAME = "floracare.daily-care-scheduler"

    fun enqueue(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyCareScheduler>(
            repeatInterval = 24, repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build(),
            )
            .setInitialDelay(nextSevenAmDelayMinutes(), TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Minutes from now until the next local 07:00. Always positive. */
    private fun nextSevenAmDelayMinutes(): Long {
        val cal = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 7)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= cal.timeInMillis) add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        return ((target.timeInMillis - cal.timeInMillis) / 60_000).coerceAtLeast(1)
    }
}
