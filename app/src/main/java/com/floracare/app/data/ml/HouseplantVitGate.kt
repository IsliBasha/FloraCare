package com.floracare.app.data.ml

/**
 * Pure decision logic for whether [HouseplantAwareClassifier] should even
 * attempt the ViT path for a given AIY result, extracted so the cascade's
 * gating behaviour is JVM-testable without a real TFLite interpreter (see
 * `AiyCalibrationTest`, an androidTest, for why the interpreter itself can
 * only be verified on-device).
 *
 * Three outcomes:
 *  - [VitConsultDecision.SKIP_AIY_CONFIDENT]: AIY's own top confidence
 *    already clears [houseplantThreshold] — the ViT runner must not even be
 *    lazily initialised for this call, per the plan's latency reasoning
 *    (the ~86MB model should stay off the hot path whenever AIY alone is
 *    trustworthy).
 *  - [VitConsultDecision.SKIP_VIT_NOT_READY]: AIY is unsure, but the ViT
 *    interpreter isn't initialised yet (first call ever, or a still
 *    in-flight background load). The caller must fall back to AIY-only for
 *    *this* call and kick off (or let continue) the lazy init in the
 *    background — never block this call on the first ~86MB load.
 *  - [VitConsultDecision.CONSULT_VIT]: AIY is unsure and the ViT
 *    interpreter is already initialised — safe to run inference
 *    synchronously within this call.
 *
 * An AIY result with no predictions at all is treated as below threshold —
 * mirrors the same rule in [mergeHouseplant].
 */
internal enum class VitConsultDecision {
    SKIP_AIY_CONFIDENT,
    SKIP_VIT_NOT_READY,
    CONSULT_VIT,
}

internal fun decideVitConsult(
    aiyPredictions: List<Prediction>,
    isVitRunnerReady: Boolean,
    houseplantThreshold: Float,
): VitConsultDecision {
    val aiyTopConfidence = aiyPredictions.firstOrNull()?.confidence ?: 0f
    return when {
        aiyTopConfidence >= houseplantThreshold -> VitConsultDecision.SKIP_AIY_CONFIDENT
        !isVitRunnerReady -> VitConsultDecision.SKIP_VIT_NOT_READY
        else -> VitConsultDecision.CONSULT_VIT
    }
}
