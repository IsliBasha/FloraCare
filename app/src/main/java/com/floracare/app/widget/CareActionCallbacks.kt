package com.floracare.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.floracare.app.data.worker.CareActionWorker

/** Shared ActionParameters key for passing task IDs into widget action callbacks. */
val WidgetTaskIdKey = ActionParameters.Key<String>(CareActionWorker.KEY_TASK_ID)

/** Glance action callback: marks a care task complete from the home-screen widget. */
class MarkDoneWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[WidgetTaskIdKey] ?: return
        CareActionWorker.enqueue(context, taskId, CareActionWorker.ACTION_MARK_DONE)
    }
}

/** Glance action callback: snoozes a care task by 2 days from the home-screen widget. */
class SnoozeWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[WidgetTaskIdKey] ?: return
        CareActionWorker.enqueue(context, taskId, CareActionWorker.ACTION_SNOOZE_2D)
    }
}
