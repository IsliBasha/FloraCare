package com.floracare.app.data.remote.weather

import com.floracare.app.BuildConfig
import com.floracare.app.data.remote.CurrentWeatherResponse
import com.floracare.app.data.remote.WeatherApi
import com.floracare.app.data.remote.perenual.RemoteResult
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Low-level OpenWeather seam used by [com.floracare.app.data.repository.WeatherRepositoryImpl].
 * Kept as an interface so tests can swap in a deterministic fake without mocking Retrofit.
 */
interface WeatherRemoteDataSource {
    suspend fun fetch(lat: Double, lon: Double): RemoteResult<CurrentWeatherResponse>
}

/**
 * Retrofit-backed implementation. Translates Retrofit exceptions into the
 * shared [RemoteResult] hierarchy so HTTP concerns never leak upward.
 *
 * When [BuildConfig.OPENWEATHER_KEY] is blank (developer machines without a key)
 * we short-circuit to [RemoteResult.Network] so the repository falls back to
 * cached snapshots without crashing.
 */
@Singleton
class WeatherRemoteDataSourceImpl @Inject constructor(
    private val api: WeatherApi,
) : WeatherRemoteDataSource {
    private val hasKey: Boolean = BuildConfig.OPENWEATHER_KEY.isNotBlank()

    override suspend fun fetch(lat: Double, lon: Double): RemoteResult<CurrentWeatherResponse> {
        if (!hasKey) {
            return RemoteResult.Network(IOException("OPENWEATHER_KEY not configured"))
        }
        return try {
            RemoteResult.Success(
                api.current(lat = lat, lon = lon, key = BuildConfig.OPENWEATHER_KEY),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == HTTP_RATE_LIMIT) RemoteResult.RateLimited
            else RemoteResult.Http(e.code())
        } catch (e: IOException) {
            RemoteResult.Network(e)
        }
    }

    private companion object {
        const val HTTP_RATE_LIMIT = 429
    }
}
