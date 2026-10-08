package com.ujwal.colai.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ujwal.colai.core.ui.theme.SquircleShape
import com.ujwal.colai.util.rememberHaptics

// Cupertino iOS palette constants
val CupertinoActiveBlue = Color(0xFF007AFF)
val CupertinoDestructiveRed = Color(0xFFFF3B30)
val CupertinoSystemGrey = Color(0xFF8E8E93)
val CupertinoSystemGrey2 = Color(0xFFAEAEB2)
val CupertinoSystemGrey5 = Color(0xFFE5E5EA)
val CupertinoSystemGrey6 = Color(0xFFF2F2F7)
val CupertinoDarkCard = Color(0xFF1C1C1E)
val CupertinoDarkCardElevated = Color(0xFF2C2C2E)
val CupertinoDarkBg = Color(0xFF121212)
val CupertinoLightBg = Color(0xFFF2F2F7)

/**
 * Standard iOS Cupertino Navigation Bar (44dp - 50dp height).
 */
@Composable
fun CupertinoNavigationBar(
    title: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    textColor: Color = Color.White
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (leading != null) {
                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    leading()
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.4).sp
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )

            if (trailing != null) {
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    trailing()
                }
            }
        }
    }
}

/**
 * Inset-grouped Cupertino Form Section matching CupertinoFormSection.insetGrouped.
 */
@Composable
fun CupertinoFormSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    isDark: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val isHighContrast = com.ujwal.colai.core.ui.theme.LocalContrastLevel.current == "high"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (!header.isNullOrBlank()) {
            Text(
                text = header.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = (-0.1).sp
                ),
                color = CupertinoSystemGrey,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
            )
        }

        val cardBg = if (isDark) {
            if (isHighContrast) Color(0xFF141416) else CupertinoDarkCard
        } else {
            Color.White
        }
        val borderLine = if (isDark) {
            if (isHighContrast) Color(0x44FFFFFF) else Color(0x22FFFFFF)
        } else {
            if (isHighContrast) Color(0x28000000) else Color(0x14000000)
        }
        val borderWidth = if (isHighContrast) 1.dp else 0.5.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .border(width = borderWidth, color = borderLine, shape = RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }

        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = CupertinoSystemGrey,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp)
            )
        }
    }
}

/**
 * Standard iOS Cupertino Form Row.
 */
@Composable
fun CupertinoFormRow(
    modifier: Modifier = Modifier,
    prefix: @Composable () -> Unit,
    helper: @Composable (() -> Unit)? = null,
    showDivider: Boolean = true,
    isDark: Boolean = true,
    onClick: (() -> Unit)? = null,
    child: @Composable () -> Unit
) {
    val haptics = rememberHaptics()
    val isHighContrast = com.ujwal.colai.core.ui.theme.LocalContrastLevel.current == "high"
    val dividerColor = if (isDark) {
        if (isHighContrast) Color(0x33FFFFFF) else Color(0x18FFFFFF)
    } else {
        if (isHighContrast) Color(0x24000000) else Color(0x14000000)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        haptics.selection()
                        onClick()
                    }
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                prefix()
                if (helper != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    helper()
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier.wrapContentWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                child()
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = if (isHighContrast) 1.dp else 0.5.dp,
                color = dividerColor
            )
        }
    }
}

/**
 * iOS Cupertino Sliding Segmented Control.
 */
@Composable
fun <T> CupertinoSlidingSegmentedControl(
    items: List<Pair<T, String>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    val haptics = rememberHaptics()
    val isHighContrast = com.ujwal.colai.core.ui.theme.LocalContrastLevel.current == "high"
    val containerBg = if (isDark) {
        if (isHighContrast) Color(0xFF222224) else Color(0xFF2C2C2E)
    } else {
        Color(0xFFE5E5EA)
    }
    val pillBg = if (isDark) {
        if (isHighContrast) Color(0xFF7C7C80) else Color(0xFF636366)
    } else {
        Color.White
    }

    Row(
        modifier = modifier
            .widthIn(min = 160.dp, max = 220.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(containerBg)
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (key, label) ->
            val isSelected = key == selectedItem
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(7.dp))
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(elevation = 2.dp, shape = RoundedCornerShape(7.dp), clip = false)
                                .background(pillBg)
                        } else Modifier
                    )
                    .clickable {
                        if (!isSelected) {
                            haptics.selection()
                            onItemSelected(key)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp
                    ),
                    color = if (isSelected) (if (isDark) Color.White else Color.Black)
                    else (if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f))
                )
            }
        }
    }
}

/**
 * iOS Cupertino Switch toggle.
 */
@Composable
fun CupertinoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    Switch(
        checked = checked,
        onCheckedChange = {
            haptics.selection()
            onCheckedChange(it)
        },
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = CupertinoActiveBlue,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0x38787880),
            uncheckedBorderColor = Color.Transparent,
            checkedBorderColor = Color.Transparent
        )
    )
}

/**
 * Authentic iOS Action Sheet Action Model.
 */
data class ActionSheetAction(
    val title: String,
    val isDestructive: Boolean = false,
    val isDefault: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Authentic iOS Cupertino Action Sheet with grouped actions and separate Cancel capsule.
 */
@Composable
fun CupertinoActionSheetDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    message: String? = null,
    actions: List<ActionSheetAction>,
    cancelTitle: String = "Cancel",
    isDark: Boolean = true
) {
    val haptics = rememberHaptics()
    val cardBg = if (isDark) Color(0xE6252525) else Color(0xF2F2F2F2)
    val dividerColor = if (isDark) Color(0x2EFFFFFF) else Color(0x22000000)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
                .padding(horizontal = 10.dp, vertical = 20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group 1: Title, Message & Actions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!title.isNullOrBlank() || !message.isNullOrBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!title.isNullOrBlank()) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        ),
                                        color = CupertinoSystemGrey,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (!message.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp
                                        ),
                                        color = CupertinoSystemGrey,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                        }

                        actions.forEachIndexed { index, action ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable {
                                        haptics.selection()
                                        action.onClick()
                                        onDismissRequest()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = action.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (action.isDefault) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 19.sp
                                    ),
                                    color = if (action.isDestructive) CupertinoDestructiveRed else CupertinoActiveBlue,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (index < actions.size - 1) {
                                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                            }
                        }
                    }
                }

                // Group 2: Cancel Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .clickable {
                            haptics.selection()
                            onDismissRequest()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cancelTitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = CupertinoActiveBlue,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Authentic iOS Cupertino Alert Dialog.
 */
@Composable
fun CupertinoAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String? = null,
    confirmTitle: String = "OK",
    cancelTitle: String? = null,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    isDark: Boolean = true,
    content: @Composable (() -> Unit)? = null
) {
    val haptics = rememberHaptics()
    val dialogBg = if (isDark) Color(0xEE2A2A2A) else Color(0xEEF8F8F8)
    val dividerColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 44.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(dialogBg)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.4).sp
                            ),
                            color = if (isDark) Color.White else Color.Black,
                            textAlign = TextAlign.Center
                        )

                        if (!message.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp
                                ),
                                color = if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }

                        if (content != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            content()
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (!cancelTitle.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable {
                                        haptics.selection()
                                        onDismissRequest()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cancelTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = CupertinoActiveBlue
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(0.5.dp)
                                    .fillMaxSize()
                                    .background(dividerColor)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable {
                                    haptics.selection()
                                    onConfirm()
                                    onDismissRequest()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = confirmTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isDestructive) CupertinoDestructiveRed else CupertinoActiveBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Authentic Apple Cupertino Activity Indicator (rotating 8-segment spinner).
 */
@Composable
fun CupertinoActivityIndicator(
    modifier: Modifier = Modifier,
    radius: Dp = 10.dp,
    color: Color = Color.White
) {
    val transition = rememberInfiniteTransition(label = "CupertinoSpinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinnerAngle"
    )

    Canvas(
        modifier = modifier.size(radius * 2)
    ) {
        val count = 8
        val sweepAngle = 360f / count
        val strokeWidth = (radius.toPx() * 0.22f)
        val petalLength = (radius.toPx() * 0.45f)

        rotate(angle) {
            for (i in 0 until count) {
                val alpha = (i + 1).toFloat() / count.toFloat()
                rotate(degrees = i * sweepAngle, pivot = center) {
                    drawLine(
                        color = color.copy(alpha = alpha),
                        start = Offset(center.x, center.y - radius.toPx() + (strokeWidth / 2)),
                        end = Offset(center.x, center.y - radius.toPx() + petalLength),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
    }
}
