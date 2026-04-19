package com.floracare.app.ui.feature.journal

import androidx.compose.runtime.Composable
import com.floracare.app.ui.components.PlaceholderScreen

@Composable
fun JournalScreen(plantId: String, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Journal",
        subtitle = "Photo timeline for plant $plantId — full-screen pager coming next.",
        onBack = onBack,
    )
}
