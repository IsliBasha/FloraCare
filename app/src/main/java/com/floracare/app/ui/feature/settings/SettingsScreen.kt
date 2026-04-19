package com.floracare.app.ui.feature.settings

import androidx.compose.runtime.Composable
import com.floracare.app.ui.components.PlaceholderScreen

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Settings",
        subtitle = "Theme, notifications, units, about.",
        onBack = onBack,
    )
}
