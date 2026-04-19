package com.floracare.app.data.ml

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface SpeciesClassifier {
    suspend fun topK(bitmap: Bitmap, k: Int = 3): List<Prediction>
    val isReady: Boolean
}

@Singleton
class TfliteSpeciesClassifier @Inject constructor(
    private val context: Context,
) : SpeciesClassifier {

    private val model = TfliteModelLoader(context, MODEL_NAME, LABELS_NAME)

    override val isReady: Boolean get() = model.isLoaded

    override suspend fun topK(bitmap: Bitmap, k: Int): List<Prediction> = withContext(Dispatchers.Default) {
        if (!model.isLoaded) {
            return@withContext MOCK_TOP3.take(k)
        }
        // TODO(person-b): full image pre-processing + inference pipeline.
        //                 Placeholder returns mock until real model is dropped in.
        MOCK_TOP3.take(k)
    }

    companion object {
        const val MODEL_NAME = "plant_species_v1.tflite"
        const val LABELS_NAME = "species_labels.txt"

        private val MOCK_TOP3 = listOf(
            Prediction("Monstera deliciosa", 0.87f),
            Prediction("Philodendron bipinnatifidum", 0.06f),
            Prediction("Epipremnum aureum", 0.04f),
        )
    }
}
