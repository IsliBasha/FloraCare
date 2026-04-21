package com.floracare.app.data.notification

import com.floracare.app.CareChannel
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationContentTest {

    private val plant = Plant(
        id = "pl-1",
        nickname = "Mona",
        speciesId = "sp-monstera",
        locationTag = LocationTag.INDOOR,
        acquiredAt = Instant.parse("2026-01-01T00:00:00Z"),
        coverPhotoUri = null,
        notes = "",
    )

    private val species = Species(
        id = "sp-monstera",
        scientificName = "Monstera deliciosa",
        commonName = "Swiss Cheese Plant",
        waterFrequencyDays = 7,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.MEDIUM,
        temperatureRangeC = 18f..28f,
        toxicity = Toxicity.MILD,
        careNotes = "",
    )

    @Test
    fun `water task routes to WATER channel with water verb`() {
        val content = buildNotificationContent(plant, species, CareTaskType.WATER)
        assertEquals(CareChannel.WATER, content.channel)
        assertEquals("Water Mona", content.title)
        assertTrue(content.body.contains("Mona"))
        assertTrue(content.body.contains("Swiss Cheese Plant"))
    }

    @Test
    fun `fertilize task routes to FERTILIZE channel`() {
        val content = buildNotificationContent(plant, species, CareTaskType.FERTILIZE)
        assertEquals(CareChannel.FERTILIZE, content.channel)
        assertEquals("Fertilize Mona", content.title)
    }

    @Test
    fun `mist rotate repot prune all route to OTHER channel`() {
        val secondary = listOf(
            CareTaskType.MIST, CareTaskType.ROTATE, CareTaskType.REPOT, CareTaskType.PRUNE,
        )
        secondary.forEach { type ->
            val content = buildNotificationContent(plant, species, type)
            assertEquals(
                "expected OTHER channel for $type",
                CareChannel.OTHER,
                content.channel,
            )
        }
    }

    @Test
    fun `missing species falls back to nickname only in body`() {
        val content = buildNotificationContent(plant, species = null, CareTaskType.WATER)
        assertEquals("Time to Water Mona.", content.body)
    }

    @Test
    fun `blank species common name does not leak parentheses`() {
        val blank = species.copy(commonName = "")
        val content = buildNotificationContent(plant, blank, CareTaskType.WATER)
        assertTrue(
            "body should not contain empty parens, got: ${content.body}",
            !content.body.contains("()"),
        )
    }
}
