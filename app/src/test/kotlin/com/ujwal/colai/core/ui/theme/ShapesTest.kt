package com.ujwal.colai.core.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapesTest {

    private val density = Density(density = 2.0f, fontScale = 1.0f)

    @Test
    fun testSuperellipseShapeGeneratesClosedOutline() {
        val shape = SuperellipseShape(n = 4.0f)
        val outline = shape.createOutline(
            size = Size(200f, 200f),
            layoutDirection = LayoutDirection.Ltr,
            density = density
        )

        assertTrue(outline is Outline.Generic)
        val genericOutline = outline as Outline.Generic
        assertNotNull(genericOutline.path)
    }

    @Test
    fun testSuperellipseShapeHandlesZeroBounds() {
        val shape = SuperellipseShape()
        val outline = shape.createOutline(
            size = Size(0f, 0f),
            layoutDirection = LayoutDirection.Ltr,
            density = density
        )
        assertTrue(outline is Outline.Generic)
    }

    @Test
    fun testSquircleShapeGeneratesSmoothOutline() {
        val shape = SquircleShape(cornerRadius = 16.dp, smoothing = 0.6f)
        val outline = shape.createOutline(
            size = Size(300f, 150f),
            layoutDirection = LayoutDirection.Ltr,
            density = density
        )

        assertTrue(outline is Outline.Generic)
        val genericOutline = outline as Outline.Generic
        assertNotNull(genericOutline.path)
    }

    @Test
    fun testSquircleShapeEquality() {
        val shape1 = SquircleShape(16.dp, 0.6f)
        val shape2 = SquircleShape(16.dp, 0.6f)
        val shape3 = SquircleShape(24.dp, 0.6f)

        assertEquals(shape1, shape2)
        assertEquals(shape1.hashCode(), shape2.hashCode())
        assertTrue(shape1 != shape3)
    }

    @Test
    fun testColAIShapesInitialization() {
        assertNotNull(ColAIShapes.extraSmall)
        assertNotNull(ColAIShapes.small)
        assertNotNull(ColAIShapes.medium)
        assertNotNull(ColAIShapes.large)
        assertNotNull(ColAIShapes.extraLarge)
    }

    @Test
    fun testGlassColorsBrushes() {
        val dark = DarkGlassColors
        assertNotNull(dark.specularBorderBrush)
        assertEquals(GlassDarkSurface, dark.surface)

        val light = LightGlassColors
        assertNotNull(light.specularBorderBrush)
        assertEquals(GlassLightSurface, light.surface)
    }
}
