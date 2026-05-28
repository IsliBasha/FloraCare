package com.floracare.app.data.ml

import kotlin.math.exp

/**
 * Temperature-scaled softmax that converts raw model logits into a proper
 * probability distribution.
 *
 * Standard softmax (`T = 1`) maps logits to probabilities but preserves
 * the raw sharpness of the model's output, which is often over-confident
 * on in-distribution inputs. A temperature `T > 1` divides the logits
 * before exponentiating, compressing the gap between classes so the
 * resulting probabilities are more spread out — closer to the model's
 * actual empirical accuracy on held-out data.
 *
 * T = 1.5f is a reasonable default for MobileNet-based plant classifiers
 * fine-tuned on PlantVillage, where the top-1 probability is often ~0.9+
 * but the true accuracy on real-world photos is 60–75%.
 *
 * The implementation subtracts the maximum logit before exponentiating
 * (log-sum-exp trick) so values like `exp(1000f)` never overflow to Inf.
 *
 * @param logits raw model output (float32, any range)
 * @param temperature positive scalar; > 1 softens, < 1 sharpens, 1 = standard
 * @return probability vector of the same length, summing to 1
 * @throws IllegalArgumentException if temperature ≤ 0
 */
fun softmax(logits: FloatArray, temperature: Float = 1.5f): FloatArray {
    require(temperature > 0f) { "temperature must be positive, got $temperature" }
    if (logits.isEmpty()) return floatArrayOf()

    val max = logits.maxOrNull() ?: 0f
    val exps = FloatArray(logits.size) { i ->
        exp(((logits[i] - max) / temperature).toDouble()).toFloat()
    }
    val sum = exps.sum()
    return FloatArray(logits.size) { i -> exps[i] / sum }
}
