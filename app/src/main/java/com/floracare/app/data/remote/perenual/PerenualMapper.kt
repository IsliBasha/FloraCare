package com.floracare.app.data.remote.perenual

import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.SpeciesDefaults
import com.floracare.app.domain.model.Toxicity
import kotlinx.datetime.Instant

/**
 * Maps a Perenual details payload into our domain [Species]. Missing fields
 * fall back to [SpeciesDefaults] so the resulting row is always usable by the
 * adaptive care engine.
 */
internal object PerenualMapper {

    fun toDomain(dto: SpeciesDetailsResponse, fetchedAt: Instant): Species {
        val scientific = dto.scientificName.firstOrNull()?.trim()
            ?: dto.commonName?.trim()
            ?: "Unknown species"
        val common = dto.commonName?.trim()?.takeIf { it.isNotEmpty() } ?: scientific

        return Species(
            id = stableIdFor(dto.id),
            scientificName = scientific,
            commonName = common,
            waterFrequencyDays = wateringToDays(dto.watering),
            lightNeed = sunlightToLight(dto.sunlight, careLevel = dto.careLevel),
            humidityNeed = humidityFor(dto.watering, dto.family),
            temperatureRangeC = hardinessToRange(dto.hardiness),
            toxicity = toxicityFrom(dto.poisonousToHumans, dto.poisonousToPets),
            careNotes = careNotesFor(dto),
            provider = Species.PROVIDER_PERENUAL,
            providerSpeciesId = dto.id.toString(),
            fetchedAt = fetchedAt,
            family = dto.family,
            genus = dto.genus,
            imageUrl = dto.image?.mediumUrl ?: dto.image?.thumbnail,
        )
    }

    fun stableIdFor(providerId: Long): String = "sp-perenual-$providerId"

    private fun wateringToDays(watering: String?): Int = when (watering?.lowercase()) {
        "frequent" -> 3
        "average" -> 7
        "minimum" -> 14
        "none" -> 21
        else -> SpeciesDefaults.WATER_FREQUENCY_DAYS
    }

    private fun sunlightToLight(sunlight: List<String>, careLevel: String?): LightNeed {
        if (sunlight.isEmpty()) return LightNeed.MEDIUM
        val brightest = sunlight.mapNotNull { sunlightOrdinal(it) }.maxOrNull() ?: return LightNeed.MEDIUM
        return when (brightest) {
            0 -> LightNeed.LOW
            1 -> LightNeed.MEDIUM
            2 -> if (careLevel?.equals("easy", ignoreCase = true) == true) {
                LightNeed.HIGH
            } else {
                LightNeed.DIRECT_SUN
            }
            else -> LightNeed.MEDIUM
        }
    }

    private fun sunlightOrdinal(token: String): Int? = when (token.lowercase().trim()) {
        "deep_shade", "full_shade" -> 0
        "part_shade", "filtered_shade", "sun-part_shade", "partial_shade" -> 1
        "full_sun" -> 2
        else -> null
    }

    private fun humidityFor(watering: String?, family: String?): HumidityNeed = when {
        watering?.equals("Frequent", ignoreCase = true) == true -> HumidityNeed.HIGH
        family?.let { it in HIGH_HUMIDITY_FAMILIES } == true -> HumidityNeed.HIGH
        watering?.lowercase() in setOf("minimum", "none") -> HumidityNeed.LOW
        else -> HumidityNeed.MEDIUM
    }

    private fun hardinessToRange(hardiness: Hardiness?): ClosedFloatingPointRange<Float> {
        val min = hardiness?.min?.let(USDA_MIN_C::get)?.coerceAtLeast(INDOOR_MIN_C)
            ?: SpeciesDefaults.MIN_TEMP_C
        val max = when (hardiness?.max) {
            "11", "12", "13" -> WARM_MAX_C
            else -> SpeciesDefaults.MAX_TEMP_C
        }
        // Guard against inverted ranges when both min and max end up at the floor.
        val safeMin = min.coerceAtMost(max - 1f)
        return safeMin..max
    }

    private fun toxicityFrom(humans: Int?, pets: Int?): Toxicity =
        if (humans == 1 || pets == 1) Toxicity.TOXIC else Toxicity.NONE

    private fun careNotesFor(dto: SpeciesDetailsResponse): String {
        val stripped = dto.description?.let(::stripHtml)?.trim().orEmpty()
        val truncated = if (stripped.length > DESCRIPTION_MAX) {
            stripped.take(DESCRIPTION_MAX).trimEnd() + "…"
        } else {
            stripped
        }
        val toxicityNote = when {
            dto.poisonousToHumans == null && dto.poisonousToPets == null ->
                "Toxicity unknown — research before exposing to pets or children."
            else -> ""
        }
        val levelNote = dto.careLevel?.takeIf { it.isNotBlank() }?.let { "Care level: $it." }.orEmpty()
        return listOf(truncated, toxicityNote, levelNote)
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n\n")
    }

    private fun stripHtml(raw: String): String = raw.replace(HTML_TAG, "")

    private val HTML_TAG = Regex("<[^>]+>")

    private val HIGH_HUMIDITY_FAMILIES = setOf("Araceae", "Marantaceae", "Gesneriaceae")

    private val USDA_MIN_C: Map<String, Float> = mapOf(
        "1" to -51f, "2" to -45f, "3" to -40f, "4" to -34f, "5" to -29f,
        "6" to -23f, "7" to -18f, "8" to -12f, "9" to -7f, "10" to -1f,
        "11" to 4f, "12" to 10f, "13" to 16f,
    )

    private const val INDOOR_MIN_C: Float = 10f
    private const val WARM_MAX_C: Float = 32f
    private const val DESCRIPTION_MAX: Int = 500
}
