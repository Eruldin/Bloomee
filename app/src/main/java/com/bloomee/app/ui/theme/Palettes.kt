package com.bloomee.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.bloomee.app.shared.theme.ShadeValues
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

    companion object {
        val DEFAULT = ROSE

        fun fromKey(key: String?): ThemePalette =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
