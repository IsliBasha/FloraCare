package com.floracare.app.test

import com.floracare.app.data.remote.CurrentWeatherResponse
import com.floracare.app.data.remote.perenual.RemoteResult
import com.floracare.app.data.remote.weather.WeatherRemoteDataSource

/**
 * Programmable fake for [WeatherRemoteDataSource]. Defaults to a Network
 * failure (mirrors the build-time "no key" short-circuit) so tests must
 * opt into success explicitly.
 */
class FakeWeatherRemoteDataSource : WeatherRemoteDataSource {
    var nextResult: RemoteResult<CurrentWeatherResponse> =
        RemoteResult.Network(java.io.IOException("not configured in test"))
    var fetchCount = 0
    var lastLat: Double? = null
    var lastLon: Double? = null

    override suspend fun fetch(lat: Double, lon: Double): RemoteResult<CurrentWeatherResponse> {
        fetchCount += 1
        lastLat = lat
        lastLon = lon
        return nextResult
    }
}
