package com.floracare.app.ui.feature.addplant

import androidx.compose.runtime.Composable
import com.floracare.app.ui.components.PlaceholderScreen

@Composable
fun AddPlantScreen(onIdentifyClick: () -> Unit, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Add plant",
        subtitle = "Two paths: identify by camera, or enter manually. UI in sprint 2.",
        onBack = onBack,
    )
}
