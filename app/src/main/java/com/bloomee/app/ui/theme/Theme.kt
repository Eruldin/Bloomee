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

private val LightColors = lightColorScheme(
    primary = Rose500,
    onPrimary = PureWhite,
    primaryContainer = Rose100,
    onPrimaryContainer = Rose700,
    secondary = Plum500,
    onSecondary = PureWhite,
    secondaryContainer = Plum200,
    onSecondaryContainer = Plum700,
    tertiary = Aqua500,
    onTertiary = PureWhite,
    tertiaryContainer = Aqua200,
    onTertiaryContainer = Aqua700,
    background = Sand100,
    onBackground = Ink900,
    surface = PureWhite,
    onSurface = Ink900,
    surfaceVariant = Rose50,
    onSurfaceVariant = Ink700,
    outline = Rose200,
    outlineVariant = Rose100
)

private val DarkColors = darkColorScheme(
    primary = Rose400,
    onPrimary = Ink900,
    primaryContainer = Rose700,
    onPrimaryContainer = Rose100,
    secondary = Plum200,
    onSecondary = Plum700,
    secondaryContainer = Plum700,
    onSecondaryContainer = Plum200,
    tertiary = Aqua200,
    onTertiary = Aqua700,
    tertiaryContainer = Aqua700,
    onTertiaryContainer = Aqua200,
    background = SurfaceDark,
    onBackground = Rose50,
    surface = SurfaceDarkElevated,
    onSurface = Rose50,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = Rose200,
    outline = Plum500,
    outlineVariant = Plum700
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
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
