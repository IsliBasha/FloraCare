package com.floracare.app.domain.usecase

import com.floracare.app.domain.model.ConfidenceThresholds
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.SpeciesDefaults
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.domain.repository.SpeciesLookupResult
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a classifier prediction into a persisted [Species] id.
 *
 * Resolution order:
 *  1. Cache hit on scientific name → existing id.
 *  2. Low-confidence pick → ask [SpeciesLookupUseCase] to fetch Perenual.
 *     Fresh/Stale results already live in the cache, so their ids are returned.
 *  3. Fall through to a local synth row (existing behaviour).
 *
 * Idempotent: calling twice with the same label returns the same id. Perenual
 * rows use a stable `sp-perenual-${providerSpeciesId}` id, so repeated lookups
 * converge on the cache after the first fetch.
 */
@Singleton
class ResolveOrCreateSpeciesUseCase @Inject constructor(
    private val plants: PlantRepository,
    private val lookup: SpeciesLookupUseCase,
) {

    /** Test seam — replace to inject a deterministic id in unit tests. */
    internal var idGenerator: () -> String = { "sp-user-${UUID.randomUUID()}" }

    suspend operator fun invoke(
        predictedLabel: String,
        topConfidence: Float,
        commonNameHint: String? = null,
        forceEnrich: Boolean = false,
    ): String {
        val normalised = predictedLabel.trim()
        require(normalised.isNotEmpty()) { "Predicted label must not be blank" }

        plants.findSpeciesByScientificName(normalised)?.let { existing ->
            return existing.id
        }

        if (forceEnrich || topConfidence < ConfidenceThresholds.LOW_CONFIDENCE) {
            when (val remote = lookup(normalised, commonNameHint)) {
                is SpeciesLookupResult.Fresh -> return remote.species.id
                is SpeciesLookupResult.Stale -> return remote.species.id
                SpeciesLookupResult.NotFound,
                is SpeciesLookupResult.Offline -> Unit
            }
        }

        val synth = Species(
            id = idGenerator(),
            scientificName = normalised,
            commonName = commonNameHint?.trim()?.takeIf { it.isNotEmpty() } ?: normalised,
            waterFrequencyDays = SpeciesDefaults.WATER_FREQUENCY_DAYS,
            lightNeed = LightNeed.MEDIUM,
            humidityNeed = HumidityNeed.MEDIUM,
            temperatureRangeC = SpeciesDefaults.MIN_TEMP_C..SpeciesDefaults.MAX_TEMP_C,
            toxicity = Toxicity.NONE,
            careNotes = "",
        )
        plants.upsertSpecies(synth)
        return synth.id
    }
}
