package com.ujwal.colai.feature.webview

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorAdapterTest {

    @Test
    fun testParseHexColors() {
        // 3-digit hex #RGB -> #RRGGBB
        val white3 = ThemeColorAdapter.parseWebColor("#fff")
        assertNotNull(white3)
        assertEquals(Color.White, white3)

        val black3 = ThemeColorAdapter.parseWebColor("#000")
        assertNotNull(black3)
        assertEquals(Color.Black, black3)

        // 6-digit hex #RRGGBB
        val chatgptColor = ThemeColorAdapter.parseWebColor("#10A37F")
        assertNotNull(chatgptColor)
        assertEquals(Color(0xFF10A37F), chatgptColor)

        // 8-digit hex #RRGGBBAA
        val translucentColor = ThemeColorAdapter.parseWebColor("#10A37F80")
        assertNotNull(translucentColor)
        assertEquals(0x80, (translucentColor!!.alpha * 255).toInt())
    }

    @Test
    fun testParseRgbAndRgbaColors() {
        val rgb = ThemeColorAdapter.parseWebColor("rgb(16, 163, 127)")
        assertNotNull(rgb)
        assertEquals(16 / 255f, rgb!!.red, 0.01f)
        assertEquals(163 / 255f, rgb.green, 0.01f)
        assertEquals(127 / 255f, rgb.blue, 0.01f)
        assertEquals(1.0f, rgb.alpha, 0.01f)

        val rgba = ThemeColorAdapter.parseWebColor("rgba(217, 119, 6, 0.5)")
        assertNotNull(rgba)
        assertEquals(217 / 255f, rgba!!.red, 0.01f)
        assertEquals(0.5f, rgba.alpha, 0.01f)
    }

    @Test
    fun testParseHslColors() {
        // Pure green: hsl(120, 100%, 50%) -> (0, 255, 0)
        val hslGreen = ThemeColorAdapter.parseWebColor("hsl(120, 100%, 50%)")
        assertNotNull(hslGreen)
        assertEquals(0.0f, hslGreen!!.red, 0.01f)
        assertEquals(1.0f, hslGreen.green, 0.01f)
        assertEquals(0.0f, hslGreen.blue, 0.01f)
    }

    @Test
    fun testParseInvalidOrSpecialColors() {
        assertNull(ThemeColorAdapter.parseWebColor(null))
        assertNull(ThemeColorAdapter.parseWebColor(""))
        assertNull(ThemeColorAdapter.parseWebColor("   "))
        assertNull(ThemeColorAdapter.parseWebColor("transparent"))
        assertNull(ThemeColorAdapter.parseWebColor("inherit"))
        assertNull(ThemeColorAdapter.parseWebColor("not-a-valid-color"))
    }

    @Test
    fun testLuminanceAndContrast() {
        val darkColor = Color(0xFF050508)
        val lightColor = Color(0xFFF6F7FA)

        assertTrue(ThemeColorAdapter.isColorDark(darkColor))
        assertFalse(ThemeColorAdapter.isColorDark(lightColor))

        val onDarkContent = ThemeColorAdapter.getContrastingContentColor(darkColor)
        assertEquals(Color.White, onDarkContent)

        val onLightContent = ThemeColorAdapter.getContrastingContentColor(lightColor)
        assertEquals(Color(0xFF111114), onLightContent)
    }

    @Test
    fun testBlendGlassTint() {
        val defaultSurface = Color(0xFF050508)
        val chatgptEmerald = Color(0xFF10A37F)

        val blended = ThemeColorAdapter.blendGlassTint(
            themeColor = chatgptEmerald,
            defaultSurface = defaultSurface,
            tintAlpha = 0.35f
        )

        assertNotNull(blended)
        assertTrue(blended.green > defaultSurface.green)
        assertTrue(blended.green < chatgptEmerald.green)

        // When theme color is null, fallback to default surface
        val nullBlend = ThemeColorAdapter.blendGlassTint(null, defaultSurface)
        assertEquals(defaultSurface, nullBlend)
    }
}
