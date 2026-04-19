package com.floracare.app.domain.model

import kotlinx.datetime.Instant

data class CareTask(
    val id: String,
    val plantId: String,
    val type: CareTaskType,
    val scheduledAt: Instant,
    val completedAt: Instant? = null,
    val snoozedUntil: Instant? = null,
    val source: CareTaskSource = CareTaskSource.ADAPTIVE,
)
