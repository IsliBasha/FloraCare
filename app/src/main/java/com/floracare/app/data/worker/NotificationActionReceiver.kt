package com.floracare.app.data.worker

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService

/**
 * Handles notification action buttons (Mark done, Snooze 2 days) without opening the app.
 * Persisting the action to the database is wired up via Hilt in [data/repository/CareRepository].
 * This receiver stays lightweight; the heavy DB work is enqueued to WorkManager.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        // Dismiss the notification immediately to give tactile feedback.
        if (notificationId != -1) {
            context.getSystemService<NotificationManager>()?.cancel(notificationId)
        }

        // TODO(person-a): enqueue a OneTimeWorkRequest for CareActionWorker
        //                 that persists MARK_DONE or SNOOZE depending on intent.action.
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val ACTION_MARK_DONE = "com.floracare.app.ACTION_MARK_DONE"
        const val ACTION_SNOOZE_2D = "com.floracare.app.ACTION_SNOOZE_2D"
    }
}
