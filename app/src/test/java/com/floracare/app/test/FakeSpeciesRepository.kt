package com.floracare.app.test

import com.floracare.app.domain.repository.SpeciesLookupResult
import com.floracare.app.domain.repository.SpeciesRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * Deterministic fake for [SpeciesRepository]. Returns the staged result and
 * records every lookup call so tests can assert exactly what was queried.
 *
 * Set [pendingGate] to a [CompletableDeferred] to suspend lookups until the
 * test explicitly completes or cancels it — useful for exercising the
 * Enriching-state cancel path.
 */
class FakeSpeciesRepository(
    var result: SpeciesLookupResult = SpeciesLookupResult.NotFound,
) : SpeciesRepository {
    val lookups = mutableListOf<Pair<String, String?>>()
    var pendingGate: CompletableDeferred<Unit>? = null

    override suspend fun lookup(scientificName: String, commonNameHint: String?): SpeciesLookupResult {
        lookups += scientificName to commonNameHint
        pendingGate?.await()
        return result
    }
}
