package com.floracare.app.ui.feature.identify

import androidx.compose.runtime.Composable
import com.floracare.app.ui.components.PlaceholderScreen

@Composable
fun IdentifyScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Identify",
        subtitle = "CameraX preview → capture → top-3 species. Wire to SpeciesClassifier.",
        onBack = onBack,
    )
}
