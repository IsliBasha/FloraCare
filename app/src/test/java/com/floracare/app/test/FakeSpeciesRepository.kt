package com.floracare.app.test

import com.floracare.app.domain.repository.SpeciesLookupResult
import com.floracare.app.domain.repository.SpeciesRepository

/**
 * Deterministic fake for [SpeciesRepository]. Returns the staged result and
 * records every lookup call so tests can assert exactly what was queried.
 */
class FakeSpeciesRepository(
    var result: SpeciesLookupResult = SpeciesLookupResult.NotFound,
) : SpeciesRepository {
    val lookups = mutableListOf<Pair<String, String?>>()

    override suspend fun lookup(scientificName: String, commonNameHint: String?): SpeciesLookupResult {
        lookups += scientificName to commonNameHint
        return result
    }
}
