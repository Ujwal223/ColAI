package com.ujwal.colai.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalContrastLevel = staticCompositionLocalOf { "relaxed" }

@Composable
fun ColAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    contrastLevel: String = "relaxed",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isHighContrast = contrastLevel == "high"

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ColAIDarkColorScheme
        else -> ColAILightColorScheme
    }

    val colorScheme = if (isHighContrast) {
        if (darkTheme) {
            baseColorScheme.copy(
                background = Color(0xFF000000), // Pure OLED black
                surface = Color(0xFF111114),
                surfaceVariant = Color(0xFF18181C),
                outline = Color(0x66FFFFFF),
                outlineVariant = Color(0x33FFFFFF),
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            baseColorScheme.copy(
                background = Color(0xFFFFFFFF), // Crisp pure white
                surface = Color(0xFFF2F2F7),
                surfaceVariant = Color(0xFFE5E5EA),
                outline = Color(0x4D000000),
                outlineVariant = Color(0x26000000),
                onBackground = Color.Black,
                onSurface = Color.Black
            )
        }
    } else {
        baseColorScheme
    }

    val baseGlass = if (darkTheme) DarkGlassColors else LightGlassColors
    val glassColors = if (isHighContrast) {
        if (darkTheme) {
            baseGlass.copy(
                surface = Color(0xFF121215),
                surfaceElevated = Color(0xFF1C1C20),
                borderStart = Color(0x66FFFFFF),
                borderEnd = Color(0x33FFFFFF),
                highlight = Color(0x40FFFFFF)
            )
        } else {
            baseGlass.copy(
                surface = Color(0xFFFFFFFF),
                surfaceElevated = Color(0xFFEBEBF0),
                borderStart = Color(0x33000000),
                borderEnd = Color(0x1A000000),
                highlight = Color(0x60FFFFFF)
            )
        }
    } else {
        baseGlass
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalGlassColors provides glassColors,
        LocalContrastLevel provides contrastLevel
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = ColAIShapes,
            typography = ColAITypography,
            content = content
        )
    }
}

object ColAITheme {
    val glassColors: GlassColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current

    val contrastLevel: String
        @Composable
        @ReadOnlyComposable
        get() = LocalContrastLevel.current

    val isHighContrast: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalContrastLevel.current == "high"

    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes
}
