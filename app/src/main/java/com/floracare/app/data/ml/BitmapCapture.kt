package com.floracare.app.data.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

/**
 * Converts a JPEG-encoded [ImageProxy] (as produced by
 * `ImageCapture.takePicture(OnImageCapturedCallback)`) into an upright [Bitmap]
 * with the correct orientation applied.
 *
 * Only the first plane is read — `ImageCapture` uses `OutputFormat.JPEG` by
 * default so there's exactly one plane containing the full encoded payload.
 */
fun ImageProxy.toUprightBitmap(): Bitmap {
    val plane = planes.firstOrNull()
        ?: error("ImageProxy has no planes")
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: error("Failed to decode capture bytes")

    val degrees = imageInfo.rotationDegrees
    return if (degrees == 0) raw else raw.rotated(degrees.toFloat())
}

private fun Bitmap.rotated(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    val rotated = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (rotated !== this) recycle()
    return rotated
}
