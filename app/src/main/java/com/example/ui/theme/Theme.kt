package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuraFitColorScheme = darkColorScheme(
    primary = NeonBlue,
    onPrimary = Color.Black,
    secondary = TextGray,
    onSecondary = Color.White,
    tertiary = OrangeWarm,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = SurfaceCard,
    onSurface = TextWhite,
    surfaceVariant = SurfaceCardBorder,
    onSurfaceVariant = TextGray,
    error = FailureMagenta,
    onError = TextWhite
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraFitColorScheme,
        typography = Typography,
        content = content
    )
}
