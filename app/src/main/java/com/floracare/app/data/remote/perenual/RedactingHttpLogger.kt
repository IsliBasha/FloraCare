package com.floracare.app.data.remote.perenual

import okhttp3.logging.HttpLoggingInterceptor

/**
 * Drop-in BASIC logger that masks `key=...` in the URL line before handing it
 * to the underlying [HttpLoggingInterceptor]. Only used in debug builds so we
 * never log API keys in release.
 */
internal class RedactingHttpLogger : HttpLoggingInterceptor.Logger {
    private val delegate = HttpLoggingInterceptor.Logger.DEFAULT
    override fun log(message: String) {
        delegate.log(KEY_PATTERN.replace(message, "key=***"))
    }

    companion object {
        private val KEY_PATTERN = Regex("key=[^&\\s]+")
    }
}
