package com.floracare.app.domain.repository

import com.floracare.app.domain.model.Species

/**
 * Second-tier taxonomy lookup. Kept separate from [PlantRepository] so the
 * on-device cache remains the authoritative read model for the UI while the
 * remote-fetch + cache plumbing stays out of the God-repository.
 */
interface SpeciesRepository {
    /**
     * Resolve a classifier prediction to a real species. Caller passes the
     * scientific name (preferred) and, optionally, the common name as a
     * disambiguation hint for the remote search.
     *
     * Never throws for network/HTTP trouble — translates everything into the
     * [SpeciesLookupResult] hierarchy so callers can handle offline and
     * rate-limit cases without a try/catch.
     */
    suspend fun lookup(scientificName: String, commonNameHint: String? = null): SpeciesLookupResult
}

sealed interface SpeciesLookupResult {
    data class Fresh(val species: Species) : SpeciesLookupResult
    data class Stale(val species: Species, val reason: StaleReason) : SpeciesLookupResult
    data object NotFound : SpeciesLookupResult
    data class Offline(val cached: Species?) : SpeciesLookupResult
}

enum class StaleReason { REMOTE_UNAVAILABLE, RATE_LIMITED, TIMEOUT }
