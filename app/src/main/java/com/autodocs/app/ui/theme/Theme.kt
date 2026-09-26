package com.autodocs.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

private val AutoDocsColorScheme = darkColorScheme(
    background = BackgroundBottom,
    surface = BackgroundBottom,
    primary = Accent,
    onPrimary = OnAccent,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    secondary = TextSecondary,
    onSecondary = OnAccent,
    error = StatusOverdue
)

/**
 * Застосунок лише темний — жодного світлого варіанту й перемикача немає.
 */
@Composable
fun AutoDocsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AutoDocsColorScheme,
        typography = AutoDocsTypography,
        content = {
            Box(modifier = Modifier.fillMaxSize().background(AppRadialBackgroundBrush())) {
                content()
            }
        }
    )
}

/** Радіальний фон екрана: #101B2D у центрі → #040508 по краях. */
fun AppRadialBackgroundBrush(): Brush = Brush.radialGradient(
    colors = listOf(BackgroundTop, BackgroundBottom),
    center = Offset.Unspecified,
    radius = 1200f
)
