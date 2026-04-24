package com.floracare.app.domain.usecase

import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.SpeciesDefaults
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.repository.PlantRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a classifier prediction label into a [Species] id that exists in the
 * database. If no species matches the predicted scientific name (case- and
 * whitespace-insensitive), a minimal placeholder row is inserted and its id is
 * returned.
 *
 * Idempotent: calling twice with the same label returns the same species id.
 */
@Singleton
class ResolveOrCreateSpeciesUseCase @Inject constructor(
    private val plants: PlantRepository,
) {

    /** Test seam — replace to inject a deterministic id in unit tests. */
    internal var idGenerator: () -> String = { "sp-user-${UUID.randomUUID()}" }

    suspend operator fun invoke(predictedLabel: String): String {
        val normalised = predictedLabel.trim()
        require(normalised.isNotEmpty()) { "Predicted label must not be blank" }

        plants.findSpeciesByScientificName(normalised)?.let { existing ->
            return existing.id
        }

        val synth = Species(
            id = idGenerator(),
            scientificName = normalised,
            commonName = normalised,
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
