package com.ujwal.colai.core.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ujwal.colai.core.security.EncryptedStorage
import com.ujwal.colai.util.rememberHaptics
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Modern Cupertino dialog to verify, set, change, or remove a 4-digit PIN lock for an isolated session.
 *
 * Security & Recovery Enforcement:
 * - Current PIN verification uses an authentic iOS numeric keypad with 4-dot indicators and haptic shake.
 * - Forgot PIN triggers the redesigned Master Recovery Key reset.
 * - Guides users to configure a Master Recovery Key on first lock setup.
 */
@Composable
fun CupertinoSetPinDialog(
    sessionName: String,
    hasExistingPin: Boolean,
    onDismissRequest: () -> Unit,
    onSavePin: (String?) -> Unit,
    onVerifyCurrentPin: ((String) -> Boolean)? = null,
    onVerifyRecoveryKey: ((String) -> Boolean)? = null,
    isDark: Boolean = true
) {
    val context = LocalContext.current
    val storage = remember { EncryptedStorage.getInstance(context) }
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var currentPhase by remember {
        mutableStateOf(
            if (hasExistingPin) "VERIFY_EXISTING"
            else if (!storage.hasMasterRecoveryKey()) "SETUP_RECOVERY"
            else "SET_NEW"
        )
    }

    var existingPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var recoveryKeyInput by remember { mutableStateOf("") }
    var confirmRecoveryKeyInput by remember { mutableStateOf("") }
    var showRecoveryKey by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0x18000000)
    val textColor = if (isDark) Color.White else Color.Black
    val keyBg = if (isDark) Color(0x28FFFFFF) else Color(0x14000000)
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
                when (currentPhase) {
                    "SETUP_RECOVERY" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(CupertinoActiveBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CupertinoActiveBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Text(
                                text = "Setup Master Password",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = textColor,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Before locking your first session, create a master password/recovery key to recover or reset any forgotten PINs in the future.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = textColor.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )

                            // Key Input
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
                                if (recoveryKeyInput.isEmpty()) {
                                    Text(
                                        text = "Master Password (min 6 characters)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        color = textColor.copy(alpha = 0.4f)
                                    )
                                }
                                BasicTextField(
                                    value = recoveryKeyInput,
                                    onValueChange = {
                                        recoveryKeyInput = it
                                        errorMessage = null
                                    },
                                    textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                                    cursorBrush = SolidColor(CupertinoActiveBlue),
                                    singleLine = true,
                                    visualTransformation = if (showRecoveryKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Confirm Key Input
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
                                if (confirmRecoveryKeyInput.isEmpty()) {
                                    Text(
                                        text = "Confirm Recovery Key",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        color = textColor.copy(alpha = 0.4f)
                                    )
                                }
                                BasicTextField(
                                    value = confirmRecoveryKeyInput,
                                    onValueChange = {
                                        confirmRecoveryKeyInput = it
                                        errorMessage = null
                                    },
                                    textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                                    cursorBrush = SolidColor(CupertinoActiveBlue),
                                    singleLine = true,
                                    visualTransformation = if (showRecoveryKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = if (showRecoveryKey) "Hide Characters" else "Show Characters",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = CupertinoActiveBlue,
                                    modifier = Modifier.clickable { showRecoveryKey = !showRecoveryKey }
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
                                            recoveryKeyInput.length < 6 -> {
                                                haptics.error()
                                                errorMessage = "Must be at least 6 characters"
                                                triggerShake()
                                            }
                                            recoveryKeyInput != confirmRecoveryKeyInput -> {
                                                haptics.error()
                                                errorMessage = "Recovery keys do not match"
                                                triggerShake()
                                            }
                                            else -> {
                                                storage.setMasterRecoveryKey(recoveryKeyInput.trim())
                                                haptics.impactLight()
                                                errorMessage = null
                                                currentPhase = "SET_NEW"
                                            }
                                        }
                                    }
                                ) {
                                    Text("Next", fontWeight = FontWeight.Bold, color = CupertinoActiveBlue)
                                }
                            }
                        }
                    }

                    "VERIFY_EXISTING" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(CupertinoActiveBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = CupertinoActiveBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Verify Current PIN",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter current 4-digit PIN for \"$sessionName\"",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = textColor.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // 4 PIN Dots Indicator
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                for (i in 0 until 4) {
                                    val isFilled = i < existingPinInput.length
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

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = Color(0xFFFF453A)
                                )
                            }

                            // 3x4 Cupertino Keypad
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
                                                            .size(60.dp)
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
                                                            .size(60.dp)
                                                            .clip(CircleShape)
                                                            .clickable {
                                                                if (existingPinInput.isNotEmpty()) {
                                                                    haptics.selection()
                                                                    existingPinInput = existingPinInput.dropLast(1)
                                                                    errorMessage = null
                                                                }
                                                            },
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
                                                            .size(60.dp)
                                                            .clip(CircleShape)
                                                            .background(keyBg)
                                                            .clickable {
                                                                if (existingPinInput.length < 4) {
                                                                    haptics.selection()
                                                                    val next = existingPinInput + key
                                                                    existingPinInput = next
                                                                    errorMessage = null

                                                                    if (next.length == 4) {
                                                                        val isCorrect = onVerifyCurrentPin?.invoke(next) ?: (next.isNotEmpty())
                                                                        if (isCorrect) {
                                                                            haptics.impactLight()
                                                                            errorMessage = null
                                                                            currentPhase = "SET_NEW"
                                                                        } else {
                                                                            haptics.error()
                                                                            errorMessage = "Incorrect current PIN"
                                                                            triggerShake()
                                                                            existingPinInput = ""
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = key,
                                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                                fontSize = 22.sp,
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

                            // Master Recovery Key Reset link
                            TextButton(
                                onClick = {
                                    haptics.selection()
                                    errorMessage = null
                                    currentPhase = "RESET_RECOVERY"
                                }
                            ) {
                                Text(
                                    text = "Forgot PIN? Reset with Master Key",
                                    fontSize = 13.sp,
                                    color = CupertinoActiveBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    "RESET_RECOVERY" -> {
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
                                    text = "Master Recovery Reset",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter your Master Recovery Key to remove the PIN lock on \"$sessionName\".",
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
                                if (recoveryKeyInput.isEmpty()) {
                                    Text(
                                        text = "Master Recovery Key",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                        color = textColor.copy(alpha = 0.4f)
                                    )
                                }
                                BasicTextField(
                                    value = recoveryKeyInput,
                                    onValueChange = {
                                        recoveryKeyInput = it
                                        errorMessage = null
                                    },
                                    textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                                    cursorBrush = SolidColor(CupertinoActiveBlue),
                                    singleLine = true,
                                    visualTransformation = if (showRecoveryKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = if (showRecoveryKey) "Hide Characters" else "Show Characters",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = CupertinoActiveBlue,
                                    modifier = Modifier.clickable { showRecoveryKey = !showRecoveryKey }
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
                                TextButton(onClick = { currentPhase = "VERIFY_EXISTING" }) {
                                    Text("Back", color = CupertinoActiveBlue)
                                }

                                TextButton(
                                    onClick = {
                                        val isValid = onVerifyRecoveryKey?.invoke(recoveryKeyInput) ?: storage.verifyMasterRecoveryKey(recoveryKeyInput)
                                        if (isValid) {
                                            haptics.impactLight()
                                            errorMessage = null
                                            currentPhase = "SET_NEW"
                                        } else {
                                            haptics.error()
                                            errorMessage = "Invalid Master Recovery Key"
                                            triggerShake()
                                        }
                                    }
                                ) {
                                    Text("Reset Lock", fontWeight = FontWeight.Bold, color = CupertinoActiveBlue)
                                }
                            }
                        }
                    }

                    else -> { // "SET_NEW"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(CupertinoActiveBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasExistingPin) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = CupertinoActiveBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (hasExistingPin) "Update PIN for \"$sessionName\"" else "Set 4-Digit PIN",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter a 4-digit code to protect this container.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = textColor.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // 4 PIN Dots Indicator
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                for (i in 0 until 4) {
                                    val isFilled = i < newPinInput.length
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

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = Color(0xFFFF453A)
                                )
                            }

                            // 3x4 Cupertino Keypad
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
                                                            .size(60.dp)
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
                                                            .size(60.dp)
                                                            .clip(CircleShape)
                                                            .clickable {
                                                                if (newPinInput.isNotEmpty()) {
                                                                    haptics.selection()
                                                                    newPinInput = newPinInput.dropLast(1)
                                                                    errorMessage = null
                                                                }
                                                            },
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
                                                            .size(60.dp)
                                                            .clip(CircleShape)
                                                            .background(keyBg)
                                                            .clickable {
                                                                if (newPinInput.length < 4) {
                                                                    haptics.selection()
                                                                    newPinInput += key
                                                                    errorMessage = null
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = key,
                                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                                fontSize = 22.sp,
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

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasExistingPin) {
                                    TextButton(
                                        onClick = {
                                            haptics.impactLight()
                                            onSavePin(null)
                                        }
                                    ) {
                                        Text("Remove PIN", color = Color(0xFFFF453A), fontWeight = FontWeight.Medium)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(1.dp))
                                }

                                TextButton(
                                    onClick = {
                                        if (newPinInput.length != 4) {
                                            haptics.error()
                                            errorMessage = "PIN must be exactly 4 digits"
                                            triggerShake()
                                        } else {
                                            haptics.impactLight()
                                            onSavePin(newPinInput)
                                        }
                                    }
                                ) {
                                    Text("Save PIN", fontWeight = FontWeight.Bold, color = CupertinoActiveBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
