package com.floracare.app.data.remote.perenual

import com.floracare.app.BuildConfig
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Low-level Perenual wrapper. Translates Retrofit exceptions into the closed
 * [RemoteResult] hierarchy so the repository can pattern-match without
 * leaking HTTP concerns.
 *
 * When [BuildConfig.PERENUAL_KEY] is blank (developer machines without a key)
 * we short-circuit to [RemoteResult.Network] so the app still runs and the
 * repository falls back to stale cache / synth rows.
 */
@Singleton
internal class PerenualRemoteDataSource @Inject constructor(
    private val api: PerenualApi,
) {
    private val hasKey: Boolean = BuildConfig.PERENUAL_KEY.isNotBlank()

    suspend fun search(query: String): RemoteResult<SpeciesSearchItem> {
        if (!hasKey) return RemoteResult.Network(IOException("PERENUAL_KEY not configured"))
        return safeCall {
            val hits = api.search(query).data
            hits.firstOrNull()
        }.toResult()
    }

    suspend fun details(id: Long): RemoteResult<SpeciesDetailsResponse> {
        if (!hasKey) return RemoteResult.Network(IOException("PERENUAL_KEY not configured"))
        return safeCall { api.details(id) }.toResult()
    }

    private suspend fun <T : Any> safeCall(block: suspend () -> T?): CallOutcome<T> =
        try {
            val value = block()
            if (value == null) CallOutcome.Empty else CallOutcome.Success(value)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == HTTP_RATE_LIMIT) CallOutcome.RateLimited
            else CallOutcome.Http(e.code())
        } catch (e: IOException) {
            CallOutcome.Network(e)
        }

    private fun <T : Any> CallOutcome<T>.toResult(): RemoteResult<T> = when (this) {
        is CallOutcome.Success -> RemoteResult.Success(value)
        CallOutcome.Empty -> RemoteResult.Empty
        CallOutcome.RateLimited -> RemoteResult.RateLimited
        is CallOutcome.Network -> RemoteResult.Network(cause)
        is CallOutcome.Http -> RemoteResult.Http(code)
    }

    private sealed interface CallOutcome<out T : Any> {
        data class Success<T : Any>(val value: T) : CallOutcome<T>
        data object Empty : CallOutcome<Nothing>
        data object RateLimited : CallOutcome<Nothing>
        data class Network(val cause: IOException) : CallOutcome<Nothing>
        data class Http(val code: Int) : CallOutcome<Nothing>
    }

    private companion object {
        const val HTTP_RATE_LIMIT = 429
    }
}

/**
 * Internal transport outcome. Does not leak beyond [com.floracare.app.data.remote.perenual]
 * — [com.floracare.app.data.repository.SpeciesRepositoryImpl] translates it to the
 * domain [com.floracare.app.domain.repository.SpeciesLookupResult].
 */
internal sealed interface RemoteResult<out T : Any> {
    data class Success<T : Any>(val value: T) : RemoteResult<T>
    data object Empty : RemoteResult<Nothing>
    data object RateLimited : RemoteResult<Nothing>
    data class Network(val cause: IOException) : RemoteResult<Nothing>
    data class Http(val code: Int) : RemoteResult<Nothing>
}
