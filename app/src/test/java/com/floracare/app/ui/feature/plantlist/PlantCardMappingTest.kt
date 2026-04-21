package com.floracare.app.ui.feature.plantlist

import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.CareTaskSource
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days

class PlantCardMappingTest {

    private val now = Instant.parse("2026-04-20T09:00:00Z")
    private val tz = TimeZone.UTC

    private fun plant(id: String = "pl-1", speciesId: String? = "sp-mon", nick: String = "Mona") =
        Plant(
            id = id,
            nickname = nick,
            speciesId = speciesId,
            locationTag = LocationTag.INDOOR,
            acquiredAt = now - 30.days,
            coverPhotoUri = null,
            notes = "",
        )

    private fun species(id: String = "sp-mon", common: String = "Swiss Cheese Plant") = Species(
        id = id,
        scientificName = "Monstera deliciosa",
        commonName = common,
        waterFrequencyDays = 7,
        lightNeed = LightNeed.MEDIUM,
        humidityNeed = HumidityNeed.MEDIUM,
        temperatureRangeC = 18f..28f,
        toxicity = Toxicity.MILD,
        careNotes = "",
    )

    private fun task(
        id: String,
        plantId: String,
        scheduledAt: Instant,
        type: CareTaskType = CareTaskType.WATER,
        completedAt: Instant? = null,
        snoozedUntil: Instant? = null,
    ) = CareTask(
        id = id,
        plantId = plantId,
        type = type,
        scheduledAt = scheduledAt,
        completedAt = completedAt,
        snoozedUntil = snoozedUntil,
        source = CareTaskSource.ADAPTIVE,
    )

    @Test
    fun `empty plants produce empty cards and zero dues`() {
        val result = toPlantCards(
            plants = emptyList(),
            species = emptyList(),
            openTasks = emptyList(),
            now = now,
            tz = tz,
        )
        assertEquals(emptyList<PlantCardUi>(), result.cards)
        assertEquals(0, result.duesToday)
    }

    @Test
    fun `plant without species shows Unknown species`() {
        val result = toPlantCards(
            plants = listOf(plant(speciesId = null)),
            species = emptyList(),
            openTasks = emptyList(),
            now = now,
            tz = tz,
        )
        assertEquals("Unknown species", result.cards.single().speciesName)
    }

    @Test
    fun `next task today labels water today and counts toward dues`() {
        val today = Instant.parse("2026-04-20T18:00:00Z")
        val result = toPlantCards(
            plants = listOf(plant()),
            species = listOf(species()),
            openTasks = listOf(task(id = "t1", plantId = "pl-1", scheduledAt = today)),
            now = now,
            tz = tz,
        )
        assertEquals("Water today", result.cards.single().nextTaskLabel)
        assertEquals(1, result.duesToday)
    }

    @Test
    fun `next task tomorrow labels tomorrow and does not count as due today`() {
        val result = toPlantCards(
            plants = listOf(plant()),
            species = listOf(species()),
            openTasks = listOf(task(id = "t", plantId = "pl-1", scheduledAt = now + 1.days)),
            now = now,
            tz = tz,
        )
        assertEquals("Water tomorrow", result.cards.single().nextTaskLabel)
        assertEquals(0, result.duesToday)
    }

    @Test
    fun `overdue task labels days ago`() {
        val overdue = task(id = "t", plantId = "pl-1", scheduledAt = now - 3.days)
        val result = toPlantCards(
            plants = listOf(plant()),
            species = listOf(species()),
            openTasks = listOf(overdue),
            now = now,
            tz = tz,
        )
        assertEquals("Water 3d ago", result.cards.single().nextTaskLabel)
    }

    @Test
    fun `snoozed future task is ignored so card shows no scheduled care`() {
        val snoozed = task(
            id = "t",
            plantId = "pl-1",
            scheduledAt = now,
            snoozedUntil = now + 2.days,
        )
        val result = toPlantCards(
            plants = listOf(plant()),
            species = listOf(species()),
            openTasks = listOf(snoozed),
            now = now,
            tz = tz,
        )
        assertEquals("No scheduled care", result.cards.single().nextTaskLabel)
        assertEquals(0, result.duesToday)
    }

    @Test
    fun `completed task is ignored`() {
        val completed = task(
            id = "t",
            plantId = "pl-1",
            scheduledAt = now,
            completedAt = now - 1.days,
        )
        val result = toPlantCards(
            plants = listOf(plant()),
            species = listOf(species()),
            openTasks = listOf(completed),
            now = now,
            tz = tz,
        )
        assertEquals("No scheduled care", result.cards.single().nextTaskLabel)
    }

    @Test
    fun `min scheduledAt wins when multiple open tasks exist`() {
        val soon = task(id = "a", plantId = "pl-1", scheduledAt = now + 1.days)
        val later = task(id = "b", plantId = "pl-1", scheduledAt = now + 5.days)
        val result = toPlantCards(
            plants = listOf(plant()),
            species = listOf(species()),
            openTasks = listOf(later, soon),
            now = now,
            tz = tz,
        )
        assertEquals("Water tomorrow", result.cards.single().nextTaskLabel)
    }

    @Test
    fun `accent is stable for the same species id`() {
        val a = accentFor("sp-monstera")
        val b = accentFor("sp-monstera")
        assertEquals(a, b)
    }

    @Test
    fun `accent differs across distinct species ids in the fixture`() {
        // Palette has 5 colours; three distinct ids should yield at least two hues.
        val colors = listOf("sp-mon", "sp-fig", "sp-sna").map { accentFor(it) }.toSet()
        assertTrue("expected >=2 distinct accents, got $colors", colors.size >= 2)
        // Null species fallback also returns a valid palette entry (not crashing).
        assertNotEquals(null, accentFor(null))
    }
}
