package com.floracare.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightFloraColors = lightColorScheme(
    primary = ForestDeep,
    onPrimary = PaperCream,
    primaryContainer = SageMuted,
    onPrimaryContainer = ForestDeep,
    secondary = SageDeep,
    onSecondary = PaperCream,
    tertiary = Terracotta,
    onTertiary = PaperCream,
    background = PaperCream,
    onBackground = InkNearBlack,
    surface = PaperCream,
    onSurface = InkSoft,
    surfaceVariant = PaperCreamDim,
    onSurfaceVariant = InkSoft,
    error = DangerClay,
    onError = PaperCream,
)

private val DarkFloraColors = darkColorScheme(
    primary = SageMuted,
    onPrimary = CharcoalDark,
    primaryContainer = ForestDeep,
    onPrimaryContainer = SageMuted,
    secondary = SageDeep,
    onSecondary = CharcoalDark,
    tertiary = Terracotta,
    onTertiary = CharcoalDark,
    background = CharcoalDark,
    onBackground = PaperCream,
    surface = CharcoalDark,
    onSurface = PaperCreamDim,
    surfaceVariant = InkSoft,
    onSurfaceVariant = PaperCreamDim,
    error = DangerClay,
    onError = PaperCream,
)

@Composable
fun FloraCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkFloraColors else LightFloraColors
    CompositionLocalProvider(
        LocalFloraSpacing provides FloraSpacing(),
        LocalFloraAccents provides FloraAccents(
            terracotta = Terracotta,
            sage = if (darkTheme) SageMuted else SageDeep,
            warning = WarningAmber,
            danger = DangerClay,
            paperGrain = if (darkTheme) InkSoft else PaperCreamDim,
        ),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = FloraCareTypography,
            shapes = FloraCareShapes,
            content = content,
        )
    }
}
