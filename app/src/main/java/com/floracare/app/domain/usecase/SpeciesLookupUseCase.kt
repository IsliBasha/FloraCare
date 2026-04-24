package com.floracare.app.domain.usecase

import com.floracare.app.domain.repository.SpeciesLookupResult
import com.floracare.app.domain.repository.SpeciesRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin delegate over [SpeciesRepository.lookup]. Lives as a use-case so
 * ViewModels depend on an intention (`lookup a species`) rather than the
 * repository implementation.
 */
@Singleton
class SpeciesLookupUseCase @Inject constructor(
    private val species: SpeciesRepository,
) {
    suspend operator fun invoke(
        scientificName: String,
        commonNameHint: String? = null,
    ): SpeciesLookupResult {
        val normalised = scientificName.trim()
        require(normalised.isNotEmpty()) { "Scientific name must not be blank" }
        val hint = commonNameHint?.trim()?.takeIf { it.isNotEmpty() }
        return species.lookup(normalised, hint)
    }
}
