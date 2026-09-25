package com.bloomee.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import com.bloomee.app.shared.theme.BloomeeColors
import com.bloomee.app.shared.theme.UiPalette

/**
 * Wear Compose has its own smaller color model; map the shared palette onto it.
 * Watch faces are always rendered on a dark background for battery and legibility.
 */
private fun colorsFor(palette: UiPalette) = Colors(
    primary = Color(palette.primary.light),
    primaryVariant = Color(palette.primary.deep),
    secondary = Color(palette.secondary.light),
    secondaryVariant = Color(palette.secondary.deep),
    background = Color(BloomeeColors.SURFACE_DARK),
    surface = Color(BloomeeColors.SURFACE_DARK_ELEVATED),
    error = Color(BloomeeColors.WARNING_AMBER),
    onPrimary = Color(BloomeeColors.INK_900),
    onSecondary = Color(BloomeeColors.PURE_WHITE),
    onBackground = Color(BloomeeColors.ROSE_50),
    onSurface = Color(BloomeeColors.ROSE_50),
    onError = Color(BloomeeColors.INK_900)
)

@Composable
fun BloomeeWearTheme(paletteName: String, content: @Composable () -> Unit) {
    MaterialTheme(
        colors = colorsFor(UiPalette.fromKey(paletteName)),
        content = content
    )
}
