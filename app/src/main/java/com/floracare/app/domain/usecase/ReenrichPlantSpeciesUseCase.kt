package com.floracare.app.domain.usecase

import com.floracare.app.domain.model.Species
import com.floracare.app.domain.repository.PlantRepository
import com.floracare.app.domain.repository.SpeciesLookupResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of attempting to re-enrich a plant whose species came from the
 * local synth path (PROVIDER_LOCAL). One value per terminal branch — keeps
 * tests + observability explicit.
 */
sealed interface ReenrichOutcome {
    /** Plant lookup failed — no row with the given id. */
    data object NoPlant : ReenrichOutcome

    /** Plant exists but has no species attached. */
    data object NoSpecies : ReenrichOutcome

    /** Species is not local — no work needed. */
    data object AlreadyEnriched : ReenrichOutcome

    /** Species exists but its `scientificName` is blank — nothing to query. */
    data object SkippedBlank : ReenrichOutcome

    /** Remote lookup found no match. */
    data object NotFound : ReenrichOutcome

    /** Remote unreachable, no usable cache. */
    data object Offline : ReenrichOutcome

    /** Plant.speciesId now points to the upgraded (Perenual) species. */
    data class Upgraded(val newSpeciesId: String) : ReenrichOutcome
}

/**
 * Re-attempts a Perenual lookup for plants whose species was created via
 * the local synth path (because Perenual was unreachable, rate-limited,
 * or the AI was confident enough to skip enrichment at save time).
 *
 * Idempotent on its own — a plant whose species is already Perenual-backed
 * short-circuits to [ReenrichOutcome.AlreadyEnriched] without a network
 * call. Callers (e.g. PlantDetailViewModel.init) can fire-and-forget this
 * on every screen open without dragging the OWM/Perenual budget.
 */
@Singleton
class ReenrichPlantSpeciesUseCase @Inject constructor(
    private val plants: PlantRepository,
    private val lookup: SpeciesLookupUseCase,
) {
    suspend operator fun invoke(plantId: String): ReenrichOutcome {
        val plant = plants.findPlant(plantId) ?: return ReenrichOutcome.NoPlant
        val speciesId = plant.speciesId ?: return ReenrichOutcome.NoSpecies
        val current = plants.findSpecies(speciesId) ?: return ReenrichOutcome.NoSpecies
        if (current.provider != Species.PROVIDER_LOCAL) return ReenrichOutcome.AlreadyEnriched

        val name = current.scientificName.trim()
        if (name.isEmpty()) return ReenrichOutcome.SkippedBlank
        val hint = current.commonName.takeIf { it.isNotBlank() }

        return when (val result = lookup(name, hint)) {
            is SpeciesLookupResult.Fresh -> upgrade(plant.id, plant.speciesId, result.species)
            is SpeciesLookupResult.Stale -> upgrade(plant.id, plant.speciesId, result.species)
            SpeciesLookupResult.NotFound -> ReenrichOutcome.NotFound
            is SpeciesLookupResult.Offline -> ReenrichOutcome.Offline
        }
    }

    private suspend fun upgrade(
        plantId: String,
        currentSpeciesId: String?,
        upgraded: Species,
    ): ReenrichOutcome {
        if (upgraded.id == currentSpeciesId) return ReenrichOutcome.AlreadyEnriched
        // Re-fetch in case the plant changed under us; preserves nickname / notes.
        val freshPlant = plants.findPlant(plantId) ?: return ReenrichOutcome.NoPlant
        plants.upsert(freshPlant.copy(speciesId = upgraded.id))
        return ReenrichOutcome.Upgraded(upgraded.id)
    }
}
