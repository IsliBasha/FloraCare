package com.floracare.app.data.ml

import android.graphics.Bitmap

/**
 * Converts an `IntArray` of ARGB pixels to a flat float RGB buffer in `[0, 1]`
 * suitable as input to a float32 TFLite image classifier. Output layout is
 * interleaved RGB: `[r0, g0, b0, r1, g1, b1, …]`. Alpha is discarded.
 */
internal fun normalizePixels(pixels: IntArray, expectedPixelCount: Int): FloatArray {
    require(pixels.size == expectedPixelCount) {
        "pixel count mismatch: got ${pixels.size}, expected $expectedPixelCount"
    }
    val out = FloatArray(expectedPixelCount * 3)
    for (i in 0 until expectedPixelCount) {
        val argb = pixels[i]
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val base = i * 3
        out[base] = r / 255f
        out[base + 1] = g / 255f
        out[base + 2] = b / 255f
    }
    return out
}

/**
 * Applies ImageNet channel-wise mean/std normalisation to a flat interleaved
 * RGB float array in `[0, 1]` (as produced by [normalizePixels]).
 *
 * Formula per channel:
 * ```
 * R_norm = (R − 0.485) / 0.229
 * G_norm = (G − 0.456) / 0.224
 * B_norm = (B − 0.406) / 0.225
 * ```
 *
 * These constants are the per-channel mean and standard deviation of the
 * ImageNet training set, and are the expected preprocessing for **all**
 * models fine-tuned from MobileNet, EfficientNet, or ResNet backbones.
 *
 * **Only call this for float32 model inputs.** Uint8 quantised models
 * handle calibration via their own scale/zero-point parameters.
 */
fun FloatArray.applyImageNetNorm(): FloatArray {
    val out = FloatArray(size)
    var i = 0
    while (i < size) {
        out[i]     = (this[i]     - 0.485f) / 0.229f
        out[i + 1] = (this[i + 1] - 0.456f) / 0.224f
        out[i + 2] = (this[i + 2] - 0.406f) / 0.225f
        i += 3
    }
    return out
}

/**
 * Bilinear-resize a bitmap to a square `targetSize x targetSize` and return a
 * float32 RGB buffer normalised to `[0, 1]`. Caller is responsible for the
 * underlying bitmap lifecycle; the scaled intermediate is created and released
 * inside this function.
 */
fun Bitmap.preprocessForModel(targetSize: Int = 224): FloatArray {
    val scaled = if (width == targetSize && height == targetSize) {
        this
    } else {
        Bitmap.createScaledBitmap(this, targetSize, targetSize, true)
    }
    val pixels = IntArray(targetSize * targetSize)
    scaled.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)
    if (scaled !== this) scaled.recycle()
    return normalizePixels(pixels, targetSize * targetSize)
}
