package com.floracare.app.data.remote.perenual

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Perenual v1 REST client. The `?key=` query param is attached by
 * [PerenualAuthInterceptor] so call sites never need to pass it.
 */
interface PerenualApi {
    @GET("species-list")
    suspend fun search(@Query("q") query: String): SpeciesSearchResponse

    @GET("species/details/{id}")
    suspend fun details(@Path("id") id: Long): SpeciesDetailsResponse
}
