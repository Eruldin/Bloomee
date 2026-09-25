package com.bloomee.app.ui.theme

import androidx.compose.ui.graphics.Color

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

private val RoseFamily = ShadeFamily(Rose100, Rose400, Rose500, Rose700)
private val PlumFamily = ShadeFamily(Plum200, Plum200, Plum500, Plum700)
private val AquaFamily = ShadeFamily(Aqua200, Aqua200, Aqua500, Aqua700)
private val ForestFamily = ShadeFamily(Forest200, Forest400, Forest500, Forest700)
private val SunsetFamily = ShadeFamily(Sunset200, Sunset400, Sunset500, Sunset700)

enum class ThemePalette(
    val key: String,
    val label: String,
    val primary: ShadeFamily,
    val secondary: ShadeFamily,
    val tertiary: ShadeFamily
) {
    ROSE("rose", "Gül", RoseFamily, PlumFamily, AquaFamily),
    LAVENDER("lavender", "Lavanta", PlumFamily, RoseFamily, AquaFamily),
    OCEAN("ocean", "Okyanus", AquaFamily, PlumFamily, RoseFamily),
    FOREST("forest", "Orman", ForestFamily, AquaFamily, RoseFamily),
    SUNSET("sunset", "Gün batımı", SunsetFamily, RoseFamily, AquaFamily);

    companion object {
        val DEFAULT = ROSE

        fun fromKey(key: String?): ThemePalette =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
