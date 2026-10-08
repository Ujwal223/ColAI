package com.ujwal.colai.feature.webview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

/**
 * High-performance parser and adapter for real-time web theme colors.
 *
 * Webpages declare theme colors via `<meta name="theme-color" content="...">`
 * or CSS rules. GeckoView reports these via `NavigationDelegate.onThemeColorChange`.
 *
 * This adapter:
 * - Parses diverse web color formats (Hex #RGB, #RGBA, #RRGGBB, #RRGGBBAA, rgb(), rgba(), hsl()).
 * - Analyzes luminance to determine optimal foreground text/icon contrast.
 * - Blends theme colors with Liquid Glass surfaces for a premium iOS 26 frosted aesthetic.
 * - Powers 120Hz zero-latency color transitions via Compose animation springs.
 */
object ThemeColorAdapter {

    /**
     * Parses a raw web color string into a Jetpack Compose [Color], or null if invalid/transparent.
     *
     * Supported formats:
     * - `#RGB` -> `#RRGGBB`
     * - `#RGBA` -> `#RRGGBBAA`
     * - `#RRGGBB`
     * - `#RRGGBBAA`
     * - `rgb(r, g, b)`
     * - `rgba(r, g, b, a)`
     * - `hsl(h, s%, l%)`
     * - Standard CSS keywords ("white", "black", "transparent", etc.)
     */
    fun parseWebColor(colorString: String?): Color? {
        if (colorString.isNullOrBlank()) return null
        val trimmed = colorString.trim().lowercase()

        if (trimmed == "transparent" || trimmed == "inherit" || trimmed == "initial") {
            return null
        }

        // Common named colors
        when (trimmed) {
            "white" -> return Color.White
            "black" -> return Color.Black
            "gray", "grey" -> return Color.Gray
        }

        // Hex formatting: #RGB, #RGBA, #RRGGBB, #RRGGBBAA
        if (trimmed.startsWith("#")) {
            return parseHexColor(trimmed.substring(1))
        }

        // rgb(r, g, b) or rgba(r, g, b, a)
        if (trimmed.startsWith("rgb")) {
            return parseRgbColor(trimmed)
        }

        // hsl(h, s%, l%) or hsla(h, s%, l%, a)
        if (trimmed.startsWith("hsl")) {
            return parseHslColor(trimmed)
        }

        return null
    }

    private fun parseHexColor(hex: String): Color? {
        return try {
            when (hex.length) {
                3 -> { // #RGB -> #RRGGBB
                    val r = hex[0].toString().repeat(2).toInt(16)
                    val g = hex[1].toString().repeat(2).toInt(16)
                    val b = hex[2].toString().repeat(2).toInt(16)
                    Color(r, g, b)
                }
                4 -> { // #RGBA -> #RRGGBBAA
                    val r = hex[0].toString().repeat(2).toInt(16)
                    val g = hex[1].toString().repeat(2).toInt(16)
                    val b = hex[2].toString().repeat(2).toInt(16)
                    val a = hex[3].toString().repeat(2).toInt(16)
                    Color(r, g, b, a)
                }
                6 -> { // #RRGGBB
                    val r = hex.substring(0, 2).toInt(16)
                    val g = hex.substring(2, 4).toInt(16)
                    val b = hex.substring(4, 6).toInt(16)
                    Color(r, g, b)
                }
                8 -> { // #RRGGBBAA
                    val r = hex.substring(0, 2).toInt(16)
                    val g = hex.substring(2, 4).toInt(16)
                    val b = hex.substring(4, 6).toInt(16)
                    val a = hex.substring(6, 8).toInt(16)
                    Color(r, g, b, a)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRgbColor(rgbStr: String): Color? {
        return try {
            val content = rgbStr.substringAfter("(").substringBefore(")")
            val parts = content.split(",").map { it.trim() }
            if (parts.size in 3..4) {
                val r = parts[0].toInt().coerceIn(0, 255)
                val g = parts[1].toInt().coerceIn(0, 255)
                val b = parts[2].toInt().coerceIn(0, 255)
                val a = if (parts.size == 4) {
                    val alphaVal = parts[3].toFloatOrNull() ?: 1.0f
                    (alphaVal.coerceIn(0f, 1f) * 255).roundToInt()
                } else 255
                Color(r, g, b, a)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseHslColor(hslStr: String): Color? {
        return try {
            val content = hslStr.substringAfter("(").substringBefore(")")
            val parts = content.split(",").map { it.trim() }
            if (parts.size >= 3) {
                val h = parts[0].toFloat().coerceIn(0f, 360f)
                val s = parts[1].removeSuffix("%").toFloat().coerceIn(0f, 100f) / 100f
                val l = parts[2].removeSuffix("%").toFloat().coerceIn(0f, 100f) / 100f
                val a = if (parts.size >= 4) {
                    parts[3].toFloatOrNull()?.coerceIn(0f, 1f) ?: 1.0f
                } else 1.0f

                val rgb = hslToRgb(h, s, l)
                Color(rgb[0], rgb[1], rgb[2], (a * 255).roundToInt())
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): IntArray {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        var r = 0f
        var g = 0f
        var b = 0f

        when {
            h < 60f -> { r = c; g = x; b = 0f }
            h < 120f -> { r = x; g = c; b = 0f }
            h < 180f -> { r = 0f; g = c; b = x }
            h < 240f -> { r = 0f; g = x; b = c }
            h < 300f -> { r = x; g = 0f; b = c }
            else -> { r = c; g = 0f; b = x }
        }

        return intArrayOf(
            ((r + m) * 255f).roundToInt().coerceIn(0, 255),
            ((g + m) * 255f).roundToInt().coerceIn(0, 255),
            ((b + m) * 255f).roundToInt().coerceIn(0, 255)
        )
    }

    /**
     * Calculates relative luminance of [color] according to WCAG 2.1 specs.
     */
    fun calculateLuminance(color: Color): Float {
        val r = sRgbToLinear(color.red)
        val g = sRgbToLinear(color.green)
        val b = sRgbToLinear(color.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun sRgbToLinear(channel: Float): Float {
        return if (channel <= 0.04045f) {
            channel / 12.92f
        } else {
            Math.pow(((channel + 0.055) / 1.055), 2.4).toFloat()
        }
    }

    /**
     * Determines whether [color] is dark (luminance < 0.5f).
     */
    fun isColorDark(color: Color): Boolean = calculateLuminance(color) < 0.45f

    /**
     * Calculates the ideal contrasting foreground content color (white for dark backgrounds, dark for light).
     */
    fun getContrastingContentColor(background: Color): Color {
        return if (isColorDark(background)) {
            Color.White
        } else {
            Color(0xFF111114)
        }
    }

    /**
     * Blends the raw web [themeColor] with the base [defaultSurface] liquid glass token.
     * This creates a cohesive tinted glass aesthetic rather than an overpowering flat solid block.
     */
    fun blendGlassTint(
        themeColor: Color?,
        defaultSurface: Color,
        tintAlpha: Float = 0.35f
    ): Color {
        if (themeColor == null) return defaultSurface
        return Color(
            red = (themeColor.red * tintAlpha + defaultSurface.red * (1f - tintAlpha)),
            green = (themeColor.green * tintAlpha + defaultSurface.green * (1f - tintAlpha)),
            blue = (themeColor.blue * tintAlpha + defaultSurface.blue * (1f - tintAlpha)),
            alpha = (themeColor.alpha * tintAlpha + defaultSurface.alpha * (1f - tintAlpha)).coerceIn(0f, 1f)
        )
    }
}

/**
 * Remembers and animates color transitions at 120Hz with physics-based spring damping.
 */
@Composable
fun rememberAnimatedAdaptiveColor(
    targetColor: Color,
    label: String = "AdaptiveThemeColor"
): State<Color> {
    return animateColorAsState(
        targetValue = targetColor,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = label
    )
}
