package com.floracare.app.data.ml

/**
 * Converts a raw probability vector and label list into the top-K [Prediction]s
 * sorted by descending confidence. Defensively trims to the minimum of
 * `logits.size` and `labels.size` so a mismatch between a model's output tensor
 * and the shipped labels file can never crash inference.
 *
 * Filler labels (blank entries produced by index-based label parsing) and the
 * AIY catch-all "background" class are excluded — they're never useful in the
 * user-facing picker — so callers still receive up to [k] real species even
 * when `background` would otherwise dominate the top.
 */
fun parseTopK(logits: FloatArray, labels: List<String>, k: Int): List<Prediction> {
    if (k <= 0) return emptyList()
    val usable = minOf(logits.size, labels.size)
    if (usable == 0) return emptyList()

    return (0 until usable)
        .asSequence()
        .filter { i -> labels[i].isNotBlank() && !labels[i].equals("background", ignoreCase = true) }
        .map { i -> Prediction(labels[i], logits[i]) }
        .sortedByDescending { it.confidence }
        .take(k)
        .toList()
}
