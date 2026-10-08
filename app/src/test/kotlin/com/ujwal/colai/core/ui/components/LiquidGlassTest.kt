package com.ujwal.colai.core.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ujwal.colai.core.ui.theme.SquircleShape
import org.junit.Assert.assertNotNull
import org.junit.Test

class LiquidGlassTest {

    @Test
    fun testSpecularGlintBrushCreation() {
        val brush = createSpecularGlintBrush(
            startColor = Color(0x80FFFFFF),
            endColor = Color(0x14FFFFFF)
        )
        assertNotNull(brush)
    }

    @Test
    fun testLiquidGlassModifierChaining() {
        val modifier = Modifier.liquidGlass(
            shape = SquircleShape(16.dp),
            blurRadius = 24.dp,
            borderWidth = 0.75.dp
        )
        assertNotNull(modifier)
    }
}
