package com.floracare.app.data.ml

import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer

/**
 * Thin wrapper around a TFLite [Interpreter] that attempts GPU acceleration
 * when the device supports it and transparently falls back to CPU. Callers
 * supply normalised float input in `[0, 1]` (from [normalizePixels] /
 * [preprocessForModel]); the runner introspects the model at load time and
 * writes either a `float32` or `uint8` input tensor accordingly. The output
 * is always returned as a `FloatArray`, dequantised when necessary.
 *
 * This lets the same code path serve a pure float32 model (e.g. MobileNetV3
 * exported with float weights) and a quantised `uint8` model (e.g. the AIY
 * Vision Plants V1 TFLite bundle) without the caller caring which is shipped
 * in `assets/ml/`.
 *
 * The runner is `Closeable` and must be closed on dispose so the native
 * interpreter handle and the GPU delegate (if any) are released.
 */
class TfliteRunner private constructor(
    private val interpreter: Interpreter,
    private val delegate: GpuDelegate?,
    val outputSize: Int,
    private val inputSide: Int,
    private val inputIsUint8: Boolean,
    private val outputIsUint8: Boolean,
    private val outputScale: Float,
    private val outputZeroPoint: Int,
    /** Apply ImageNet mean/std to float32 inputs before inference. */
    private val useImageNetNorm: Boolean,
    /** Apply temperature-scaled softmax to float32 outputs after inference. */
    private val useSoftmax: Boolean,
) : Closeable {

    fun run(input: FloatArray): FloatArray {
        val expected = inputSide * inputSide * 3
        require(input.size == expected) {
            "input length ${input.size} does not match model (expected $expected)"
        }

        val inputBuffer = if (inputIsUint8) allocUint8Input(input) else allocFloat32Input(input)

        if (outputIsUint8) {
            val raw = Array(1) { ByteArray(outputSize) }
            interpreter.run(inputBuffer, raw)
            // Uint8 models already have softmax baked in before quantisation —
            // dequantise only; do not apply temperature-softmax a second time.
            return dequantise(raw[0], outputScale, outputZeroPoint)
        }

        val floatOut = Array(1) { FloatArray(outputSize) }
        interpreter.run(inputBuffer, floatOut)
        // Float32 models output raw logits; convert to calibrated probabilities.
        return if (useSoftmax) softmax(floatOut[0]) else floatOut[0]
    }

    private fun allocFloat32Input(input: FloatArray): ByteBuffer {
        // Float32 models expect ImageNet-normalised input; apply before packing.
        val prepared = if (useImageNetNorm) input.applyImageNetNorm() else input
        val buffer = ByteBuffer
            .allocateDirect(prepared.size * 4)
            .order(ByteOrder.nativeOrder())
        for (v in prepared) buffer.putFloat(v)
        buffer.rewind()
        return buffer
    }

    /** Quantised models expect `[0, 255]` uint8 — rescale the `[0, 1]` floats. */
    private fun allocUint8Input(input: FloatArray): ByteBuffer {
        val buffer = ByteBuffer
            .allocateDirect(input.size)
            .order(ByteOrder.nativeOrder())
        for (v in input) {
            val clamped = (v * 255f).toInt().coerceIn(0, 255)
            buffer.put(clamped.toByte())
        }
        buffer.rewind()
        return buffer
    }

    private fun dequantise(raw: ByteArray, scale: Float, zeroPoint: Int): FloatArray {
        val out = FloatArray(raw.size)
        // Use a sensible default when scale is 0 so we still get distinct logits.
        val effectiveScale = if (scale == 0f) 1f / 255f else scale
        for (i in raw.indices) {
            val u = raw[i].toInt() and 0xFF
            out[i] = (u - zeroPoint) * effectiveScale
        }
        return out
    }

    override fun close() {
        runCatching { interpreter.close() }
        runCatching { delegate?.close() }
    }

    companion object {
        private const val TAG = "TfliteRunner"

        /**
         * Builds an interpreter for [modelBuffer]. Tries the GPU delegate first when
         * the device reports support; on any failure, falls back to a plain CPU
         * interpreter so inference never hard-fails because of vendor GPU quirks.
         */
        fun create(modelBuffer: MappedByteBuffer, numCpuThreads: Int = 2): TfliteRunner {
            val gpuAttempt = runCatching {
                val compat = CompatibilityList()
                if (!compat.isDelegateSupportedOnThisDevice) {
                    compat.close()
                    return@runCatching null
                }
                compat.close()
                val delegate = GpuDelegate()
                val options = Interpreter.Options().apply { addDelegate(delegate) }
                val interp = Interpreter(modelBuffer, options)
                interp to delegate
            }.onFailure { Log.w(TAG, "GPU delegate init failed, falling back to CPU", it) }
                .getOrNull()

            val (interpreter, delegate) = gpuAttempt ?: run {
                val options = Interpreter.Options().apply { setNumThreads(numCpuThreads) }
                Interpreter(modelBuffer, options) to null
            }

            val inputTensor = interpreter.getInputTensor(0)
            val outputTensor = interpreter.getOutputTensor(0)

            val inputShape = inputTensor.shape()
            val inputSide = if (inputShape.size >= 3) inputShape[1] else 224
            val outputSize = outputTensor.shape().lastOrNull() ?: 0

            val inputIsUint8 = inputTensor.dataType() == DataType.UINT8
            val outputIsUint8 = outputTensor.dataType() == DataType.UINT8
            val inQp = inputTensor.quantizationParams()
            val outQp = outputTensor.quantizationParams()

            // Auto-detect calibration strategy from tensor dtypes:
            // float32 input → apply ImageNet mean/std (models fine-tuned from
            // ImageNet-pretrained backbones expect this exact preprocessing).
            // float32 output → apply temperature-scaled softmax (raw logits
            // need to become a proper probability distribution for the UI).
            // uint8 paths skip both — quantisation params handle calibration.
            val useImageNetNorm = !inputIsUint8
            val useSoftmax = !outputIsUint8

            Log.i(
                TAG,
                "TFLite ready: inputSide=$inputSide, outputSize=$outputSize, " +
                    "inputUint8=$inputIsUint8 (inScale=${inQp.scale}, inZero=${inQp.zeroPoint}), " +
                    "outputUint8=$outputIsUint8 (outScale=${outQp.scale}, outZero=${outQp.zeroPoint}), " +
                    "imageNetNorm=$useImageNetNorm, softmax=$useSoftmax",
            )

            return TfliteRunner(
                interpreter = interpreter,
                delegate = delegate,
                outputSize = outputSize,
                inputSide = inputSide,
                inputIsUint8 = inputIsUint8,
                outputIsUint8 = outputIsUint8,
                outputScale = outQp.scale,
                outputZeroPoint = outQp.zeroPoint,
                useImageNetNorm = useImageNetNorm,
                useSoftmax = useSoftmax,
            )
        }
    }
}
