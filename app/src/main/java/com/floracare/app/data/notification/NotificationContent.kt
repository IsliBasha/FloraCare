package com.floracare.app.data.notification

import com.floracare.app.CareChannel
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species

/**
 * Pure, Android-free copy helper. Kept separate from [NotificationDispatcher] so
 * the string formatting + channel routing can be exercised by JVM unit tests.
 */
data class NotificationContent(
    val channel: CareChannel,
    val title: String,
    val body: String,
)

fun buildNotificationContent(
    plant: Plant,
    species: Species?,
    type: CareTaskType,
): NotificationContent {
    val channel = channelFor(type)
    val verb = when (type) {
        CareTaskType.WATER -> "Water"
        CareTaskType.FERTILIZE -> "Fertilize"
        CareTaskType.MIST -> "Mist"
        CareTaskType.ROTATE -> "Rotate"
        CareTaskType.REPOT -> "Repot"
        CareTaskType.PRUNE -> "Prune"
    }
    val title = "$verb ${plant.nickname}"
    val body = species?.commonName?.takeIf { it.isNotBlank() }
        ?.let { "Time to $verb ${plant.nickname} ($it)." }
        ?: "Time to $verb ${plant.nickname}."
    return NotificationContent(channel = channel, title = title, body = body)
}

fun channelFor(type: CareTaskType): CareChannel = when (type) {
    CareTaskType.WATER -> CareChannel.WATER
    CareTaskType.FERTILIZE -> CareChannel.FERTILIZE
    CareTaskType.MIST, CareTaskType.ROTATE, CareTaskType.REPOT, CareTaskType.PRUNE -> CareChannel.OTHER
}
