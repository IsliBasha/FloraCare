package com.floracare.app.data.ml

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.E

class LogitCalibratorTest {

    // ── Basic contract ──────────────────────────────────────────────────────

    @Test
    fun `softmax of single-element array returns 1`() {
        val result = softmax(floatArrayOf(0.5f))
        assertEquals(1, result.size)
        assertEquals(1f, result[0], 1e-4f)
    }

    @Test
    fun `softmax outputs sum to 1`() {
        val result = softmax(floatArrayOf(1f, 2f, 3f))
        assertEquals(1f, result.sum(), 1e-4f)
    }

    @Test
    fun `softmax preserves descending rank order`() {
        val result = softmax(floatArrayOf(3f, 1f, 2f))
        assertTrue("index 0 (logit=3) should dominate", result[0] > result[2])
        assertTrue("index 2 (logit=2) should beat index 1 (logit=1)", result[2] > result[1])
    }

    @Test
    fun `softmax of equal logits is uniform`() {
        val result = softmax(floatArrayOf(1f, 1f, 1f, 1f))
        val expected = 1f / 4f
        for (v in result) assertEquals(expected, v, 1e-4f)
    }

    @Test
    fun `empty logits returns empty array`() {
        assertEquals(0, softmax(floatArrayOf()).size)
    }

    // ── Temperature behaviour ───────────────────────────────────────────────

    @Test
    fun `temperature 1 matches standard softmax formula`() {
        val logits = floatArrayOf(1f, 2f, 3f)
        val result = softmax(logits, temperature = 1f)
        val e1 = E.toFloat()
        val e2 = (E * E).toFloat()
        val e3 = (E * E * E).toFloat()
        val sum = e1 + e2 + e3
        assertArrayEquals(floatArrayOf(e1 / sum, e2 / sum, e3 / sum), result, 1e-4f)
    }

    @Test
    fun `higher temperature flattens distribution`() {
        val logits = floatArrayOf(3f, 1f, 0.5f)
        val sharp = softmax(logits, temperature = 0.5f)
        val flat = softmax(logits, temperature = 3f)
        assertTrue("high temperature should reduce the top confidence", flat[0] < sharp[0])
    }

    @Test
    fun `lower temperature sharpens distribution toward argmax`() {
        val logits = floatArrayOf(5f, 2f, 1f)
        val result = softmax(logits, temperature = 0.1f)
        assertTrue("argmax class should dominate at low temperature", result[0] > 0.99f)
    }

    @Test
    fun `negative temperature throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            softmax(floatArrayOf(1f, 2f), temperature = -1f)
        }
    }

    @Test
    fun `zero temperature throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            softmax(floatArrayOf(1f, 2f), temperature = 0f)
        }
    }

    // ── Numerical stability ─────────────────────────────────────────────────

    @Test
    fun `softmax is stable with very large logits`() {
        val result = softmax(floatArrayOf(1000f, 999f, 998f))
        assertTrue("no NaN expected", result.none { it.isNaN() })
        assertTrue("no Inf expected", result.none { it.isInfinite() })
        assertEquals(1f, result.sum(), 1e-3f)
    }

    @Test
    fun `softmax is stable with very negative logits`() {
        val result = softmax(floatArrayOf(-1000f, -999f, -998f))
        assertTrue("no NaN expected", result.none { it.isNaN() })
        assertEquals(1f, result.sum(), 1e-3f)
    }
}
