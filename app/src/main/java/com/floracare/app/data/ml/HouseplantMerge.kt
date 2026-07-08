package com.floracare.app.data.ml

/**
 * Cascade/router merge between the general-purpose AIY classifier and the
 * houseplant-specialist ViT classifier, per the architecture in
 * `floracare-houseplant-classifier.md`.
 *
 * AIY's result is the default — it is returned unchanged unless *all* of the
 * following hold:
 *  1. `vitReady` is true and `vitPredictions` is non-empty (the ~86MB ViT
 *     model may still be loading, or absent/failed, and must never crash or
 *     silently inject a fabricated result — see bug #4 in the plan doc).
 *  2. AIY's own top confidence is below [houseplantThreshold]. An AIY result
 *     with no predictions at all is treated as below threshold too, so ViT
 *     can still fill in when AIY comes back completely empty.
 *  3. The ViT top prediction's label is translatable through
 *     [HouseplantAliasMap]. If it isn't, the merge falls back to AIY rather
 *     than emitting a raw, untranslatable ViT label — the resolver downstream
 *     resolves species by scientific name, not by ViT label text.
 *
 * When the ViT result wins, every entry is translated through
 * [HouseplantAliasMap] (entries that fail to translate are dropped, not
 * emitted raw) so the merged output is consistently scientific names
 * regardless of which model won.
 *
 * [houseplantThreshold] is intentionally a parameter rather than a hardcoded
 * constant so this function stays pure and JVM-testable independent of
 * [com.floracare.app.domain.model.ConfidenceThresholds].
 */
fun mergeHouseplant(
    aiyPredictions: List<Prediction>,
    vitPredictions: List<Prediction>,
    vitReady: Boolean,
    houseplantThreshold: Float,
): List<Prediction> {
    val aiyTopConfidence = aiyPredictions.firstOrNull()?.confidence ?: 0f
    val aiyIsConfident = aiyTopConfidence >= houseplantThreshold

    if (!vitReady || vitPredictions.isEmpty() || aiyIsConfident) {
        return aiyPredictions
    }

    val vitTopLabel = vitPredictions.first().label
    HouseplantAliasMap.scientificNameFor(vitTopLabel) ?: return aiyPredictions

    return vitPredictions.mapNotNull { prediction ->
        HouseplantAliasMap.scientificNameFor(prediction.label)?.let { scientificName ->
            prediction.copy(label = scientificName)
        }
    }
}
