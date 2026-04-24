package com.floracare.app.test

import com.floracare.app.data.remote.perenual.PerenualRemoteDataSource
import com.floracare.app.data.remote.perenual.RemoteResult
import com.floracare.app.data.remote.perenual.SpeciesDetailsResponse
import com.floracare.app.data.remote.perenual.SpeciesSearchItem

/**
 * Deterministic fake for [PerenualRemoteDataSource]. Each call records the
 * inputs and returns a pre-staged result, so tests can assert exactly which
 * lookups fired and what came back.
 */
class FakePerenualRemoteDataSource : PerenualRemoteDataSource {
    var searchResult: RemoteResult<SpeciesSearchItem> = RemoteResult.Empty
    var detailsResult: RemoteResult<SpeciesDetailsResponse> = RemoteResult.Empty

    val searchQueries = mutableListOf<String>()
    val detailsIds = mutableListOf<Long>()

    override suspend fun search(query: String): RemoteResult<SpeciesSearchItem> {
        searchQueries += query
        return searchResult
    }

    override suspend fun details(id: Long): RemoteResult<SpeciesDetailsResponse> {
        detailsIds += id
        return detailsResult
    }
}
