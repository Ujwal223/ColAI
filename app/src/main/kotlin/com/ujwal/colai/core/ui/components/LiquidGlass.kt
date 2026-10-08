package com.ujwal.colai.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ujwal.colai.core.ui.theme.ColAITheme
import com.ujwal.colai.core.ui.theme.SquircleShape

/**
 * Applies authentic iOS surface visual treatment:
 * - Crisp squircle/superellipse clipping
 * - iOS elevation drop shadows
 * - Directional hairline specular highlight border
 * - Top-edge subtle refraction sheen
 * - 100% razor-sharp rasterization with ZERO content-blurring shaders
 *
 * @param shape Corner or boundary shape (defaults to 16.dp Squircle)
 * @param blurRadius Kept for backward compatibility, unused to prevent blurring UI contents
 * @param backgroundColor Override background surface tint
 * @param borderWidth Thickness of the hairline reflection edge (default 0.75.dp)
 * @param elevation Drop shadow elevation
 * @param specularBorder Custom specular refraction brush
 */
fun Modifier.liquidGlass(
    shape: Shape = SquircleShape(16.dp),
    blurRadius: Dp = 0.dp,
    backgroundColor: Color? = null,
    borderWidth: Dp = 0.75.dp,
    elevation: Dp = 0.dp,
    specularBorder: Brush? = null,
    showInternalHighlight: Boolean = true
): Modifier = this.then(
    Modifier
        .then(
            if (elevation > 0.dp) {
                Modifier.shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false
                )
            } else Modifier
        )
        .graphicsLayer {
            this.shape = shape
            this.clip = true
        }
        .drawBehind {
            if (showInternalHighlight) {
                val highlightBrush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.08f),
                    0.35f to Color.White.copy(alpha = 0.01f),
                    1.0f to Color.Transparent,
                    startY = 0f,
                    endY = size.height
                )
                drawRect(brush = highlightBrush)
            }
        }
)

/**
 * High-level Composable container providing authentic iOS surface aesthetics.
 * Renders all inner content (text, icons, images) 100% sharp and fully visible.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = SquircleShape(16.dp),
    blurRadius: Dp = 0.dp,
    backgroundColor: Color = ColAITheme.glassColors.surface,
    borderWidth: Dp = 0.75.dp,
    elevation: Dp = 0.dp,
    specularBrush: Brush = ColAITheme.glassColors.specularBorderBrush,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        clip = false
                    )
                } else Modifier
            )
            .clip(shape)
            .background(color = backgroundColor, shape = shape)
            .border(
                width = borderWidth,
                brush = specularBrush,
                shape = shape
            )
            .drawBehind {
                val highlightBrush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.08f),
                    0.35f to Color.White.copy(alpha = 0.01f),
                    1.0f to Color.Transparent,
                    startY = 0f,
                    endY = size.height
                )
                drawRect(brush = highlightBrush)
            }
            .then(
                when {
                    onClick != null && onLongClick != null -> {
                        Modifier.combinedClickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = onClick,
                            onLongClick = onLongClick
                        )
                    }
                    onClick != null -> {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = onClick
                        )
                    }
                    else -> Modifier
                }
            ),
        content = content
    )
}

/**
 * Creates a directional refraction border brush for iOS surfaces.
 *
 * @param startColor Top-left refraction entry color
 * @param endColor Bottom-right refraction exit color
 */
fun createSpecularGlintBrush(
    startColor: Color = Color(0x38FFFFFF),
    endColor: Color = Color(0x14FFFFFF)
): Brush = Brush.linearGradient(
    colors = listOf(startColor, endColor),
    start = Offset.Zero,
    end = Offset.Infinite
)
