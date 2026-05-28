package com.floracare.app.domain.model

import kotlinx.datetime.Instant

data class DiagnosisResult(
    val id: String,
    val plantId: String?,
    val photoUri: String,
    val diagnosisLabel: String,
    val confidence: Float,
    val treatmentSuggestion: String,
    val createdAt: Instant,
)
