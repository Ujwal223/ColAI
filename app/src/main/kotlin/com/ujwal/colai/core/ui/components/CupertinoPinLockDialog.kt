package com.ujwal.colai.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ujwal.colai.util.rememberHaptics
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Authentic Apple iOS 4-digit PIN authentication dialog.
 */
@Composable
fun CupertinoPinLockDialog(
    sessionName: String,
    onDismissRequest: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onSuccess: () -> Unit,
    onForgotPin: (() -> Unit)? = null,
    isDark: Boolean = true
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF9F9FB)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0x18000000)
    val textColor = if (isDark) Color.White else Color.Black
    val keyBg = if (isDark) Color(0x28FFFFFF) else Color(0x14000000)

    fun handleDigitPress(digit: String) {
        if (enteredPin.length < 4) {
            haptics.selection()
            val next = enteredPin + digit
            enteredPin = next
            isError = false
            if (next.length == 4) {
                if (onVerifyPin(next)) {
                    haptics.impactMedium()
                    onSuccess()
                } else {
                    haptics.error()
                    isError = true
                    scope.launch {
                        shakeOffset.animateTo(20f, tween(50))
                        shakeOffset.animateTo(-20f, tween(50))
                        shakeOffset.animateTo(15f, tween(50))
                        shakeOffset.animateTo(-15f, tween(50))
                        shakeOffset.animateTo(0f, tween(50))
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            haptics.selection()
            enteredPin = enteredPin.dropLast(1)
            isError = false
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(BorderStroke(0.75.dp, cardBorder), RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Lock Icon
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(CupertinoActiveBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Session Locked",
                            tint = CupertinoActiveBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Unlock \"$sessionName\"",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isError) "Incorrect PIN, try again" else "Enter 4-digit PIN",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = if (isError) Color(0xFFFF453A) else textColor.copy(alpha = 0.6f)
                        )
                    }

                    // 4 Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        repeat(4) { index ->
                            val isFilled = index < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) CupertinoActiveBlue else (if (isDark) Color(0x33FFFFFF) else Color(0x22000000))
                                    )
                            )
                        }
                    }

                    // 3x4 Keypad
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("cancel", "0", "delete")
                        )

                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { key ->
                                    when (key) {
                                        "cancel" -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(62.dp)
                                                    .clickable {
                                                        haptics.selection()
                                                        onDismissRequest()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Cancel",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = CupertinoActiveBlue
                                                )
                                            }
                                        }
                                        "delete" -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(62.dp)
                                                    .clip(CircleShape)
                                                    .clickable { handleBackspace() },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "⌫",
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = textColor.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                        else -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(62.dp)
                                                    .clip(CircleShape)
                                                    .background(keyBg)
                                                    .clickable { handleDigitPress(key) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = key,
                                                    style = MaterialTheme.typography.headlineMedium.copy(
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = textColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (onForgotPin != null) {
                        TextButton(
                            onClick = {
                                haptics.selection()
                                onForgotPin()
                            }
                        ) {
                            Text(
                                text = "Forgot PIN?",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = CupertinoActiveBlue
                            )
                        }
                    }
                }
            }
        }
    }
}
