package com.floracare.app.data.worker

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService

/**
 * Handles notification action buttons (Mark done, Snooze 2 days) without opening the app.
 * Dismisses the notification for tactile feedback, then enqueues a [CareActionWorker]
 * so the DB write survives receiver teardown.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (notificationId != -1) {
            context.getSystemService<NotificationManager>()?.cancel(notificationId)
        }

        val workerAction = when (intent.action) {
            ACTION_MARK_DONE -> CareActionWorker.ACTION_MARK_DONE
            ACTION_SNOOZE_2D -> CareActionWorker.ACTION_SNOOZE_2D
            else -> return
        }
        CareActionWorker.enqueue(context, taskId = taskId, action = workerAction)
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val ACTION_MARK_DONE = "com.floracare.app.ACTION_MARK_DONE"
        const val ACTION_SNOOZE_2D = "com.floracare.app.ACTION_SNOOZE_2D"
    }
}
