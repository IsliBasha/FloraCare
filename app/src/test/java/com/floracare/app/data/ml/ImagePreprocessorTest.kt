package com.floracare.app.data.ml

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePreprocessorTest {

    @Test
    fun `normalizePixels returns flat RGB array of size width times height times 3`() {
        val pixels = IntArray(2 * 2) { 0xFF808080.toInt() }
        val out = normalizePixels(pixels, expectedPixelCount = 4)
        assertEquals(4 * 3, out.size)
    }

    @Test
    fun `normalizePixels maps white pixel to 1-1-1`() {
        val pixels = intArrayOf(0xFFFFFFFF.toInt())
        val out = normalizePixels(pixels, expectedPixelCount = 1)
        assertArrayEquals(floatArrayOf(1f, 1f, 1f), out, 0.0001f)
    }

    @Test
    fun `normalizePixels maps black pixel to 0-0-0`() {
        val pixels = intArrayOf(0xFF000000.toInt())
        val out = normalizePixels(pixels, expectedPixelCount = 1)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), out, 0.0001f)
    }

    @Test
    fun `normalizePixels extracts RGB channels in correct order ignoring alpha`() {
        // Color = ARGB(0x12, 0x40, 0x80, 0xC0). Alpha must not leak into output.
        val pixels = intArrayOf(0x124080C0)
        val out = normalizePixels(pixels, expectedPixelCount = 1)
        assertArrayEquals(
            floatArrayOf(0x40 / 255f, 0x80 / 255f, 0xC0 / 255f),
            out,
            0.0001f,
        )
    }

    @Test
    fun `normalizePixels throws when pixel count does not match expected`() {
        val pixels = IntArray(3) { 0 }
        assertThrows(IllegalArgumentException::class.java) {
            normalizePixels(pixels, expectedPixelCount = 4)
        }
    }

    // ── applyImageNetNorm ───────────────────────────────────────────────────

    @Test
    fun `applyImageNetNorm on white pixel produces correct positive values`() {
        // White pixel after [0,1] norm: R=1, G=1, B=1
        val input = floatArrayOf(1f, 1f, 1f)
        val out = input.applyImageNetNorm()
        assertEquals((1f - 0.485f) / 0.229f, out[0], 1e-4f)
        assertEquals((1f - 0.456f) / 0.224f, out[1], 1e-4f)
        assertEquals((1f - 0.406f) / 0.225f, out[2], 1e-4f)
    }

    @Test
    fun `applyImageNetNorm on black pixel produces correct negative values`() {
        val input = floatArrayOf(0f, 0f, 0f)
        val out = input.applyImageNetNorm()
        assertEquals(-0.485f / 0.229f, out[0], 1e-4f)
        assertEquals(-0.456f / 0.224f, out[1], 1e-4f)
        assertEquals(-0.406f / 0.225f, out[2], 1e-4f)
    }

    @Test
    fun `applyImageNetNorm preserves array length`() {
        val input = FloatArray(224 * 224 * 3) { 0.5f }
        val out = input.applyImageNetNorm()
        assertEquals(input.size, out.size)
    }

    @Test
    fun `applyImageNetNorm does not mutate the original array`() {
        val input = floatArrayOf(0.5f, 0.5f, 0.5f)
        input.applyImageNetNorm()
        assertArrayEquals(floatArrayOf(0.5f, 0.5f, 0.5f), input, 1e-4f)
    }

    @Test
    fun `applyImageNetNorm applies different means per channel`() {
        // Feed 0.5 to all channels — each shifts by a different ImageNet mean
        val input = floatArrayOf(0.5f, 0.5f, 0.5f)
        val out = input.applyImageNetNorm()
        // R mean=0.485, G mean=0.456, B mean=0.406 → all differ
        assertTrue(out[0] != out[1] || out[1] != out[2])
    }
}
