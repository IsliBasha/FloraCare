package com.floracare.app.data.remote.perenual

import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.SpeciesDefaults
import com.floracare.app.domain.model.Toxicity
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerenualMapperTest {

    private val fetchedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    private fun details(
        id: Long = 1234L,
        commonName: String? = "Monstera",
        scientificName: List<String> = listOf("Monstera deliciosa"),
        family: String? = "Araceae",
        genus: String? = "Monstera",
        watering: String? = "Average",
        sunlight: List<String> = listOf("part_shade"),
        hardiness: Hardiness? = Hardiness(min = "10", max = "12"),
        poisonousToHumans: Int? = 0,
        poisonousToPets: Int? = 1,
        careLevel: String? = "Easy",
        description: String? = "<p>Tropical aroid with split leaves.</p>",
        image: PerenualImage? = PerenualImage(thumbnail = "thumb", mediumUrl = "medium"),
    ) = SpeciesDetailsResponse(
        id = id,
        commonName = commonName,
        scientificName = scientificName,
        family = family,
        genus = genus,
        watering = watering,
        sunlight = sunlight,
        hardiness = hardiness,
        poisonousToHumans = poisonousToHumans,
        poisonousToPets = poisonousToPets,
        careLevel = careLevel,
        description = description,
        image = image,
    )

    @Test
    fun `watering category maps to the configured day count`() {
        val frequent = PerenualMapper.toDomain(details(watering = "Frequent"), fetchedAt)
        val average = PerenualMapper.toDomain(details(watering = "Average"), fetchedAt)
        val minimum = PerenualMapper.toDomain(details(watering = "Minimum"), fetchedAt)
        val none = PerenualMapper.toDomain(details(watering = "None"), fetchedAt)
        val missing = PerenualMapper.toDomain(details(watering = null), fetchedAt)

        assertEquals(3, frequent.waterFrequencyDays)
        assertEquals(7, average.waterFrequencyDays)
        assertEquals(14, minimum.waterFrequencyDays)
        assertEquals(21, none.waterFrequencyDays)
        assertEquals(SpeciesDefaults.WATER_FREQUENCY_DAYS, missing.waterFrequencyDays)
    }

    @Test
    fun `sunlight prefers the brightest token and escalates to direct sun`() {
        val shade = PerenualMapper.toDomain(details(sunlight = listOf("full_shade")), fetchedAt)
        val partial = PerenualMapper.toDomain(details(sunlight = listOf("part_shade")), fetchedAt)
        val easyFullSun = PerenualMapper.toDomain(
            details(sunlight = listOf("full_shade", "full_sun"), careLevel = "Easy"),
            fetchedAt,
        )
        val hardFullSun = PerenualMapper.toDomain(
            details(sunlight = listOf("full_sun"), careLevel = "Difficult"),
            fetchedAt,
        )

        assertEquals(LightNeed.LOW, shade.lightNeed)
        assertEquals(LightNeed.MEDIUM, partial.lightNeed)
        assertEquals(LightNeed.HIGH, easyFullSun.lightNeed)
        assertEquals(LightNeed.DIRECT_SUN, hardFullSun.lightNeed)
    }

    @Test
    fun `Araceae family forces HIGH humidity even on Average watering`() {
        val result = PerenualMapper.toDomain(details(watering = "Average", family = "Araceae"), fetchedAt)
        assertEquals(HumidityNeed.HIGH, result.humidityNeed)
    }

    @Test
    fun `hardiness min zone maps to USDA celsius floor and clamps indoor at 10C`() {
        val cold = PerenualMapper.toDomain(details(hardiness = Hardiness("3", "8")), fetchedAt)
        // USDA 3 = -40C; clamped to the 10C indoor floor.
        assertEquals(10f, cold.temperatureRangeC.start)
        assertEquals(SpeciesDefaults.MAX_TEMP_C, cold.temperatureRangeC.endInclusive)

        val tropical = PerenualMapper.toDomain(details(hardiness = Hardiness("12", "13")), fetchedAt)
        // USDA 12 = 10C; warm zones raise the max.
        assertEquals(10f, tropical.temperatureRangeC.start)
        assertEquals(32f, tropical.temperatureRangeC.endInclusive)
    }

    @Test
    fun `any poisonous flag escalates to TOXIC otherwise NONE`() {
        val petToxic = PerenualMapper.toDomain(details(poisonousToHumans = 0, poisonousToPets = 1), fetchedAt)
        val safe = PerenualMapper.toDomain(details(poisonousToHumans = 0, poisonousToPets = 0), fetchedAt)
        val unknown = PerenualMapper.toDomain(details(poisonousToHumans = null, poisonousToPets = null), fetchedAt)

        assertEquals(Toxicity.TOXIC, petToxic.toxicity)
        assertEquals(Toxicity.NONE, safe.toxicity)
        assertEquals("null toxicity defaults to NONE per D1", Toxicity.NONE, unknown.toxicity)
        assertTrue(
            "unknown toxicity is disclosed in care notes",
            unknown.careNotes.contains("Toxicity unknown", ignoreCase = true),
        )
    }

    @Test
    fun `care notes strip HTML tags and truncate long descriptions`() {
        val longText = "A".repeat(600)
        val html = "<p>$longText</p><br/>"
        val result = PerenualMapper.toDomain(details(description = html), fetchedAt)

        assertFalse("HTML tags must be stripped", result.careNotes.contains("<"))
        assertTrue(
            "description is truncated with ellipsis",
            result.careNotes.trim().startsWith("AAAA") && result.careNotes.contains("…"),
        )
    }

    @Test
    fun `id is stable across calls and carries provider metadata`() {
        val a = PerenualMapper.toDomain(details(id = 42L), fetchedAt)
        val b = PerenualMapper.toDomain(details(id = 42L, description = "different"), fetchedAt)

        assertEquals(a.id, b.id)
        assertEquals("sp-perenual-42", a.id)
        assertEquals(Species.PROVIDER_PERENUAL, a.provider)
        assertEquals("42", a.providerSpeciesId)
        assertEquals(fetchedAt, a.fetchedAt)
    }
}
