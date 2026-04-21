package com.floracare.app.data.notification

/**
 * Stable notification + request codes derived from the task id so re-posting
 * replaces an existing notification in place instead of spawning duplicates.
 */
internal object NotificationIds {
    fun forTask(taskId: String): Int = taskId.hashCode()

    /** Unique per (task, action) so PendingIntent cache lookups don't collide. */
    fun actionRequestCode(taskId: String, action: String): Int =
        (taskId.hashCode() * 31) xor action.hashCode()

    /** Content-tap PI request code, distinct from action codes. */
    fun contentRequestCode(taskId: String): Int = (taskId.hashCode() * 31) xor CONTENT_SALT

    private const val CONTENT_SALT = 0x70CA_5E1F.toInt()
}
