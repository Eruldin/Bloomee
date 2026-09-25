package com.bloomee.app.desktop

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloomee.app.shared.theme.BloomeeColors
import com.bloomee.app.shared.theme.ShadeValues
import com.bloomee.app.shared.theme.UiPalette

private fun ShadeValues.toFamily() =
    ShadeFamily(Color(soft), Color(light), Color(main), Color(deep))

data class ShadeFamily(val soft: Color, val light: Color, val main: Color, val deep: Color)

private val White = Color(BloomeeColors.PURE_WHITE)
private val Sand100 = Color(BloomeeColors.SAND_100)
private val Ink900 = Color(BloomeeColors.INK_900)
private val Ink700 = Color(BloomeeColors.INK_700)
private val Rose50 = Color(BloomeeColors.ROSE_50)
private val SurfaceDark = Color(BloomeeColors.SURFACE_DARK)
private val SurfaceDarkElevated = Color(BloomeeColors.SURFACE_DARK_ELEVATED)

val FlowSpotting = Color(BloomeeColors.FLOW_SPOTTING)
val FlowLight = Color(BloomeeColors.FLOW_LIGHT)
val FlowMedium = Color(BloomeeColors.FLOW_MEDIUM)
val FlowHeavy = Color(BloomeeColors.FLOW_HEAVY)
val FertilePeak = Color(BloomeeColors.FERTILE_PEAK)
val WarningAmber = Color(BloomeeColors.WARNING_AMBER)

private fun lightScheme(palette: UiPalette): androidx.compose.material3.ColorScheme {
    val primary = palette.primary.toFamily()
    val secondary = palette.secondary.toFamily()
    val tertiary = palette.tertiary.toFamily()
    return lightColorScheme(
        primary = primary.main,
        onPrimary = White,
        primaryContainer = primary.soft,
        onPrimaryContainer = primary.deep,
        secondary = secondary.main,
        onSecondary = White,
        secondaryContainer = secondary.soft,
        onSecondaryContainer = secondary.deep,
        tertiary = tertiary.main,
        onTertiary = White,
        tertiaryContainer = tertiary.soft,
        onTertiaryContainer = tertiary.deep,
        background = Sand100,
        onBackground = Ink900,
        surface = White,
        onSurface = Ink900,
        surfaceVariant = Rose50,
        onSurfaceVariant = Ink700,
        outline = primary.light,
        outlineVariant = primary.soft
    )
}

private fun darkScheme(palette: UiPalette): androidx.compose.material3.ColorScheme {
    val primary = palette.primary.toFamily()
    val secondary = palette.secondary.toFamily()
    val tertiary = palette.tertiary.toFamily()
    return darkColorScheme(
        primary = primary.light,
        onPrimary = Ink900,
        primaryContainer = primary.deep,
        onPrimaryContainer = primary.soft,
        secondary = secondary.light,
        onSecondary = secondary.deep,
        secondaryContainer = secondary.deep,
        onSecondaryContainer = secondary.light,
        tertiary = tertiary.light,
        onTertiary = tertiary.deep,
        tertiaryContainer = tertiary.deep,
        onTertiaryContainer = tertiary.light,
        background = SurfaceDark,
        onBackground = Rose50,
        surface = SurfaceDarkElevated,
        onSurface = Rose50,
        surfaceVariant = SurfaceDarkElevated,
        onSurfaceVariant = secondary.light,
        outline = secondary.main,
        outlineVariant = secondary.deep
    )
}

private val BloomeeTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)

private val BloomeeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BloomeeDesktopTheme(
    paletteName: String,
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val palette = UiPalette.fromKey(paletteName)
    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme(palette) else lightScheme(palette),
        typography = BloomeeTypography,
        shapes = BloomeeShapes,
        content = content
    )
}
