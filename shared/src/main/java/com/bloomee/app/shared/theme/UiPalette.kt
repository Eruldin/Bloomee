package com.bloomee.app.shared.theme

/**
 * Platform-neutral palette values shared by the Android, desktop and Wear apps.
 * ARGB hex values; each platform wraps them into its own Compose color types.
 */
object BloomeeColors {
    const val ROSE_50 = 0xFFFFF1F4L
    const val ROSE_100 = 0xFFFFE0E7L
    const val ROSE_400 = 0xFFF2708FL
    const val ROSE_500 = 0xFFD94E73L
    const val ROSE_700 = 0xFFA3204AL

    const val PLUM_200 = 0xFFD9C4EAL
    const val PLUM_500 = 0xFF7A5AA0L
    const val PLUM_700 = 0xFF4B3268L

    const val AQUA_200 = 0xFFBCE6F2L
    const val AQUA_500 = 0xFF3AA6C4L
    const val AQUA_700 = 0xFF1C6C85L

    const val FOREST_200 = 0xFFBFE3D0L
    const val FOREST_400 = 0xFF7CC29DL
    const val FOREST_500 = 0xFF4E9A72L
    const val FOREST_700 = 0xFF2E6B4CL

    const val SUNSET_200 = 0xFFFCD7C5L
    const val SUNSET_400 = 0xFFEE9B72L
    const val SUNSET_500 = 0xFFD97B57L
    const val SUNSET_700 = 0xFF9C4A2DL

    const val PURE_WHITE = 0xFFFFFFFFL
    const val SAND_100 = 0xFFFDF6F0L
    const val INK_50 = 0xFFF5F2F4L
    const val INK_900 = 0xFF211B22L
    const val INK_700 = 0xFF4A424CL
    const val INK_500 = 0xFF7A727CL

    const val SURFACE_DARK = 0xFF1A151CL
    const val SURFACE_DARK_ELEVATED = 0xFF241E27L

    const val FLOW_SPOTTING = 0xFFF7C9D4L
    const val FLOW_LIGHT = 0xFFEF9BB1L
    const val FLOW_MEDIUM = 0xFFDD5C81L
    const val FLOW_HEAVY = 0xFFB02A50L
    const val FERTILE_PEAK = 0xFF6BB88FL
    const val WARNING_AMBER = 0xFFC98A00L
}

data class ShadeValues(
    val soft: Long,
    val light: Long,
    val main: Long,
    val deep: Long
)

private val RoseShades = ShadeValues(0xFFFFE0E7, 0xFFF2708F, 0xFFD94E73, 0xFFA3204A)
private val PlumShades = ShadeValues(0xFFD9C4EA, 0xFFD9C4EA, 0xFF7A5AA0, 0xFF4B3268)
private val AquaShades = ShadeValues(0xFFBCE6F2, 0xFFBCE6F2, 0xFF3AA6C4, 0xFF1C6C85)
private val ForestShades = ShadeValues(0xFFBFE3D0, 0xFF7CC29D, 0xFF4E9A72, 0xFF2E6B4C)
private val SunsetShades = ShadeValues(0xFFFCD7C5, 0xFFEE9B72, 0xFFD97B57, 0xFF9C4A2D)

/**
 * Per-palette surface colors so each theme has its own tinted canvas instead of
 * one shared white scheme. Light tones are very light washes of the family color;
 * dark tones are darks with a subtle hue cast of the same family.
 */
data class SurfaceTones(
    val background: Long,
    val surface: Long,
    val surfaceVariant: Long,
    val backgroundDark: Long,
    val surfaceDark: Long,
    val surfaceVariantDark: Long
)

private val RoseSurfaces = SurfaceTones(0xFFFAEBEF, 0xFFFFF9FA, 0xFFF5DDE2, 0xFF1E1418, 0xFF281C22, 0xFF382733)
private val PlumSurfaces = SurfaceTones(0xFFF1EAF7, 0xFFFAF7FD, 0xFFE8DCF3, 0xFF1A1424, 0xFF241E31, 0xFF322A45)
private val AquaSurfaces = SurfaceTones(0xFFEAF5F8, 0xFFF7FBFD, 0xFFDAEEF3, 0xFF121C21, 0xFF1A2830, 0xFF243844)
private val ForestSurfaces = SurfaceTones(0xFFECF5EF, 0xFFF7FBF9, 0xFFDFEFE5, 0xFF131B16, 0xFF1B271F, 0xFF26372C)
private val SunsetSurfaces = SurfaceTones(0xFFF9F0EA, 0xFFFCF9F5, 0xFFF4E4D9, 0xFF1E1611, 0xFF292019, 0xFF3A2D22)

enum class UiPalette(
    val key: String,
    val label: String,
    val primary: ShadeValues,
    val secondary: ShadeValues,
    val tertiary: ShadeValues,
    val surfaces: SurfaceTones
) {
    ROSE("rose", "Gül", RoseShades, RoseShades, RoseShades, RoseSurfaces),
    LAVENDER("lavender", "Lavanta", PlumShades, PlumShades, PlumShades, PlumSurfaces),
    OCEAN("ocean", "Okyanus", AquaShades, AquaShades, AquaShades, AquaSurfaces),
    FOREST("forest", "Orman", ForestShades, ForestShades, ForestShades, ForestSurfaces),
    SUNSET("sunset", "Gün batımı", SunsetShades, SunsetShades, SunsetShades, SunsetSurfaces);

    companion object {
        val DEFAULT = ROSE

        fun fromKey(key: String?): UiPalette =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
