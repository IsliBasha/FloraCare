package com.floracare.app.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.floracare.app.BuildConfig
import com.floracare.app.domain.model.ConfidenceThresholds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cascade/router [SpeciesClassifier] per `floracare-houseplant-classifier.md`:
 * runs the general-purpose AIY Vision Plants V1 model first, and only
 * consults the houseplant-specialist ViT model (47 classes) when AIY's own
 * top confidence is below [ConfidenceThresholds.HOUSEPLANT_LOW_CONFIDENCE].
 *
 * This class owns *both* underlying TFLite runners directly — it is
 * deliberately not a composite that wraps two independent [SpeciesClassifier]
 * beans (e.g. [TfliteSpeciesClassifier] plus a hypothetical ViT counterpart).
 * That keeps this class as a single lazy-init story and a single
 * fallback story, and rules out the old per-classifier MOCK-fallback
 * pattern leaking into the merge on the ViT side (see bug #4 in the plan:
 * a missing/failed ViT model must yield `vitReady = false` and an empty
 * prediction list, never a fabricated result).
 *
 * The ViT interpreter is ~86MB and takes noticeably longer to build than
 * AIY's. To avoid stalling the very call that triggered it, the first ViT
 * init happens off the calling coroutine on [vitInitScope]; a call that
 * arrives while that background init is still in flight (or hasn't started
 * yet) transparently falls back to AIY-only for that one call and simply
 * benefits from the now-warm runner on the next call. See
 * [decideVitConsult] for the pure, JVM-testable routing decision this
 * class delegates to.
 */
@Singleton
class HouseplantAwareClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeciesClassifier {

    private val aiyModel = TfliteModelLoader(context, AIY_MODEL_NAME, AIY_LABELS_NAME)
    private val aiyRunnerLock = Mutex()

    @Volatile
    private var aiyRunner: TfliteRunner? = null

    @Volatile
    private var aiyRunnerInitFailed: Boolean = false

    private val vitModel = TfliteModelLoader(context, VIT_MODEL_NAME, VIT_LABELS_NAME)
    private val vitRunnerLock = Mutex()
    private val vitInitScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var vitRunner: TfliteRunner? = null

    @Volatile
    private var vitRunnerInitFailed: Boolean = false

    @Volatile
    private var vitInitJob: Job? = null

    /**
     * Reports AIY readiness only — the feature must never regress to
     * "classifier not ready" just because the ViT asset is absent or still
     * loading in the background.
     */
    override val isReady: Boolean get() = aiyModel.isLoaded

    override suspend fun topK(bitmap: Bitmap, k: Int): List<Prediction> =
        withContext(Dispatchers.Default) {
            val aiyPredictions = runAiy(bitmap, k)

            when (
                decideVitConsult(
                    aiyPredictions = aiyPredictions,
                    isVitRunnerReady = vitRunner != null,
                    houseplantThreshold = ConfidenceThresholds.HOUSEPLANT_LOW_CONFIDENCE,
                )
            ) {
                VitConsultDecision.SKIP_AIY_CONFIDENT -> aiyPredictions
                VitConsultDecision.SKIP_VIT_NOT_READY -> {
                    kickOffVitInitIfNeeded()
                    aiyPredictions
                }
                VitConsultDecision.CONSULT_VIT -> {
                    val (vitReady, vitPredictions) = runVit(bitmap, k)
                    mergeHouseplant(
                        aiyPredictions = aiyPredictions,
                        vitPredictions = vitPredictions,
                        vitReady = vitReady,
                        houseplantThreshold = ConfidenceThresholds.HOUSEPLANT_LOW_CONFIDENCE,
                    )
                }
            }
        }

    // ---- AIY path (mirrors TfliteSpeciesClassifier's existing contract) ----

    private suspend fun runAiy(bitmap: Bitmap, k: Int): List<Prediction> {
        if (!aiyModel.isLoaded || aiyRunnerInitFailed) return MOCK_TOP3.take(k)

        val activeRunner = ensureAiyRunner() ?: return MOCK_TOP3.take(k)
        val input = bitmap.preprocessForModel(AIY_INPUT_SIDE)
        val logits = aiyRunnerLock.withLock { activeRunner.run(input) }
        if (BuildConfig.DEBUG) logTopRawForDebug(logits, aiyModel.labels)
        return parseTopK(logits, aiyModel.labels, k)
    }

    private suspend fun ensureAiyRunner(): TfliteRunner? {
        aiyRunner?.let { return it }
        return aiyRunnerLock.withLock {
            aiyRunner ?: runCatching {
                val buf = aiyModel.loadModelBuffer()
                    ?: error("model present but mmap failed")
                TfliteRunner.create(buf).also { aiyRunner = it }
            }.onFailure {
                Log.e(TAG, "AIY species classifier init failed, using mock", it)
                aiyRunnerInitFailed = true
            }.getOrNull()
        }
    }

    private fun logTopRawForDebug(logits: FloatArray, labels: List<String>) {
        val usable = minOf(logits.size, labels.size)
        if (usable == 0) return
        val top = (0 until usable)
            .sortedByDescending { logits[it] }
            .take(DEBUG_TOP_N)
        val summary = buildString {
            append("raw top-$DEBUG_TOP_N: ")
            top.forEachIndexed { i, idx ->
                if (i > 0) append(", ")
                val label = labels[idx].ifBlank { "(blank)" }
                append("%s=%.4f".format(label, logits[idx]))
            }
        }
        Log.d(TAG, summary)
    }

    // ---- ViT path ----

    /**
     * Result of attempting the ViT path for one call. [ready] is `false`
     * whenever the runner wasn't available or inference failed for any
     * reason — in both cases [predictions] is always empty, never a
     * fabricated/mock result (bug #4 in the plan).
     */
    private data class VitOutcome(val ready: Boolean, val predictions: List<Prediction>)

    private suspend fun runVit(bitmap: Bitmap, k: Int): VitOutcome {
        val activeRunner = vitRunner ?: return VitOutcome(ready = false, predictions = emptyList())

        val predictions = runCatching {
            val input = bitmap.preprocessForModel(VIT_INPUT_SIDE)
            val logits = vitRunnerLock.withLock { activeRunner.run(input) }
            parseTopK(logits, vitModel.labels, k)
        }.onFailure {
            Log.e(TAG, "Houseplant ViT inference failed", it)
        }.getOrNull()

        return if (predictions != null) {
            VitOutcome(ready = true, predictions = predictions)
        } else {
            VitOutcome(ready = false, predictions = emptyList())
        }
    }

    /**
     * Starts building the ViT interpreter on [vitInitScope] if it isn't
     * already built, failed, or currently in flight. Never called for a
     * call where AIY was already confident (see [VitConsultDecision]),
     * and never awaited by the caller — the current `topK` call always
     * falls back to AIY-only regardless of how this background job turns
     * out.
     */
    @Synchronized
    private fun kickOffVitInitIfNeeded() {
        if (!vitModel.isLoaded) return
        if (vitRunner != null || vitRunnerInitFailed) return
        if (vitInitJob?.isActive == true) return

        vitInitJob = vitInitScope.launch {
            vitRunnerLock.withLock {
                if (vitRunner != null || vitRunnerInitFailed) return@withLock
                runCatching {
                    val buf = vitModel.loadModelBuffer()
                        ?: error("model present but mmap failed")
                    // The ViT model bakes its own [0.5,0.5,0.5] normalisation
                    // into the graph and expects raw [0,255] pixel values --
                    // not ImageNet mean/std (that's a different model family's
                    // convention and would corrupt this model's input).
                    TfliteRunner.create(buf, float32InputMode = TfliteRunner.Float32InputMode.RAW_0_255)
                }.onSuccess {
                    vitRunner = it
                }.onFailure {
                    Log.e(TAG, "Houseplant ViT classifier init failed", it)
                    vitRunnerInitFailed = true
                }
            }
        }
    }

    companion object {
        private const val TAG = "HouseplantClassifier"

        const val AIY_MODEL_NAME = "plant_species_v1.tflite"
        const val AIY_LABELS_NAME = "species_labels.txt"
        private const val AIY_INPUT_SIDE = 224

        const val VIT_MODEL_NAME = "houseplant_vit_v1.tflite"
        const val VIT_LABELS_NAME = "houseplant_vit_labels.txt"
        private const val VIT_INPUT_SIDE = 224

        private const val DEBUG_TOP_N = 10

        private val MOCK_TOP3 = listOf(
            Prediction("Monstera deliciosa", 0.87f),
            Prediction("Philodendron bipinnatifidum", 0.06f),
            Prediction("Epipremnum aureum", 0.04f),
        )
    }
}
