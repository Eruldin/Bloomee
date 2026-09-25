package com.bloomee.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.bloomee.app.domain.model.ThemeMode

private fun lightScheme(palette: ThemePalette) = lightColorScheme(
    primary = palette.primary.main,
    onPrimary = PureWhite,
    primaryContainer = palette.primary.soft,
    onPrimaryContainer = palette.primary.deep,
    secondary = palette.secondary.main,
    onSecondary = PureWhite,
    secondaryContainer = palette.secondary.soft,
    onSecondaryContainer = palette.secondary.deep,
    tertiary = palette.tertiary.main,
    onTertiary = PureWhite,
    tertiaryContainer = palette.tertiary.soft,
    onTertiaryContainer = palette.tertiary.deep,
    background = palette.surfaces.background,
    onBackground = Ink900,
    surface = palette.surfaces.surface,
    onSurface = Ink900,
    surfaceVariant = palette.surfaces.surfaceVariant,
    onSurfaceVariant = Ink700,
    outline = palette.primary.light,
    outlineVariant = palette.primary.soft
)

private fun darkScheme(palette: ThemePalette) = darkColorScheme(
    primary = palette.primary.light,
    onPrimary = Ink900,
    primaryContainer = palette.primary.deep,
    onPrimaryContainer = palette.primary.soft,
    secondary = palette.secondary.light,
    onSecondary = palette.secondary.deep,
    secondaryContainer = palette.secondary.deep,
    onSecondaryContainer = palette.secondary.light,
    tertiary = palette.tertiary.light,
    onTertiary = palette.tertiary.deep,
    tertiaryContainer = palette.tertiary.deep,
    onTertiaryContainer = palette.tertiary.light,
    background = palette.surfaces.backgroundDark,
    onBackground = Ink50,
    surface = palette.surfaces.surfaceDark,
    onSurface = Ink50,
    surfaceVariant = palette.surfaces.surfaceVariantDark,
    onSurfaceVariant = palette.secondary.light,
    outline = palette.secondary.main,
    outlineVariant = palette.secondary.deep
)

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
fun BloomeeTheme(
    paletteName: String = ThemePalette.DEFAULT.key,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val palette = ThemePalette.fromKey(paletteName)
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) darkScheme(palette) else lightScheme(palette)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BloomeeTypography,
        shapes = BloomeeShapes,
        content = content
    )
}
