package com.floracare.app.ui.feature.identify.components

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/**
 * Binds a [LifecycleCameraController] to the composable's lifecycle and exposes
 * a shutter callback. The caller is handed a [CameraShutter] that can be
 * invoked to trigger a capture.
 */
@Composable
fun rememberCameraShutter(): CameraShutter {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(context) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    DisposableEffect(controller, lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    return remember(controller) { CameraShutter(context, controller) }
}

@Composable
fun CameraPreview(
    shutter: CameraShutter,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                controller = shutter.controller
            }
        },
    )
}

class CameraShutter internal constructor(
    private val context: Context,
    internal val controller: LifecycleCameraController,
) {
    fun takePicture(onImage: ImageCapture.OnImageCapturedCallback) {
        val executor = ContextCompat.getMainExecutor(context)
        runCatching {
            controller.takePicture(executor, onImage)
        }.onFailure {
            onImage.onError(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, it.message.orEmpty(), it))
        }
    }
}
