package com.floracare.app.data.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-registers the periodic [DailyCareScheduler] after device reboot or app
 * upgrade. [CareScheduleBootstrapper.enqueue] is idempotent, so this is safe to
 * call unconditionally.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> CareScheduleBootstrapper.enqueue(context)
        }
    }
}
