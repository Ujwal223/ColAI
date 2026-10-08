package com.ujwal.colai.core.ui.theme

import com.ujwal.colai.util.Haptics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MotionTest {

    @Test
    fun testColAISpringsDampingAndStiffness() {
        assertNotNull(ColAISprings.Snappy)
        assertNotNull(ColAISprings.Fluid)
        assertNotNull(ColAISprings.Bouncy)
        assertNotNull(ColAISprings.ThemeColorAdaptation)

        val customSpring = ColAISprings.custom<Float>(dampingRatio = 0.8f, stiffness = 400f)
        assertEquals(0.8f, customSpring.dampingRatio, 0.001f)
        assertEquals(400f, customSpring.stiffness, 0.001f)
    }

    @Test
    fun testHapticsInstantiationWithoutCrashing() {
        // Haptics initialized with null context/view in JVM test environment should safely handle calls
        val haptics = Haptics(context = null, view = null)
        haptics.selection()
        haptics.impactLight()
        haptics.impactMedium()
        haptics.impactHeavy()
        haptics.success()
        haptics.error()
    }
}
