package com.ujwal.colai.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==============================================================================
// 1. OLED Base & System Canvas Colors
// ==============================================================================
val PitchBlack = Color(0xFF050508)          // Pitch OLED Black (#050508)
val DarkSurface = Color(0xFF0D0E15)         // Deep Void Surface
val DarkSurfaceElevated = Color(0xFF141622) // Elevated Surface
val DarkSurfaceContainer = Color(0xFF1C1E2D)// Card Container

val LuminousMist = Color(0xFFF6F7FA)        // Luminous Mist (#F6F7FA)
val LightSurface = Color(0xFFFFFFFF)        // Pure White Surface
val LightSurfaceElevated = Color(0xFFF0F2F7)// Elevated White-Grey
val LightSurfaceContainer = Color(0xFFE8EBF2)// Card Container

// ==============================================================================
// 2. Official Apple iOS System Card Surfaces & Specular Lighting
// ==============================================================================
val GlassDarkSurface = Color(0xFF1C1C1E)          // Apple Secondary System Grouped Background
val GlassDarkSurfaceElevated = Color(0xFF2C2C2E)  // Apple Tertiary System Grouped Background
val GlassDarkBorderStart = Color(0x38FFFFFF)      // Subtle Top-Left Specular Refraction
val GlassDarkBorderEnd = Color(0x18FFFFFF)        // Subtle Bottom-Right Specular Exit
val GlassDarkHighlight = Color(0x20FFFFFF)        // Internal Ambient Highlight
val GlassDarkShadow = Color(0x66000000)           // 40% Ambient Drop Shadow

val GlassLightSurface = Color(0xFFFFFFFF)         // Apple Pure White Card Surface
val GlassLightSurfaceElevated = Color(0xFFF2F2F7) // Apple System Grouped Elevated
val GlassLightBorderStart = Color(0x18000000)     // Hairline Light Border
val GlassLightBorderEnd = Color(0x0C000000)       // Ambient Rim
val GlassLightHighlight = Color(0x40FFFFFF)       // Internal Ambient Highlight
val GlassLightShadow = Color(0x14000000)          // Subtle Apple Drop Shadow

// ==============================================================================
// 3. AI Service Brand Accent Palette
// ==============================================================================
val BrandChatGPT = Color(0xFF10A37F)
val BrandClaude = Color(0xFFD97706)
val BrandDeepSeek = Color(0xFF1E88E5)
val BrandGrok = Color(0xFFFF3B30)
val BrandGemini = Color(0xFF3B82F6)
val BrandPerplexity = Color(0xFF22B8CF)

// ==============================================================================
// 4. Accent & Semantic Action Colors
// ==============================================================================
val ElectricIndigo = Color(0xFF6366F1)
val ElectricIndigoLight = Color(0xFF818CF8)
val EmeraldSuccess = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val CrimsonError = Color(0xFFEF4444)

// ==============================================================================
// 5. Glass Token Data Structure & Composition Local
// ==============================================================================
@Immutable
data class GlassColors(
    val surface: Color,
    val surfaceElevated: Color,
    val borderStart: Color,
    val borderEnd: Color,
    val highlight: Color,
    val shadow: Color
) {
    val specularBorderBrush: Brush
        get() = Brush.linearGradient(
            listOf(borderStart, borderEnd)
        )
}

val DarkGlassColors = GlassColors(
    surface = GlassDarkSurface,
    surfaceElevated = GlassDarkSurfaceElevated,
    borderStart = GlassDarkBorderStart,
    borderEnd = GlassDarkBorderEnd,
    highlight = GlassDarkHighlight,
    shadow = GlassDarkShadow
)

val LightGlassColors = GlassColors(
    surface = GlassLightSurface,
    surfaceElevated = GlassLightSurfaceElevated,
    borderStart = GlassLightBorderStart,
    borderEnd = GlassLightBorderEnd,
    highlight = GlassLightHighlight,
    shadow = GlassLightShadow
)

val LocalGlassColors = staticCompositionLocalOf { DarkGlassColors }

// ==============================================================================
// 6. Material 3 Color Schemes
// ==============================================================================
val ColAIDarkColorScheme = darkColorScheme(
    primary = ElectricIndigoLight,
    onPrimary = PitchBlack,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = BrandPerplexity,
    onSecondary = PitchBlack,
    secondaryContainer = Color(0xFF0E4C56),
    onSecondaryContainer = Color(0xFFC5F6FA),
    tertiary = BrandChatGPT,
    onTertiary = PitchBlack,
    background = PitchBlack,
    onBackground = Color(0xFFF3F4F6),
    surface = DarkSurface,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1AFFFFFF),
    error = CrimsonError,
    onError = PitchBlack
)

val ColAILightColorScheme = lightColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = BrandPerplexity,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3FAFC),
    onSecondaryContainer = Color(0xFF0C8599),
    tertiary = BrandChatGPT,
    onTertiary = Color.White,
    background = LuminousMist,
    onBackground = Color(0xFF111827),
    surface = LightSurface,
    onSurface = Color(0xFF111827),
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0x26000000),
    outlineVariant = Color(0x14000000),
    error = CrimsonError,
    onError = Color.White
)
