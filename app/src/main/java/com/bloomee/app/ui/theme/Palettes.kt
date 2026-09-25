package com.bloomee.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.bloomee.app.shared.theme.ShadeValues
import com.bloomee.app.shared.theme.SurfaceTones
import com.bloomee.app.shared.theme.UiPalette

/**
 * One color family per theme role. [soft] fills containers in light mode and
 * tints on-container text in dark mode; [light] is the accent in dark mode;
 * [main] is the accent in light mode; [deep] anchors on-container text in
 * light mode and containers in dark mode.
 */
data class ShadeFamily(
    val soft: Color,
    val light: Color,
    val main: Color,
    val deep: Color
)

private fun ShadeValues.toFamily() =
    ShadeFamily(Color(soft), Color(light), Color(main), Color(deep))

/**
 * Per-palette canvas colors: [background] is the screen wash, [surface] is card
 * fill, [surfaceVariant] is quiet fills like text-field backgrounds. The *Dark
 * values give each family its own night canvas.
 */
data class SurfaceColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val backgroundDark: Color,
    val surfaceDark: Color,
    val surfaceVariantDark: Color
)

private fun SurfaceTones.toColors() = SurfaceColors(
    Color(background), Color(surface), Color(surfaceVariant),
    Color(backgroundDark), Color(surfaceDark), Color(surfaceVariantDark)
)

enum class ThemePalette(src: UiPalette) {
    ROSE(UiPalette.ROSE),
    LAVENDER(UiPalette.LAVENDER),
    OCEAN(UiPalette.OCEAN),
    FOREST(UiPalette.FOREST),
    SUNSET(UiPalette.SUNSET);

    val key: String = src.key
    val label: String = src.label
    val primary: ShadeFamily = src.primary.toFamily()
    val secondary: ShadeFamily = src.secondary.toFamily()
    val tertiary: ShadeFamily = src.tertiary.toFamily()
    val surfaces: SurfaceColors = src.surfaces.toColors()

    companion object {
        val DEFAULT = ROSE

        fun fromKey(key: String?): ThemePalette =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
