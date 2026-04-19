package com.floracare.app.domain.model

import kotlinx.datetime.Instant

data class CareLog(
    val id: String,
    val plantId: String,
    val taskType: CareTaskType,
    val performedAt: Instant,
    val soilMoistureNote: SoilMoistureNote?,
    val userNote: String? = null,
)
