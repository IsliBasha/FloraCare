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
