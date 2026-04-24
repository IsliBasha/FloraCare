package com.floracare.app.data.remote.perenual

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Appends the Perenual API key as a `key=...` query parameter on every request.
 *
 * The key comes from BuildConfig at injection time; never logged. Pair with
 * [RedactingHttpLogger] so debug logcat output masks the key even at BASIC
 * logging level.
 */
internal class PerenualAuthInterceptor(
    private val apiKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.newBuilder()
            .addQueryParameter("key", apiKey)
            .build()
        return chain.proceed(original.newBuilder().url(url).build())
    }
}
