package com.floracare.app.data.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.floracare.app.BuildConfig

/**
 * Debug-only broadcast receiver. Lets you fire the [DailyCareScheduler] on
 * demand without waiting for 07:00:
 *
 * ```
 * adb shell am broadcast \
 *   -a com.floracare.app.ACTION_DEBUG_RUN_SCHEDULER \
 *   -n com.floracare.app/.data.worker.DebugTriggerReceiver
 * ```
 *
 * No-ops on release builds.
 */
class DebugTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) return
        if (intent.action != ACTION_DEBUG_RUN_SCHEDULER) return

        val forceAll = intent.getBooleanExtra(EXTRA_FORCE_ALL, false)
        val data = androidx.work.Data.Builder()
            .putBoolean(DailyCareScheduler.KEY_FORCE_ALL, forceAll)
            .build()
        val request = OneTimeWorkRequestBuilder<DailyCareScheduler>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    companion object {
        const val ACTION_DEBUG_RUN_SCHEDULER = "com.floracare.app.ACTION_DEBUG_RUN_SCHEDULER"
        const val EXTRA_FORCE_ALL = "force_all"
    }
}
