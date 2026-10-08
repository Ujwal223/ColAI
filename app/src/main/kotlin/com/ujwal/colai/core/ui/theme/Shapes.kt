package com.ujwal.colai.core.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Mathematical Superellipse Shape (Lamé Curve).
 * Follows equation: |x/a|^n + |y/b|^n = 1.
 * An exponent of n=4 produces the iconic organic iOS squircle curvature.
 */
class SuperellipseShape(
    val n: Float = 4.0f,
    private val steps: Int = 72
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) {
            return Outline.Generic(Path())
        }

        val a = width / 2f
        val b = height / 2f
        val p = 2f / n

        val path = Path()
        val stepAngle = (2.0 * PI / steps).toFloat()

        for (i in 0..steps) {
            val theta = i * stepAngle
            val cosT = cos(theta)
            val sinT = sin(theta)

            val x = a + a * sign(cosT) * abs(cosT).pow(p)
            val y = b + b * sign(sinT) * abs(sinT).pow(p)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuperellipseShape) return false
        return n == other.n && steps == other.steps
    }

    override fun hashCode(): Int = 31 * n.hashCode() + steps.hashCode()
}

/**
 * Organic Squircle Corner-based Shape with continuous G2 curvature.
 * Extends [CornerBasedShape] to fully integrate with Jetpack Compose Material 3 theme.
 *
 * @param smoothing Curvature smoothness factor (0.0 = circular arc, 0.6 = iOS squircle)
 */
class SquircleShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    val smoothing: Float = 0.6f
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    constructor(
        corner: CornerSize,
        smoothing: Float = 0.6f
    ) : this(corner, corner, corner, corner, smoothing)

    constructor(
        cornerRadius: Dp = 16.dp,
        smoothing: Float = 0.6f
    ) : this(CornerSize(cornerRadius), smoothing)

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
    ): CornerBasedShape = SquircleShape(topStart, topEnd, bottomEnd, bottomStart, smoothing)

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.Generic(Path())

        val minDimension = min(w, h)
        val maxAllowedRadius = minDimension / 2f

        val rTS = min(topStart, maxAllowedRadius)
        val rTE = min(topEnd, maxAllowedRadius)
        val rBE = min(bottomEnd, maxAllowedRadius)
        val rBS = min(bottomStart, maxAllowedRadius)

        if (rTS <= 0f && rTE <= 0f && rBE <= 0f && rBS <= 0f) {
            val path = Path().apply {
                addRect(Rect(0f, 0f, w, h))
            }
            return Outline.Generic(path)
        }

        val p = smoothing.coerceIn(0f, 1f)
        val path = Path()

        val offTS = min(rTS * (1f + p), maxAllowedRadius)
        val offTE = min(rTE * (1f + p), maxAllowedRadius)
        val offBE = min(rBE * (1f + p), maxAllowedRadius)
        val offBS = min(rBS * (1f + p), maxAllowedRadius)

        // Top edge: from top-start to top-end
        path.moveTo(offTS, 0f)
        path.lineTo(w - offTE, 0f)

        // Top-right corner
        if (rTE > 0f) {
            path.cubicTo(
                w - offTE + rTE * p * 0.5f, 0f,
                w, offTE - rTE * p * 0.5f,
                w, offTE
            )
        } else {
            path.lineTo(w, 0f)
        }

        // Right edge: from top-end to bottom-end
        path.lineTo(w, h - offBE)

        // Bottom-right corner
        if (rBE > 0f) {
            path.cubicTo(
                w, h - offBE + rBE * p * 0.5f,
                w - offBE + rBE * p * 0.5f, h,
                w - offBE, h
            )
        } else {
            path.lineTo(w, h)
        }

        // Bottom edge: from bottom-end to bottom-start
        path.lineTo(offBS, h)

        // Bottom-left corner
        if (rBS > 0f) {
            path.cubicTo(
                offBS - rBS * p * 0.5f, h,
                0f, h - offBS + rBS * p * 0.5f,
                0f, h - offBS
            )
        } else {
            path.lineTo(0f, h)
        }

        // Left edge: from bottom-start to top-start
        path.lineTo(0f, offTS)

        // Top-left corner
        if (rTS > 0f) {
            path.cubicTo(
                0f, offTS - rTS * p * 0.5f,
                offTS - rTS * p * 0.5f, 0f,
                offTS, 0f
            )
        } else {
            path.lineTo(0f, 0f)
        }

        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquircleShape) return false
        return topStart == other.topStart &&
                topEnd == other.topEnd &&
                bottomEnd == other.bottomEnd &&
                bottomStart == other.bottomStart &&
                smoothing == other.smoothing
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + smoothing.hashCode()
        return result
    }
}

// ==============================================================================
// Material 3 Squircle Shapes Hierarchy
// ==============================================================================
val ColAIShapes = Shapes(
    extraSmall = SquircleShape(8.dp),
    small = SquircleShape(12.dp),
    medium = SquircleShape(16.dp),
    large = SquircleShape(24.dp),
    extraLarge = SquircleShape(32.dp)
)
