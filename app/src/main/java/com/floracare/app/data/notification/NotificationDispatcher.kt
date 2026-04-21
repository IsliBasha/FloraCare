package com.floracare.app.data.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.floracare.app.MainActivity
import com.floracare.app.R
import com.floracare.app.data.worker.NotificationActionReceiver
import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts care-task notifications. Kept as a small, injected collaborator
 * so [com.floracare.app.data.worker.DailyCareScheduler] stays single-purpose.
 *
 * Silently no-ops when `POST_NOTIFICATIONS` is not granted; the runtime request
 * belongs to onboarding.
 */
@Singleton
class NotificationDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun post(task: CareTask, plant: Plant, species: Species?) {
        if (!canPost()) return
        val content = buildNotificationContent(plant, species, task.type)
        val notificationId = NotificationIds.forTask(task.id)

        val contentIntent = PendingIntent.getActivity(
            context,
            NotificationIds.contentRequestCode(task.id),
            deepLinkIntent(task.plantId),
            FLAGS,
        )
        val markDoneIntent = actionPendingIntent(
            task = task,
            action = NotificationActionReceiver.ACTION_MARK_DONE,
            notificationId = notificationId,
        )
        val snoozeIntent = actionPendingIntent(
            task = task,
            action = NotificationActionReceiver.ACTION_SNOOZE_2D,
            notificationId = notificationId,
        )

        val notification = NotificationCompat.Builder(context, content.channel.id)
            .setSmallIcon(R.drawable.ic_notification_leaf)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(
                NotificationCompat.Action.Builder(
                    /* icon = */ 0,
                    /* title = */ "Mark done",
                    /* intent = */ markDoneIntent,
                ).build(),
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    /* icon = */ 0,
                    /* title = */ "Snooze 2d",
                    /* intent = */ snoozeIntent,
                ).build(),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun cancel(taskId: String) {
        (context.getSystemService(NotificationManager::class.java))
            ?.cancel(NotificationIds.forTask(taskId))
    }

    private fun actionPendingIntent(
        task: CareTask,
        action: String,
        notificationId: Int,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, task.id)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            NotificationIds.actionRequestCode(task.id, action),
            intent,
            FLAGS,
        )
    }

    private fun deepLinkIntent(plantId: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PLANT_ID, plantId)
        }

    private fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        /** Intent extra read by [MainActivity] to deep-link into plant detail. */
        const val EXTRA_PLANT_ID = "com.floracare.app.extra.PLANT_ID"

        private const val FLAGS =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
