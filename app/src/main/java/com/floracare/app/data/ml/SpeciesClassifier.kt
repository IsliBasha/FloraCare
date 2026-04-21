package com.floracare.app.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface SpeciesClassifier {
    suspend fun topK(bitmap: Bitmap, k: Int = 3): List<Prediction>
    val isReady: Boolean
}

@Singleton
class TfliteSpeciesClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeciesClassifier {

    private val model = TfliteModelLoader(context, MODEL_NAME, LABELS_NAME)
    private val runnerLock = Mutex()

    @Volatile
    private var runner: TfliteRunner? = null

    @Volatile
    private var runnerInitFailed: Boolean = false

    override val isReady: Boolean get() = model.isLoaded

    override suspend fun topK(bitmap: Bitmap, k: Int): List<Prediction> =
        withContext(Dispatchers.Default) {
            if (!model.isLoaded || runnerInitFailed) return@withContext MOCK_TOP3.take(k)

            val activeRunner = ensureRunner() ?: return@withContext MOCK_TOP3.take(k)
            val input = bitmap.preprocessForModel(INPUT_SIDE)
            val logits = runnerLock.withLock { activeRunner.run(input) }
            parseTopK(logits, model.labels, k)
        }

    private suspend fun ensureRunner(): TfliteRunner? {
        runner?.let { return it }
        return runnerLock.withLock {
            runner ?: runCatching {
                val buf = model.loadModelBuffer()
                    ?: error("model present but mmap failed")
                TfliteRunner.create(buf).also { runner = it }
            }.onFailure {
                Log.e(TAG, "Species classifier init failed, using mock", it)
                runnerInitFailed = true
            }.getOrNull()
        }
    }

    companion object {
        private const val TAG = "SpeciesClassifier"
        const val MODEL_NAME = "plant_species_v1.tflite"
        const val LABELS_NAME = "species_labels.txt"
        private const val INPUT_SIDE = 224

        private val MOCK_TOP3 = listOf(
            Prediction("Monstera deliciosa", 0.87f),
            Prediction("Philodendron bipinnatifidum", 0.06f),
            Prediction("Epipremnum aureum", 0.04f),
        )
    }
}
