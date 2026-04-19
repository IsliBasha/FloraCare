package com.floracare.app.data.ml

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
}
