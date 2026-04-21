package com.floracare.app.ui.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.floracare.app.ui.theme.LocalFloraSpacing

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val notificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionResult(OnboardingPermission.Notifications, granted)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionResult(OnboardingPermission.Camera, granted)
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionResult(OnboardingPermission.Location, granted)
    }

    // Sync already-granted permissions on first composition so users who returned
    // to onboarding after granting via settings see the correct state.
    LaunchedEffect(Unit) {
        if (context.isGranted(notificationsPermission())) {
            viewModel.onPermissionResult(OnboardingPermission.Notifications, true)
        }
        if (context.isGranted(Manifest.permission.CAMERA)) {
            viewModel.onPermissionResult(OnboardingPermission.Camera, true)
        }
        if (context.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            viewModel.onPermissionResult(OnboardingPermission.Location, true)
        }
    }

    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        val spacing = LocalFloraSpacing.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.lg, vertical = spacing.xl),
        ) {
            StepDots(step = state.step, modifier = Modifier.padding(bottom = spacing.lg))
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboarding-step",
                ) { step ->
                    when (step) {
                        OnboardingStep.Welcome -> WelcomePane()
                        OnboardingStep.Permissions -> PermissionsPane(
                            state = state,
                            onAskNotifications = {
                                val perm = notificationsPermission()
                                if (perm == null) {
                                    // Pre-33: no runtime prompt needed.
                                    viewModel.onPermissionResult(
                                        OnboardingPermission.Notifications,
                                        true,
                                    )
                                } else {
                                    notificationsLauncher.launch(perm)
                                }
                            },
                            onAskCamera = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                            onAskLocation = {
                                locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            },
                        )
                        OnboardingStep.Ready -> ReadyPane()
                    }
                }
            }
            FooterButtons(
                step = state.step,
                onBack = { viewModel.onBack() },
                onNext = { viewModel.onNext() },
                onFinish = { viewModel.onFinish() },
            )
        }
    }
}

@Composable
private fun StepDots(step: OnboardingStep, modifier: Modifier = Modifier) {
    val index = when (step) {
        OnboardingStep.Welcome -> 0
        OnboardingStep.Permissions -> 1
        OnboardingStep.Ready -> 2
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(3) { i ->
            val active = i == index
            Box(
                modifier = Modifier
                    .size(if (active) 10.dp else 8.dp)
                    .padding(horizontal = 0.dp)
                    .background(
                        color = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(50),
                    ),
            )
            if (i < 2) Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun WelcomePane() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Welcome to FloraCare",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Your plants, remembered — and cared for on time. " +
                "Adaptive reminders, one-tap diagnosis, a quiet garden journal.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionsPane(
    state: OnboardingUiState,
    onAskNotifications: () -> Unit,
    onAskCamera: () -> Unit,
    onAskLocation: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "A few permissions",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Everything here is optional — skip what you don't want and you can change it later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        PermissionRow(
            icon = Icons.Outlined.Notifications,
            title = "Reminders",
            body = "Gentle pings when a plant needs water, light, or a check-in.",
            granted = state.notificationsGranted,
            asked = state.notificationsAsked,
            onAsk = onAskNotifications,
        )
        Spacer(Modifier.height(16.dp))
        PermissionRow(
            icon = Icons.Outlined.CameraAlt,
            title = "Camera",
            body = "Identify species and diagnose leaf trouble from a photo.",
            granted = state.cameraGranted,
            asked = state.cameraAsked,
            onAsk = onAskCamera,
        )
        Spacer(Modifier.height(16.dp))
        PermissionRow(
            icon = Icons.Outlined.Place,
            title = "Location (rough)",
            body = "Use local weather to adjust watering when it's hot or raining.",
            granted = state.locationGranted,
            asked = state.locationAsked,
            onAsk = onAskLocation,
        )
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    body: String,
    granted: Boolean,
    asked: Boolean,
    onAsk: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(12.dp))
            when {
                granted -> Icon(
                    Icons.Outlined.Check,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                )
                else -> Button(
                    onClick = onAsk,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) { Text(if (asked) "Retry" else "Allow") }
            }
        }
    }
}

@Composable
private fun ReadyPane() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "You're set",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Time to meet your garden. We've seeded a few starter plants — replace " +
                "them with your own as you go.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FooterButtons(
    step: OnboardingStep,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (step != OnboardingStep.Welcome) {
            TextButton(onClick = onBack) { Text("Back") }
        } else {
            Spacer(Modifier.size(1.dp))
        }
        Spacer(Modifier.weight(1f))
        when (step) {
            OnboardingStep.Welcome -> Button(onClick = onNext) { Text("Get started") }
            OnboardingStep.Permissions -> Button(onClick = onNext) { Text("Continue") }
            OnboardingStep.Ready -> Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) { Text("Enter garden") }
        }
    }
}

/** Returns the runtime permission string for notifications on API 33+, or null pre-33. */
private fun notificationsPermission(): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

private fun android.content.Context.isGranted(permission: String?): Boolean {
    if (permission == null) return true
    return ContextCompat.checkSelfPermission(this, permission) ==
        PackageManager.PERMISSION_GRANTED
}
