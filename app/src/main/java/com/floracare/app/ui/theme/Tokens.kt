package com.floracare.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class FloraSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    val gutter: Dp = 20.dp,
    val section: Dp = 64.dp,
)

@Immutable
data class FloraAccents(
    val terracotta: Color,
    val sage: Color,
    val warning: Color,
    val danger: Color,
    val paperGrain: Color,
)

val LocalFloraSpacing = staticCompositionLocalOf { FloraSpacing() }

val LocalFloraAccents = staticCompositionLocalOf {
    FloraAccents(
        terracotta = Terracotta,
        sage = SageMuted,
        warning = WarningAmber,
        danger = DangerClay,
        paperGrain = PaperCreamDim,
    )
}
