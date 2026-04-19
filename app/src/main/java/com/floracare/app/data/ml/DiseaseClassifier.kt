package com.floracare.app.data.ml

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface DiseaseClassifier {
    suspend fun topK(bitmap: Bitmap, k: Int = 3): List<Prediction>
    val isReady: Boolean
}

@Singleton
class TfliteDiseaseClassifier @Inject constructor(
    private val context: Context,
) : DiseaseClassifier {

    private val model = TfliteModelLoader(context, MODEL_NAME, LABELS_NAME)

    override val isReady: Boolean get() = model.isLoaded

    override suspend fun topK(bitmap: Bitmap, k: Int): List<Prediction> = withContext(Dispatchers.Default) {
        if (!model.isLoaded) return@withContext MOCK_TOP3.take(k)
        // TODO(person-b): real leaf preprocessing + inference.
        MOCK_TOP3.take(k)
    }

    companion object {
        const val MODEL_NAME = "leaf_disease_v1.tflite"
        const val LABELS_NAME = "disease_labels.txt"

        private val MOCK_TOP3 = listOf(
            Prediction("Healthy", 0.82f),
            Prediction("Leaf spot (fungal)", 0.12f),
            Prediction("Nutrient deficiency", 0.06f),
        )
    }
}
