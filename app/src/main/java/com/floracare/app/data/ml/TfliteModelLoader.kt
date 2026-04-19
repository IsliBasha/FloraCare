package com.floracare.app.data.ml

import android.content.Context
import android.util.Log

/**
 * Attempts to locate a TFLite asset by name. Returns [isLoaded] = false when the model
 * file is absent so callers can transparently fall back to a mock prediction path.
 * The real Interpreter wiring (with GPU delegate) lives in a follow-up ticket.
 */
class TfliteModelLoader(
    context: Context,
    private val modelAssetName: String,
    private val labelsAssetName: String,
) {
    val isLoaded: Boolean
    val labels: List<String>

    init {
        val assets = context.assets.list("ml")?.toSet().orEmpty()
        isLoaded = modelAssetName in assets
        labels = if (labelsAssetName in assets) {
            runCatching {
                context.assets.open("ml/$labelsAssetName").bufferedReader().useLines { it.toList() }
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        if (!isLoaded) {
            Log.i(TAG, "TFLite model $modelAssetName not in assets/ml — using mock predictions.")
        }
    }

    companion object {
        private const val TAG = "TfliteModelLoader"
    }
}
