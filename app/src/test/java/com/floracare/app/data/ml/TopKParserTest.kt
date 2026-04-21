package com.floracare.app.data.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopKParserTest {

    private val labels = listOf("Monstera", "Pothos", "Philodendron", "Fiddle-leaf")

    @Test
    fun `returns k predictions sorted by confidence descending`() {
        val logits = floatArrayOf(0.1f, 0.7f, 0.15f, 0.05f)

        val top3 = parseTopK(logits, labels, k = 3)

        assertEquals(3, top3.size)
        assertEquals("Pothos", top3[0].label)
        assertEquals("Philodendron", top3[1].label)
        assertEquals("Monstera", top3[2].label)
        assertTrue(top3[0].confidence > top3[1].confidence)
        assertTrue(top3[1].confidence > top3[2].confidence)
    }

    @Test
    fun `maps highest index to correct label`() {
        val logits = floatArrayOf(0.0f, 0.0f, 0.0f, 0.99f)

        val top1 = parseTopK(logits, labels, k = 1)

        assertEquals(1, top1.size)
        assertEquals("Fiddle-leaf", top1[0].label)
        assertEquals(0.99f, top1[0].confidence, 0.0001f)
    }

    @Test
    fun `clamps k to labels size when k exceeds available labels`() {
        val logits = floatArrayOf(0.4f, 0.3f, 0.2f, 0.1f)

        val result = parseTopK(logits, labels, k = 99)

        assertEquals(labels.size, result.size)
    }

    @Test
    fun `returns empty list when k is zero or negative`() {
        val logits = floatArrayOf(0.4f, 0.3f)
        val smallLabels = listOf("A", "B")

        assertEquals(0, parseTopK(logits, smallLabels, k = 0).size)
        assertEquals(0, parseTopK(logits, smallLabels, k = -3).size)
    }

    @Test
    fun `uses minimum of logits and labels length on size mismatch`() {
        // More logits than labels — must not index out of bounds.
        val logits = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
        val shortLabels = listOf("A", "B")

        val result = parseTopK(logits, shortLabels, k = 5)

        assertEquals(2, result.size)
        assertTrue(result.all { it.label == "A" || it.label == "B" })
    }

    @Test
    fun `background class is filtered from results`() {
        val labelsWithBg = listOf("Monstera", "Pothos", "background", "Fiddle-leaf")
        val logits = floatArrayOf(0.1f, 0.2f, 0.6f, 0.1f) // background would win

        val top3 = parseTopK(logits, labelsWithBg, k = 3)

        assertEquals(3, top3.size)
        assertTrue("background must not appear", top3.none { it.label == "background" })
        assertEquals("Pothos", top3[0].label)
    }

    @Test
    fun `blank filler labels are skipped so k is honoured`() {
        val labelsWithGap = listOf("Monstera", "", "Pothos", "Fiddle-leaf")
        val logits = floatArrayOf(0.1f, 0.5f, 0.2f, 0.1f)

        val top2 = parseTopK(logits, labelsWithGap, k = 2)

        assertEquals(2, top2.size)
        assertTrue(top2.none { it.label.isBlank() })
    }
}
