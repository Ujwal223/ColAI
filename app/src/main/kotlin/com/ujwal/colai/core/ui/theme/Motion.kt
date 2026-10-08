package com.ujwal.colai.core.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * iOS 26 Physics Spring Motion System.
 * Tuned damping ratios and stiffness constants creating organic, fluid micro-interactions.
 */
object ColAISprings {

    /**
     * Snappy response for button taps, toggle switches, and immediate micro-interactions.
     * High stiffness with zero overshooting bounce.
     */
    val Snappy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 500f
    )

    val SnappyDp: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 500f
    )

    /**
     * Fluid response for tab transitions, sliding bottom sheets, and view bounds.
     * Damping 0.75f with 350f stiffness provides gentle deceleration.
     */
    val Fluid: SpringSpec<Float> = spring(
        dampingRatio = 0.75f,
        stiffness = 350f
    )

    val FluidDp: SpringSpec<Dp> = spring(
        dampingRatio = 0.75f,
        stiffness = 350f
    )

    val FluidOffset: SpringSpec<Offset> = spring(
        dampingRatio = 0.75f,
        stiffness = 350f
    )

    val FluidIntOffset: SpringSpec<IntOffset> = spring(
        dampingRatio = 0.75f,
        stiffness = 350f
    )

    /**
     * Bouncy physics for playful badges, floating modals, and celebratory reveals.
     */
    val Bouncy: SpringSpec<Float> = spring(
        dampingRatio = 0.65f,
        stiffness = 250f
    )

    val BouncyDp: SpringSpec<Dp> = spring(
        dampingRatio = 0.65f,
        stiffness = 250f
    )

    /**
     * Buttery zero-lag color adaptation spring for GeckoView 120Hz theme morphing.
     */
    val ThemeColorAdaptation: AnimationSpec<Color> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Generic spring creator with custom stiffness and damping.
     */
    fun <T> custom(
        dampingRatio: Float = 0.75f,
        stiffness: Float = 350f
    ): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )
}
