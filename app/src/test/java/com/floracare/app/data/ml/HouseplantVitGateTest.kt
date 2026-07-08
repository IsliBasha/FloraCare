package com.floracare.app.data.ml

import org.junit.Assert.assertEquals
import org.junit.Test

class HouseplantVitGateTest {

    private val threshold = 0.5f

    private val aiyConfident = listOf(Prediction("Monstera deliciosa", 0.8f))
    private val aiyUnsure = listOf(Prediction("Ficus lyrata", 0.2f))

    @Test
    fun `AIY confident and ViT runner ready still skips ViT`() {
        val decision = decideVitConsult(
            aiyPredictions = aiyConfident,
            isVitRunnerReady = true,
            houseplantThreshold = threshold,
        )

        assertEquals(VitConsultDecision.SKIP_AIY_CONFIDENT, decision)
    }

    @Test
    fun `AIY confident and ViT runner not ready skips ViT without touching readiness`() {
        val decision = decideVitConsult(
            aiyPredictions = aiyConfident,
            isVitRunnerReady = false,
            houseplantThreshold = threshold,
        )

        assertEquals(VitConsultDecision.SKIP_AIY_CONFIDENT, decision)
    }

    @Test
    fun `AIY unsure and ViT runner ready consults ViT`() {
        val decision = decideVitConsult(
            aiyPredictions = aiyUnsure,
            isVitRunnerReady = true,
            houseplantThreshold = threshold,
        )

        assertEquals(VitConsultDecision.CONSULT_VIT, decision)
    }

    @Test
    fun `AIY unsure and ViT runner not ready falls back to AIY-only`() {
        val decision = decideVitConsult(
            aiyPredictions = aiyUnsure,
            isVitRunnerReady = false,
            houseplantThreshold = threshold,
        )

        assertEquals(VitConsultDecision.SKIP_VIT_NOT_READY, decision)
    }

    @Test
    fun `AIY confidence exactly at threshold counts as confident, skips ViT`() {
        val aiyAtThreshold = listOf(Prediction("Ficus lyrata", threshold))

        val decision = decideVitConsult(
            aiyPredictions = aiyAtThreshold,
            isVitRunnerReady = true,
            houseplantThreshold = threshold,
        )

        assertEquals(VitConsultDecision.SKIP_AIY_CONFIDENT, decision)
    }

    @Test
    fun `empty AIY predictions is treated as below threshold, consults ViT when ready`() {
        val decision = decideVitConsult(
            aiyPredictions = emptyList(),
            isVitRunnerReady = true,
            houseplantThreshold = threshold,
        )

        assertEquals(VitConsultDecision.CONSULT_VIT, decision)
    }

    @Test
    fun `empty AIY predictions with ViT not ready falls back to AIY-only`() {
        val decision = decideVitConsult(
            aiyPredictions = emptyList(),
            isVitRunnerReady = false,
            houseplantThreshold = threshold,
        )

        assertEquals(VitConsultDecision.SKIP_VIT_NOT_READY, decision)
    }
}
