package com.floracare.app.data.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HouseplantMergeTest {

    private val threshold = 0.5f

    private val aiyConfident = listOf(
        Prediction("Monstera deliciosa", 0.8f),
        Prediction("Philodendron bipinnatifidum", 0.1f),
    )

    private val aiyUnsure = listOf(
        Prediction("Ficus lyrata", 0.2f),
        Prediction("Ficus elastica", 0.1f),
    )

    // "Pothos (Ivy arum)" is a real label from HouseplantAliasMap, translates
    // to "Epipremnum aureum".
    private val vitTranslatable = listOf(
        Prediction("Pothos (Ivy arum)", 0.9f),
        Prediction("Snake plant (Sanseviera)", 0.05f),
    )

    // Not a real ViT label — guaranteed to be absent from HouseplantAliasMap.
    private val vitUntranslatable = listOf(
        Prediction("Not A Real Vit Label", 0.9f),
    )

    @Test
    fun `AIY confident and ViT not ready returns AIY result unchanged`() {
        val result = mergeHouseplant(
            aiyPredictions = aiyConfident,
            vitPredictions = vitTranslatable,
            vitReady = false,
            houseplantThreshold = threshold,
        )

        assertSame(aiyConfident, result)
    }

    @Test
    fun `AIY confident and ViT ready still returns AIY result, ViT ignored`() {
        val result = mergeHouseplant(
            aiyPredictions = aiyConfident,
            vitPredictions = vitTranslatable,
            vitReady = true,
            houseplantThreshold = threshold,
        )

        assertSame(aiyConfident, result)
    }

    @Test
    fun `AIY below threshold and ViT ready with translatable label returns ViT result as scientific names`() {
        val result = mergeHouseplant(
            aiyPredictions = aiyUnsure,
            vitPredictions = vitTranslatable,
            vitReady = true,
            houseplantThreshold = threshold,
        )

        assertEquals(2, result.size)
        assertEquals("Epipremnum aureum", result[0].label)
        assertEquals(0.9f, result[0].confidence, 0.0001f)
        assertEquals("Sansevieria", result[1].label)
    }

    @Test
    fun `AIY below threshold and ViT top label untranslatable falls back to AIY`() {
        val result = mergeHouseplant(
            aiyPredictions = aiyUnsure,
            vitPredictions = vitUntranslatable,
            vitReady = true,
            houseplantThreshold = threshold,
        )

        assertSame(aiyUnsure, result)
    }

    @Test
    fun `AIY below threshold and ViT not ready returns AIY result, no crash`() {
        val result = mergeHouseplant(
            aiyPredictions = aiyUnsure,
            vitPredictions = vitTranslatable,
            vitReady = false,
            houseplantThreshold = threshold,
        )

        assertSame(aiyUnsure, result)
    }

    @Test
    fun `AIY below threshold and ViT ready but empty returns AIY result, no crash`() {
        val result = mergeHouseplant(
            aiyPredictions = aiyUnsure,
            vitPredictions = emptyList(),
            vitReady = true,
            houseplantThreshold = threshold,
        )

        assertSame(aiyUnsure, result)
    }

    @Test
    fun `AIY confidence exactly at threshold counts as confident, AIY wins`() {
        val aiyAtThreshold = listOf(Prediction("Ficus lyrata", threshold))

        val result = mergeHouseplant(
            aiyPredictions = aiyAtThreshold,
            vitPredictions = vitTranslatable,
            vitReady = true,
            houseplantThreshold = threshold,
        )

        assertSame(aiyAtThreshold, result)
    }

    @Test
    fun `empty AIY predictions is treated as below threshold, ViT can still win`() {
        val result = mergeHouseplant(
            aiyPredictions = emptyList(),
            vitPredictions = vitTranslatable,
            vitReady = true,
            houseplantThreshold = threshold,
        )

        assertEquals("Epipremnum aureum", result.first().label)
    }

    @Test
    fun `empty AIY predictions with ViT not ready returns empty list, no crash`() {
        val result = mergeHouseplant(
            aiyPredictions = emptyList(),
            vitPredictions = emptyList(),
            vitReady = false,
            houseplantThreshold = threshold,
        )

        assertEquals(emptyList<Prediction>(), result)
    }
}
