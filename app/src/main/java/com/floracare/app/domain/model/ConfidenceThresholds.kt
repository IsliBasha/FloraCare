package com.floracare.app.domain.model

/**
 * Product-policy thresholds shared across the identify flow and the species
 * resolver. Calibrated on-device against AIY Vision Plants V1 — see
 * `AiyCalibrationTest` for the evidence.
 */
object ConfidenceThresholds {
    /**
     * Below this top-1 score the classifier is effectively guessing against a
     * 2,102-class vocabulary that lacks most indoor tropicals. UI nudges the
     * user; the resolver fetches real taxonomy from Perenual.
     */
    const val LOW_CONFIDENCE: Float = 0.25f
}

/**
 * Shared defaults applied whenever a [Species] is synthesised locally or a
 * remote payload is missing fields. Kept next to [ConfidenceThresholds] so the
 * resolver, the Perenual mapper, and any future provider agree on the same
 * conservative indoor-plant profile.
 */
object SpeciesDefaults {
    const val WATER_FREQUENCY_DAYS: Int = 7
    const val MIN_TEMP_C: Float = 15f
    const val MAX_TEMP_C: Float = 28f
}
