package com.floracare.app.data.repository

import com.floracare.app.data.local.SpeciesDao
import com.floracare.app.data.local.toDomain
import com.floracare.app.data.local.toEntity
import com.floracare.app.data.remote.perenual.PerenualMapper
import com.floracare.app.data.remote.perenual.PerenualRemoteDataSource
import com.floracare.app.data.remote.perenual.RemoteResult
import com.floracare.app.data.remote.perenual.SpeciesDetailsResponse
import com.floracare.app.data.remote.perenual.SpeciesSearchItem
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.repository.SpeciesLookupResult
import com.floracare.app.domain.repository.SpeciesRepository
import com.floracare.app.domain.repository.StaleReason
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

/**
 * Stale-while-revalidate repository over Perenual + Room.
 *
 * Cache policy:
 *  - fresh cache (< [FRESH_TTL]) → [SpeciesLookupResult.Fresh] without a network call.
 *  - stale-but-usable cache (< [USABLE_TTL]) → try remote; on success upsert + Fresh,
 *    on failure return the cached row as [SpeciesLookupResult.Stale].
 *  - no cache → try remote; on failure return [SpeciesLookupResult.Offline]
 *    with `cached = null`.
 */
@Singleton
class SpeciesRepositoryImpl @Inject constructor(
    private val speciesDao: SpeciesDao,
    private val remote: PerenualRemoteDataSource,
    private val clock: Clock,
) : SpeciesRepository {

    override suspend fun lookup(
        scientificName: String,
        commonNameHint: String?,
    ): SpeciesLookupResult {
        val normalised = scientificName.trim()
        val now = clock.now()
        val cached = speciesDao.findByScientificName(normalised)?.toDomain()
        val usableCache = cached?.takeIf { it.isPerenualWithinUsableWindow(now) }

        if (cached != null && cached.isPerenualFresh(now)) {
            return SpeciesLookupResult.Fresh(cached)
        }

        val query = commonNameHint?.trim()?.takeIf { it.isNotEmpty() } ?: normalised
        return when (val search = remote.search(query)) {
            is RemoteResult.Success -> fetchDetailsAndPersist(search.value, usableCache, now)
            RemoteResult.Empty -> noRemoteMatch(usableCache, StaleReason.REMOTE_UNAVAILABLE)
            RemoteResult.RateLimited -> staleOr(usableCache, StaleReason.RATE_LIMITED)
            is RemoteResult.Network -> staleOr(usableCache, StaleReason.REMOTE_UNAVAILABLE)
            is RemoteResult.Http -> staleOr(usableCache, StaleReason.REMOTE_UNAVAILABLE)
        }
    }

    private suspend fun fetchDetailsAndPersist(
        hit: SpeciesSearchItem,
        cached: Species?,
        now: Instant,
    ): SpeciesLookupResult = when (val details = remote.details(hit.id)) {
        is RemoteResult.Success -> persistFresh(details.value, now)
        RemoteResult.Empty -> staleOr(cached, StaleReason.REMOTE_UNAVAILABLE)
        RemoteResult.RateLimited -> staleOr(cached, StaleReason.RATE_LIMITED)
        is RemoteResult.Network -> staleOr(cached, StaleReason.REMOTE_UNAVAILABLE)
        is RemoteResult.Http -> staleOr(cached, StaleReason.REMOTE_UNAVAILABLE)
    }

    private suspend fun persistFresh(
        details: SpeciesDetailsResponse,
        now: Instant,
    ): SpeciesLookupResult {
        val mapped = PerenualMapper.toDomain(details, now)
        speciesDao.upsert(mapped.toEntity())
        return SpeciesLookupResult.Fresh(mapped)
    }

    private fun staleOr(cached: Species?, reason: StaleReason): SpeciesLookupResult =
        if (cached != null) SpeciesLookupResult.Stale(cached, reason)
        else SpeciesLookupResult.Offline(null)

    private fun noRemoteMatch(cached: Species?, reason: StaleReason): SpeciesLookupResult =
        if (cached != null) SpeciesLookupResult.Stale(cached, reason)
        else SpeciesLookupResult.NotFound

    private fun Species.isPerenualFresh(now: Instant): Boolean {
        val at = fetchedAt ?: return false
        return provider == Species.PROVIDER_PERENUAL && now - at < FRESH_TTL
    }

    private fun Species.isPerenualWithinUsableWindow(now: Instant): Boolean {
        val at = fetchedAt ?: return false
        return provider == Species.PROVIDER_PERENUAL && now - at < USABLE_TTL
    }

    private companion object {
        val FRESH_TTL = 7.days
        val USABLE_TTL = 90.days
    }
}
