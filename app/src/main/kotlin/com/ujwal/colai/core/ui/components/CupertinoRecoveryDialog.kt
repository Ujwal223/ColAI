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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ujwal.colai.util.rememberHaptics
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Modern Cupertino dialog for setting up or changing the Master Recovery Key.
 *
 * This key works universally across all isolated sessions, allowing
 * users to safely reset forgotten individual session locks.
 */
@Composable
fun CupertinoSetupRecoveryKeyDialog(
    onDismissRequest: () -> Unit,
    onKeySaved: (String) -> Unit,
    isDark: Boolean = true
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var recoveryKeyText by remember { mutableStateOf("") }
    var confirmKeyText by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0x18000000)
    val textColor = if (isDark) Color.White else Color.Black
    val inputBg = if (isDark) Color(0x20FFFFFF) else Color(0x0C000000)
    val inputBorder = if (isDark) Color(0x38FFFFFF) else Color(0x1F000000)

    fun triggerShake() {
        scope.launch {
            shakeOffset.animateTo(18f, tween(40))
            shakeOffset.animateTo(-18f, tween(40))
            shakeOffset.animateTo(12f, tween(40))
            shakeOffset.animateTo(-12f, tween(40))
            shakeOffset.animateTo(0f, tween(40))
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
                    .fillMaxWidth(0.90f)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .clip(RoundedCornerShape(26.dp))
                    .background(cardBg)
                    .border(BorderStroke(0.75.dp, cardBorder), RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(vertical = 22.dp, horizontal = 20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(CupertinoActiveBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CupertinoActiveBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Set Up Master Recovery Key",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create a master recovery key (at least 6 characters). If you ever forget an individual session PIN, this key resets it.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = textColor.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Input Box 1: Recovery Key
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(inputBg)
                            .border(BorderStroke(0.5.dp, inputBorder), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (recoveryKeyText.isEmpty()) {
                            Text(
                                text = "Enter recovery key (min 6 chars)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = textColor.copy(alpha = 0.4f)
                            )
                        }
                        BasicTextField(
                            value = recoveryKeyText,
                            onValueChange = {
                                recoveryKeyText = it
                                errorMessage = null
                            },
                            textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            cursorBrush = SolidColor(CupertinoActiveBlue),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Input Box 2: Confirm Key
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(inputBg)
                            .border(BorderStroke(0.5.dp, inputBorder), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (confirmKeyText.isEmpty()) {
                            Text(
                                text = "Confirm recovery key",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = textColor.copy(alpha = 0.4f)
                            )
                        }
                        BasicTextField(
                            value = confirmKeyText,
                            onValueChange = {
                                confirmKeyText = it
                                errorMessage = null
                            },
                            textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            cursorBrush = SolidColor(CupertinoActiveBlue),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (showPassword) "Hide Characters" else "Show Characters",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = CupertinoActiveBlue,
                            modifier = Modifier.clickable { showPassword = !showPassword }
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFFFF453A)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text("Cancel", color = CupertinoActiveBlue)
                        }

                        TextButton(
                            onClick = {
                                when {
                                    recoveryKeyText.length < 6 -> {
                                        haptics.error()
                                        errorMessage = "Must be at least 6 characters"
                                        triggerShake()
                                    }
                                    recoveryKeyText != confirmKeyText -> {
                                        haptics.error()
                                        errorMessage = "Recovery keys do not match"
                                        triggerShake()
                                    }
                                    else -> {
                                        haptics.impactLight()
                                        onKeySaved(recoveryKeyText.trim())
                                    }
                                }
                            }
                        ) {
                            Text("Save Key", fontWeight = FontWeight.Bold, color = CupertinoActiveBlue)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Cupertino dialog to reset a forgotten session lock using the Master Recovery Key.
 */
@Composable
fun CupertinoResetWithRecoveryKeyDialog(
    sessionName: String,
    onDismissRequest: () -> Unit,
    onVerifyRecoveryKey: (String) -> Boolean,
    onRecoverySuccess: () -> Unit,
    isDark: Boolean = true
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var inputKey by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0x18000000)
    val textColor = if (isDark) Color.White else Color.Black
    val inputBg = if (isDark) Color(0x20FFFFFF) else Color(0x0C000000)
    val inputBorder = if (isDark) Color(0x38FFFFFF) else Color(0x1F000000)

    fun triggerShake() {
        scope.launch {
            shakeOffset.animateTo(18f, tween(40))
            shakeOffset.animateTo(-18f, tween(40))
            shakeOffset.animateTo(12f, tween(40))
            shakeOffset.animateTo(-12f, tween(40))
            shakeOffset.animateTo(0f, tween(40))
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
                    .fillMaxWidth(0.90f)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .clip(RoundedCornerShape(26.dp))
                    .background(cardBg)
                    .border(BorderStroke(0.75.dp, cardBorder), RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(vertical = 22.dp, horizontal = 20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(CupertinoActiveBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = null,
                            tint = CupertinoActiveBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Reset Lock for \"$sessionName\"",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter your Master Recovery Key to remove the PIN lock on this session.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = textColor.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(inputBg)
                            .border(BorderStroke(0.5.dp, inputBorder), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputKey.isEmpty()) {
                            Text(
                                text = "Master Recovery Key",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = textColor.copy(alpha = 0.4f)
                            )
                        }
                        BasicTextField(
                            value = inputKey,
                            onValueChange = {
                                inputKey = it
                                errorMessage = null
                            },
                            textStyle = TextStyle(
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            cursorBrush = SolidColor(CupertinoActiveBlue),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (showPassword) "Hide Characters" else "Show Characters",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = CupertinoActiveBlue,
                            modifier = Modifier.clickable { showPassword = !showPassword }
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFFFF453A)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text("Cancel", color = CupertinoActiveBlue)
                        }

                        TextButton(
                            onClick = {
                                if (onVerifyRecoveryKey(inputKey)) {
                                    haptics.impactLight()
                                    onRecoverySuccess()
                                } else {
                                    haptics.error()
                                    errorMessage = "Invalid Master Recovery Key"
                                    triggerShake()
                                }
                            }
                        ) {
                            Text("Reset Lock", fontWeight = FontWeight.Bold, color = Color(0xFFFF453A))
                        }
                    }
                }
            }
        }
    }
}
