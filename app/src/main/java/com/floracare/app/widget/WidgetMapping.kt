package com.floracare.app.widget

import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.Plant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

const val WIDGET_MAX_ROWS: Int = 3

private const val FALLBACK_PLANT_NICKNAME = "Plant"

data class WidgetRow(
    val taskId: String,
    val plantId: String,
    val plantNickname: String,
    val taskType: CareTaskType,
    val scheduledAt: Instant,
    val dueLabel: String,
)

/**
 * Pure rollup for the home-screen Glance widget. Filters out completed and
 * still-snoozed tasks, sorts by [scheduledAt] ascending, looks up plant
 * nicknames, formats a relative-time label per row, and returns the first
 * [WIDGET_MAX_ROWS]. No I/O, no clock — caller supplies [now].
 */
fun toWidgetRows(
    now: Instant,
    tasks: List<CareTask>,
    plants: List<Plant>,
    tz: TimeZone = TimeZone.currentSystemDefault(),
): List<WidgetRow> {
    val plantsById = plants.associateBy { it.id }
    return tasks
        .filter { it.completedAt == null && !isSnoozedAtNow(it, now) }
        .sortedBy { it.scheduledAt }
        .take(WIDGET_MAX_ROWS)
        .map { task ->
            val nickname = plantsById[task.plantId]?.nickname?.takeIf { it.isNotBlank() }
                ?: FALLBACK_PLANT_NICKNAME
            WidgetRow(
                taskId = task.id,
                plantId = task.plantId,
                plantNickname = nickname,
                taskType = task.type,
                scheduledAt = task.scheduledAt,
                dueLabel = relativeDueLabel(task.scheduledAt, now, tz),
            )
        }
}

private fun isSnoozedAtNow(task: CareTask, now: Instant): Boolean =
    task.snoozedUntil?.let { it > now } == true

private fun relativeDueLabel(scheduledAt: Instant, now: Instant, tz: TimeZone): String {
    val today = now.toLocalDateTime(tz).date
    val day = scheduledAt.toLocalDateTime(tz).date
    val deltaDays = day.toEpochDays() - today.toEpochDays()
    return when {
        deltaDays == 0 -> "Today"
        deltaDays == 1 -> "Tomorrow"
        deltaDays > 1 -> "in ${deltaDays}d"
        deltaDays == -1 -> "1d ago"
        else -> "${-deltaDays}d ago"
    }
}
