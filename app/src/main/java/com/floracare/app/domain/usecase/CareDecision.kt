package com.floracare.app.domain.usecase

import com.floracare.app.domain.model.CareAdjustmentReason
import com.floracare.app.domain.model.CareTask

/**
 * Result of [ComputeNextCareTaskUseCase.decide]: the upserted [task] plus the
 * stable set of modifier rules that fired against the baseline interval.
 *
 * Reasons are surfaced verbatim by the UI; the schedule date itself remains
 * canonical on `task.scheduledAt`.
 */
data class CareDecision(
    val task: CareTask,
    val reasons: Set<CareAdjustmentReason>,
)
