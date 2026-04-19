package com.floracare.app.data.ml

/**
 * Converts a raw probability vector and label list into the top-K [Prediction]s
 * sorted by descending confidence. Defensively trims to the minimum of
 * `logits.size` and `labels.size` so a mismatch between a model's output tensor
 * and the shipped labels file can never crash inference.
 */
fun parseTopK(logits: FloatArray, labels: List<String>, k: Int): List<Prediction> {
    if (k <= 0) return emptyList()
    val usable = minOf(logits.size, labels.size)
    if (usable == 0) return emptyList()
    val effectiveK = minOf(k, usable)

    return (0 until usable)
        .asSequence()
        .map { i -> Prediction(labels[i], logits[i]) }
        .sortedByDescending { it.confidence }
        .take(effectiveK)
        .toList()
}
