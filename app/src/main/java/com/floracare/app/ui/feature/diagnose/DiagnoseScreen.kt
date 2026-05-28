package com.floracare.app.ui.feature.diagnose

import android.Manifest
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floracare.app.data.ml.Prediction
import com.floracare.app.data.ml.toUprightBitmap
import com.floracare.app.ui.feature.identify.components.CameraPreview
import com.floracare.app.ui.feature.identify.components.rememberCameraShutter
import com.floracare.app.ui.theme.LocalFloraSpacing
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DiagnoseScreen(
    onBack: () -> Unit,
    onDone: () -> Unit = onBack,
    viewModel: DiagnoseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (granted) viewModel.onPermissionGranted() else viewModel.onPermissionDenied()
    }
    LaunchedEffect(cameraPermission.status) {
        when (val s = cameraPermission.status) {
            PermissionStatus.Granted -> viewModel.onPermissionGranted()
            is PermissionStatus.Denied -> {
                if (!s.shouldShowRationale && state is DiagnoseUiState.RequestPermission) {
                    cameraPermission.launchPermissionRequest()
                }
            }
        }
    }

    LaunchedEffect(state) {
        if (state is DiagnoseUiState.Saved) onDone()
    }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0),
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                DiagnoseUiState.RequestPermission,
                DiagnoseUiState.PermissionDenied,
                -> PermissionGate(
                    denied = state is DiagnoseUiState.PermissionDenied,
                    onGrant = { cameraPermission.launchPermissionRequest() },
                    onBack = onBack,
                )

                else -> CameraStage(
                    state = state,
                    onShutterStart = { viewModel.onCaptureStart() },
                    onImageCaptured = { proxy ->
                        runCatching { proxy.toUprightBitmap() }
                            .onSuccess(viewModel::onBitmapCaptured)
                            .onFailure { viewModel.onCaptureFailed(it.message ?: "Decode failed") }
                        proxy.close()
                    },
                    onCaptureFailed = { viewModel.onCaptureFailed(it) },
                    onRetake = viewModel::onRetake,
                    onBack = onBack,
                    onDone = onDone,
                    onPredictionSelected = viewModel::onPredictionSelected,
                    onSave = viewModel::onSave,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraStage(
    state: DiagnoseUiState,
    onShutterStart: () -> Unit,
    onImageCaptured: (ImageProxy) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onRetake: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onPredictionSelected: (Prediction) -> Unit,
    onSave: () -> Unit,
) {
    val shutter = rememberCameraShutter()
    val statusInset = WindowInsets.statusBars.asPaddingValues()
    val navInset = WindowInsets.navigationBars.asPaddingValues()

    CameraPreview(shutter = shutter, modifier = Modifier.fillMaxSize())

    IconButton(
        onClick = onBack,
        modifier = Modifier
            .padding(top = statusInset.calculateTopPadding() + 8.dp, start = 8.dp)
            .background(Color.Black.copy(alpha = 0.35f), CircleShape),
    ) {
        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = navInset.calculateBottomPadding()),
    ) {
        ShutterRow(
            enabled = state is DiagnoseUiState.Ready,
            busy = state is DiagnoseUiState.Capturing || state is DiagnoseUiState.Classifying,
            onShutter = {
                onShutterStart()
                shutter.takePicture(
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) = onImageCaptured(image)
                        override fun onError(exception: ImageCaptureException) {
                            onCaptureFailed(exception.message ?: "Capture failed")
                        }
                    },
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )
    }

    when (val current = state) {
        is DiagnoseUiState.Picker -> PickerSheet(
            predictions = current.predictions,
            lowConfidence = current.lowConfidence,
            onSelect = onPredictionSelected,
            onDismiss = onRetake,
        )
        is DiagnoseUiState.Result -> ResultSheet(
            result = current,
            onSave = onSave,
            onDone = onDone,
            onRetake = onRetake,
        )
        is DiagnoseUiState.Error -> ErrorOverlay(
            message = current.message,
            onDismiss = onRetake,
        )
        else -> Unit
    }
}

@Composable
private fun ShutterRow(
    enabled: Boolean,
    busy: Boolean,
    onShutter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            color = Color.White.copy(alpha = if (enabled) 1f else 0.6f),
            shape = CircleShape,
            modifier = Modifier.size(84.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (busy) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(36.dp))
                } else {
                    FilledTonalIconButton(
                        onClick = { if (enabled) onShutter() },
                        enabled = enabled,
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "Capture leaf")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(
    predictions: List<Prediction>,
    lowConfidence: Boolean,
    onSelect: (Prediction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacing = LocalFloraSpacing.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
        ) {
            Text(
                "Top 3 possibilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (lowConfidence) {
                Spacer(Modifier.height(8.dp))
                LowConfidenceBanner()
            }
            Spacer(Modifier.height(8.dp))
            predictions.forEachIndexed { index, prediction ->
                PredictionRow(prediction = prediction, onClick = { onSelect(prediction) })
                if (index < predictions.lastIndex) HorizontalDivider()
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Retake")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultSheet(
    result: DiagnoseUiState.Result,
    onSave: () -> Unit,
    onDone: () -> Unit,
    onRetake: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacing = LocalFloraSpacing.current
    ModalBottomSheet(
        onDismissRequest = onRetake,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.md, vertical = spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        result.diagnosisLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        confidenceBand(result.confidence),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(spacing.md))

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(spacing.md)) {
                    Text(
                        "Treatment",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        result.treatmentSuggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(spacing.lg))

            if (result.plantId != null) {
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Text("Save to plant")
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
            TextButton(
                onClick = onRetake,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = spacing.sm),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.size(4.dp))
                Text("Retake")
            }
        }
    }
}

@Composable
private fun PredictionRow(prediction: Prediction, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                prediction.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                confidenceBand(prediction.confidence),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClick) { Text("Select") }
    }
}

private fun confidenceBand(confidence: Float): String = when {
    confidence >= 0.6f -> "High confidence"
    confidence >= 0.35f -> "Possible match"
    else -> "Best guess"
}

@Composable
private fun LowConfidenceBanner() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                "Low confidence",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                "Try a closer shot of a single leaf in good lighting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.padding(32.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Something went wrong",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) { Text("OK") }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionGate(denied: Boolean, onGrant: () -> Unit, onBack: () -> Unit) {
    val spacing = LocalFloraSpacing.current
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(spacing.xl),
    ) {
        Text(
            if (denied) "Camera access is blocked" else "FloraCare needs your camera",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (denied) {
                "Enable camera access in system settings, then come back."
            } else {
                "To analyse a leaf for disease, we need to use the back camera."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) { Text("Grant camera") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Not now") }
    }
}
