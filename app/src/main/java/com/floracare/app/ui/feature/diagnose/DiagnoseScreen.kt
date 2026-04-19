package com.floracare.app.ui.feature.diagnose

import androidx.compose.runtime.Composable
import com.floracare.app.ui.components.PlaceholderScreen

@Composable
fun DiagnoseScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Diagnose",
        subtitle = "Leaf capture → DiseaseClassifier → treatment card.",
        onBack = onBack,
    )
}
