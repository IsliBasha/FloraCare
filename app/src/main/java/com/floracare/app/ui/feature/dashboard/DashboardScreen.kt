package com.floracare.app.ui.feature.dashboard

import androidx.compose.runtime.Composable
import com.floracare.app.ui.components.PlaceholderScreen

@Composable
fun DashboardScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Dashboard",
        subtitle = "Streaks, watering consistency (Vico), health trend.",
        onBack = onBack,
    )
}
