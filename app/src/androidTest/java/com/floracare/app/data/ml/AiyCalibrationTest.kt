package com.floracare.app.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.floracare.app.ui.feature.identify.IdentifyViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * On-device calibration probe for the AIY Vision Plants V1 classifier.
 *
 * Symptom being investigated: on indoor houseplant photos (e.g. snake plant),
 * the quantised AIY model returns `background` ~30 % with remaining top-k
 * values packed near 0.001, making the picker useless.
 *
 * This test is *diagnostic*, not a regression assertion. It prints top-10
 * predictions per preprocessing variant to logcat (tag `AiyCalib`) and test
 * stdout so we can compare:
 *
 *  - **V_currentPipeline** — what the app actually feeds the model today
 *    (`normalizePixels → (v*255) uint8`, matches [TfliteRunner.allocUint8Input]).
 *  - **V_aiyCanonical** — `[-1, 1]` float quantised via the tensor's real
 *    `inScale` / `inZero` (`round(v / scale + zero)`).
 *  - **V_rawBytes** — direct RGB bytes, skipping float normalisation entirely.
 *  - **Rotation sweep** — V_currentPipeline on 0°/90°/180°/270° copies of the
 *    same fixture, to rule out CameraX-orientation collapse.
 *
 * The three variants should produce *identical* output if quantisation is
 * arithmetically correct — any divergence proves a preprocessing bug.
 */
@RunWith(AndroidJUnit4::class)
class AiyCalibrationTest {

    /** App-under-test context — used for model + labels shipped in production assets. */
    private val appCtx: Context get() = ApplicationProvider.getApplicationContext()

    /** Instrumentation (test APK) context — holds the androidTest assets/fixtures files. */
    private val testCtx: Context get() = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun calibrates_snake_plant_fixture() = runCalibration(
        fixturePath = "fixtures/snake_plant.jpg",
        expected = ExpectedCalibration(
            // AIY V1 has no Sansevieria class — top-1 for this fixture is "background"
            // with a tiny confidence. The banner threshold must catch it.
            top1Label = "background",
            top1Min = 0.05f,
            top1Max = 0.15f,
            belowBannerThreshold = true,
        ),
    )

    @Test
    fun calibrates_unidentified_plant_fixture() = runCalibration(
        fixturePath = "fixtures/unidentified_plant.jpg",
        expected = ExpectedCalibration(
            // An in-distribution outdoor plant — the model is actually confident.
            top1Label = "Pinus strobus",
            top1Min = 0.35f,
            top1Max = 0.60f,
            belowBannerThreshold = false,
        ),
    )

    @Test
    fun production_classifier_emits_top3_for_oov_snake_plant() {
        val bitmap = loadFixture("fixtures/snake_plant.jpg")
        val classifier = TfliteSpeciesClassifier(appCtx)
        assertTrue("classifier should be ready; is the tflite asset bundled?", classifier.isReady)

        val prodTop3 = runBlocking { classifier.topK(bitmap, k = 3) }
        logBlock("production_classifier snake_plant top-3") {
            prodTop3.forEachIndexed { i, p ->
                line("  #${i + 1} ${p.label} = %.4f".format(p.confidence))
            }
        }
        assertEquals("production path must deliver exactly 3 predictions", 3, prodTop3.size)
        // `background` is filtered by parseTopK; the raw top-1 here (background) gets dropped
        // so the first surviving label should be an unfiltered species — not "background", not blank.
        val firstLabel = prodTop3.first().label
        assertTrue(
            "top label must not be 'background' (should be filtered by parseTopK): $firstLabel",
            !firstLabel.equals("background", ignoreCase = true),
        )
        assertTrue("top label must not be blank", firstLabel.isNotBlank())
        // Confidence is expected to sit below the banner threshold — this is an OOV input.
        assertTrue(
            "OOV fixture should stay below the low-confidence banner threshold " +
                "(${IdentifyViewModel.LOW_CONFIDENCE_THRESHOLD}); got ${prodTop3.first().confidence}",
            prodTop3.first().confidence < IdentifyViewModel.LOW_CONFIDENCE_THRESHOLD,
        )
    }

    // --- harness -----------------------------------------------------------

    /**
     * Expected calibration outcome for a fixture. Pinning these values turns
     * the on-device diagnostic into a regression guard — if a future model
     * swap or preprocessing change shifts the top-1 label or confidence, the
     * test fails noisily instead of silently degrading the identify flow.
     */
    private data class ExpectedCalibration(
        val top1Label: String,
        val top1Min: Float,
        val top1Max: Float,
        /** Whether the fixture should be flagged by [IdentifyViewModel.LOW_CONFIDENCE_THRESHOLD]. */
        val belowBannerThreshold: Boolean,
    )

    private fun runCalibration(fixturePath: String, expected: ExpectedCalibration) {
        val probe = InterpreterProbe.open(appCtx)
        try {
            val bitmap = loadFixture(fixturePath)

            logBlock("=== $fixturePath (${bitmap.width}x${bitmap.height}) ===") {
                line("model inputSide=${probe.inputSide}, inScale=${probe.inScale}, inZero=${probe.inZero}")
                line("model outputSize=${probe.outputSize}, outScale=${probe.outScale}, outZero=${probe.outZero}")
            }

            val resized = Bitmap.createScaledBitmap(bitmap, probe.inputSide, probe.inputSide, /* filter */ true)
            val pixels = IntArray(probe.inputSide * probe.inputSide)
            resized.getPixels(pixels, 0, probe.inputSide, 0, 0, probe.inputSide, probe.inputSide)

            val variants = listOf(
                "V_currentPipeline" to encodeCurrentPipeline(pixels, probe),
                "V_aiyCanonical" to encodeAiyCanonical(pixels, probe),
                "V_rawBytes" to encodeRawBytes(pixels),
            )
            val topByVariant = mutableMapOf<String, Pair<String, Float>>()
            for ((name, input) in variants) {
                val logits = probe.run(input)
                logTopK(name, logits, probe.labels, k = 10)
                topByVariant[name] = extractTop1(logits, probe.labels)
            }

            // Rotation sweep uses the current-pipeline encoding, which is what production ships.
            for (degrees in listOf(0, 90, 180, 270)) {
                val rotated = rotate(resized, degrees)
                val rotPixels = IntArray(probe.inputSide * probe.inputSide)
                rotated.getPixels(rotPixels, 0, probe.inputSide, 0, 0, probe.inputSide, probe.inputSide)
                val input = encodeCurrentPipeline(rotPixels, probe)
                val logits = probe.run(input)
                logTopK("V_currentPipeline@${degrees}deg", logits, probe.labels, k = 5)
                if (rotated !== resized) rotated.recycle()
            }

            if (resized !== bitmap) resized.recycle()
            bitmap.recycle()

            // Assert: shipping pipeline and raw-bytes pipeline agree byte-for-byte
            // (proof the float `p/255 → *255` round-trip is a no-op).
            assertEquals(
                "V_currentPipeline and V_rawBytes must produce the same top-1 label",
                topByVariant.getValue("V_rawBytes").first,
                topByVariant.getValue("V_currentPipeline").first,
            )
            assertEquals(
                "V_currentPipeline and V_rawBytes must produce the same top-1 confidence",
                topByVariant.getValue("V_rawBytes").second,
                topByVariant.getValue("V_currentPipeline").second,
                /* delta */ 0.0001f,
            )

            // Assert expected calibration outcome for the shipping pipeline.
            val (shipLabel, shipConf) = topByVariant.getValue("V_currentPipeline")
            assertEquals(
                "unexpected top-1 label for $fixturePath (has the model or label set changed?)",
                expected.top1Label,
                shipLabel,
            )
            assertTrue(
                "top-1 confidence $shipConf outside expected band " +
                    "[${expected.top1Min}, ${expected.top1Max}] for $fixturePath",
                shipConf in expected.top1Min..expected.top1Max,
            )
            val belowBanner = shipConf < IdentifyViewModel.LOW_CONFIDENCE_THRESHOLD
            assertEquals(
                "low-confidence banner gating disagrees with expectation for $fixturePath",
                expected.belowBannerThreshold,
                belowBanner,
            )
        } finally {
            probe.close()
        }
    }

    private fun extractTop1(logits: FloatArray, labels: List<String>): Pair<String, Float> {
        val usable = minOf(logits.size, labels.size)
        val idx = (0 until usable).maxBy { logits[it] }
        return labels[idx] to logits[idx]
    }

    private fun loadFixture(path: String): Bitmap {
        testCtx.assets.open(path).use { stream ->
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            return requireNotNull(BitmapFactory.decodeStream(stream, null, opts)) {
                "failed to decode fixture $path"
            }
        }
    }

    private fun rotate(src: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return src
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, /* filter */ true)
    }

    private fun logTopK(variant: String, logits: FloatArray, labels: List<String>, k: Int) {
        val usable = minOf(logits.size, labels.size)
        val top = (0 until usable).sortedByDescending { logits[it] }.take(k)
        logBlock("$variant top-$k") {
            top.forEachIndexed { rank, idx ->
                val label = labels[idx].ifBlank { "(blank@$idx)" }
                line("  #${rank + 1} [$idx] $label = %.4f".format(logits[idx]))
            }
        }
    }

    // --- encoders ----------------------------------------------------------

    /**
     * Mirrors production path: [normalizePixels] + [TfliteRunner.allocUint8Input].
     * Net effect per channel byte `p`: `p/255 * 255 → p` (bytes unchanged, save clamping).
     */
    private fun encodeCurrentPipeline(pixels: IntArray, probe: InterpreterProbe): ByteBuffer {
        val buf = probe.newInputBuffer()
        for (argb in pixels) {
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            buf.put((r * 255f).toInt().coerceIn(0, 255).toByte())
            buf.put((g * 255f).toInt().coerceIn(0, 255).toByte())
            buf.put((b * 255f).toInt().coerceIn(0, 255).toByte())
        }
        buf.rewind()
        return buf
    }

    /**
     * AIY canonical: float `[-1, 1]` (Inception preprocessing) quantised with the
     * interpreter's real input-tensor params — `q = round(f / scale + zero)`.
     */
    private fun encodeAiyCanonical(pixels: IntArray, probe: InterpreterProbe): ByteBuffer {
        val buf = probe.newInputBuffer()
        val inScale = if (probe.inScale == 0f) 1f / 128f else probe.inScale
        for (argb in pixels) {
            val r = (((argb shr 16) and 0xFF) / 127.5f) - 1f
            val g = (((argb shr 8) and 0xFF) / 127.5f) - 1f
            val b = ((argb and 0xFF) / 127.5f) - 1f
            buf.put(quantise(r, inScale, probe.inZero))
            buf.put(quantise(g, inScale, probe.inZero))
            buf.put(quantise(b, inScale, probe.inZero))
        }
        buf.rewind()
        return buf
    }

    /**
     * Control condition: feed raw `[0, 255]` pixel bytes directly. Equivalent
     * to V_currentPipeline arithmetically (same quantised bytes); any
     * divergence would indicate a bug in the float round-trip.
     */
    private fun encodeRawBytes(pixels: IntArray): ByteBuffer {
        val buf = ByteBuffer.allocateDirect(pixels.size * 3).order(ByteOrder.nativeOrder())
        for (argb in pixels) {
            buf.put(((argb shr 16) and 0xFF).toByte())
            buf.put(((argb shr 8) and 0xFF).toByte())
            buf.put((argb and 0xFF).toByte())
        }
        buf.rewind()
        return buf
    }

    private fun quantise(value: Float, scale: Float, zero: Int): Byte =
        (value / scale + zero).roundToInt().coerceIn(0, 255).toByte()

    // --- logging helper ----------------------------------------------------

    private fun logBlock(header: String, body: LineSink.() -> Unit) {
        val sink = LineSink()
        sink.line(header)
        sink.body()
        val rendered = sink.build()
        Log.i(TAG, rendered)
        // Also mirror to stdout so `./gradlew connectedAndroidTest` captures it.
        println(rendered)
    }

    private class LineSink {
        private val sb = StringBuilder()
        fun line(s: String) { sb.append(s).append('\n') }
        fun build(): String = sb.toString().trimEnd()
    }

    companion object {
        private const val TAG = "AiyCalib"
    }
}

/**
 * Loads the bundled TFLite asset and exposes a raw [Interpreter] + its
 * quantisation parameters so the calibration test can feed arbitrary
 * pre-encoded input buffers without going through [TfliteRunner].
 */
private class InterpreterProbe(
    private val interpreter: Interpreter,
    val labels: List<String>,
    val inputSide: Int,
    val outputSize: Int,
    val inScale: Float,
    val inZero: Int,
    val outScale: Float,
    val outZero: Int,
    private val outputIsUint8: Boolean,
) : AutoCloseable {

    fun newInputBuffer(): ByteBuffer =
        ByteBuffer.allocateDirect(inputSide * inputSide * 3).order(ByteOrder.nativeOrder())

    fun run(input: ByteBuffer): FloatArray {
        if (outputIsUint8) {
            val raw = Array(1) { ByteArray(outputSize) }
            interpreter.run(input, raw)
            val scale = if (outScale == 0f) 1f / 255f else outScale
            val out = FloatArray(outputSize)
            for (i in raw[0].indices) {
                val u = raw[0][i].toInt() and 0xFF
                out[i] = (u - outZero) * scale
            }
            return out
        }
        val floats = Array(1) { FloatArray(outputSize) }
        interpreter.run(input, floats)
        return floats[0]
    }

    override fun close() {
        runCatching { interpreter.close() }
    }

    companion object {
        fun open(ctx: Context): InterpreterProbe {
            val loader = TfliteModelLoader(
                ctx,
                TfliteSpeciesClassifier.MODEL_NAME,
                TfliteSpeciesClassifier.LABELS_NAME,
            )
            check(loader.isLoaded) { "model asset missing: ${TfliteSpeciesClassifier.MODEL_NAME}" }
            val buf = requireNotNull(loader.loadModelBuffer()) { "model mmap failed" }
            val interp = Interpreter(buf, Interpreter.Options().apply { setNumThreads(2) })
            val inT = interp.getInputTensor(0)
            val outT = interp.getOutputTensor(0)
            val inShape = inT.shape()
            val side = if (inShape.size >= 3) inShape[1] else 224
            val outSize = outT.shape().lastOrNull() ?: 0
            val inQ = inT.quantizationParams()
            val outQ = outT.quantizationParams()
            return InterpreterProbe(
                interpreter = interp,
                labels = loader.labels,
                inputSide = side,
                outputSize = outSize,
                inScale = inQ.scale,
                inZero = inQ.zeroPoint,
                outScale = outQ.scale,
                outZero = outQ.zeroPoint,
                outputIsUint8 = outT.dataType() == org.tensorflow.lite.DataType.UINT8,
            )
        }
    }
}
