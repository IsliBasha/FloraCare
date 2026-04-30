package com.floracare.app.domain.model

/**
 * Why the adaptive scheduler shifted a watering task off the species default.
 * Returned alongside a [CareTask] from [com.floracare.app.domain.usecase.ComputeNextCareTaskUseCase.decide].
 *
 * Stable, closed set — UI maps each value to a human label without an `else` branch.
 */
enum class CareAdjustmentReason {
    /** 3-day average temperature exceeded the heat threshold; interval shortened. */
    HEAT,

    /** Outdoor plant + 3-day rain total exceeded the rain threshold; interval extended. */
    RAIN,

    /** 3-day average humidity dropped below the dry-air threshold; interval shortened. */
    LOW_HUMIDITY,

    /** Last two care logs both reported damp soil; interval extended by a day. */
    DAMP_SOIL,
}
