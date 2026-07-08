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

    /**
     * Below this top-1 AIY score, the houseplant-specialist ViT classifier
     * (47 classes) is allowed to override AIY's guess — see
     * `mergeHouseplant()` in `HouseplantMerge.kt`.
     *
     * Placeholder pending real on-device calibration (tracked separately in
     * `HouseplantVitCalibrationTest`, an androidTest). It is deliberately set
     * higher than [LOW_CONFIDENCE] rather than reused: a 47-class softmax has
     * a much easier discrimination problem than AIY's 2,102-class one, so a
     * well-calibrated ViT model's "correct" top-1 confidence baseline is
     * expected to sit meaningfully higher than AIY's. Reusing 0.25f here
     * would let ViT override AIY on results that are barely more than a
     * random-ish guess out of 47 classes. 0.5f is a conservative middle
     * ground: high enough that we only trust ViT when it looks genuinely
     * confident, low enough to still kick in on the AIY-is-clearly-guessing
     * cases (AIY is weak on houseplants — the entire reason this classifier
     * exists) rather than defaulting to AIY on every borderline case.
     */
    const val HOUSEPLANT_LOW_CONFIDENCE: Float = 0.5f
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
