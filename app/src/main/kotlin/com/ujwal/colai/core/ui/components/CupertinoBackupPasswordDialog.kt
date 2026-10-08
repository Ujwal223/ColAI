package com.ujwal.colai.core.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ujwal.colai.core.security.BackupInspection
import com.ujwal.colai.core.security.BackupOptions
import com.ujwal.colai.core.security.RestoreOptions
import com.ujwal.colai.util.rememberHaptics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cupertino Dialog for selecting backup data categories and encrypting with AES-256-GCM.
 */
@Composable
fun CupertinoBackupOptionsDialog(
    onDismissRequest: () -> Unit,
    onSubmit: (password: String, options: BackupOptions) -> Unit,
    isDark: Boolean = true
) {
    val haptics = rememberHaptics()
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var includeLogins by remember { mutableStateOf(true) }
    var includeServices by remember { mutableStateOf(true) }
    var includeSecurity by remember { mutableStateOf(true) }
    var includePreferences by remember { mutableStateOf(true) }

    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF9F9FB)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0x18000000)
    val textColor = if (isDark) Color.White else Color.Black
    val inputBg = if (isDark) Color(0x28FFFFFF) else Color(0x14000000)

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
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(BorderStroke(0.75.dp, cardBorder), RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CupertinoActiveBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CupertinoActiveBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Export Encrypted Backup",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Choose what to backup and protect with AES-256-GCM.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textColor.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )

                    // Options Switches
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(inputBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BackupToggleRow(
                            icon = Icons.Default.Security,
                            title = "Web Logins & Cookies",
                            subtitle = "Stay logged in after restore",
                            checked = includeLogins,
                            onCheckedChange = { includeLogins = it },
                            isDark = isDark
                        )
                        BackupToggleRow(
                            icon = Icons.Default.Layers,
                            title = "AI Services & Profiles",
                            subtitle = "Custom models & account containers",
                            checked = includeServices,
                            onCheckedChange = { includeServices = it },
                            isDark = isDark
                        )
                        BackupToggleRow(
                            icon = Icons.Default.Lock,
                            title = "PIN Locks & Security",
                            subtitle = "Session locks & Master Recovery Key",
                            checked = includeSecurity,
                            onCheckedChange = { includeSecurity = it },
                            isDark = isDark
                        )
                        BackupToggleRow(
                            icon = Icons.Default.Settings,
                            title = "App Preferences",
                            subtitle = "Theme & content blocking settings",
                            checked = includePreferences,
                            onCheckedChange = { includePreferences = it },
                            isDark = isDark
                        )
                    }

                    // Password Inputs
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(inputBg)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (password.isEmpty()) {
                            Text(
                                text = "Enter password",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = textColor.copy(alpha = 0.4f)
                            )
                        }
                        BasicTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                            visualTransformation = PasswordVisualTransformation(),
                            cursorBrush = SolidColor(CupertinoActiveBlue),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(inputBg)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (confirmPassword.isEmpty()) {
                            Text(
                                text = "Confirm password",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = textColor.copy(alpha = 0.4f)
                            )
                        }
                        BasicTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMessage = null
                            },
                            textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                            visualTransformation = PasswordVisualTransformation(),
                            cursorBrush = SolidColor(CupertinoActiveBlue),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
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
                                val cleanPassword = password.trim()
                                val cleanConfirm = confirmPassword.trim()
                                when {
                                    !includeLogins && !includeServices && !includeSecurity && !includePreferences -> {
                                        haptics.error()
                                        errorMessage = "Select at least one category"
                                    }
                                    cleanPassword.isBlank() -> {
                                        haptics.error()
                                        errorMessage = "Password cannot be blank"
                                    }
                                    cleanPassword != cleanConfirm -> {
                                        haptics.error()
                                        errorMessage = "Passwords do not match"
                                    }
                                    else -> {
                                        haptics.impactLight()
                                        onSubmit(
                                            cleanPassword,
                                            BackupOptions(
                                                includeLogins = includeLogins,
                                                includeServices = includeServices,
                                                includeSecurity = includeSecurity,
                                                includePreferences = includePreferences
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "Export",
                                fontWeight = FontWeight.Bold,
                                color = CupertinoActiveBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cupertino Dialog for decrypting, inspecting, and selectively restoring backup data.
 */
@Composable
fun CupertinoRestoreDialog(
    onDismissRequest: () -> Unit,
    onInspectBackup: (password: String, onResult: (Result<BackupInspection>) -> Unit) -> Unit,
    onConfirmRestore: (password: String, options: RestoreOptions) -> Unit,
    isDark: Boolean = true
) {
    val haptics = rememberHaptics()
    var password by remember { mutableStateOf("") }
    var isInspecting by remember { mutableStateOf(false) }
    var inspectionResult by remember { mutableStateOf<BackupInspection?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Selective restore switches
    var restoreLogins by remember { mutableStateOf(true) }
    var restoreServices by remember { mutableStateOf(true) }
    var restoreSecurity by remember { mutableStateOf(true) }
    var restorePreferences by remember { mutableStateOf(true) }

    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF9F9FB)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0x18000000)
    val textColor = if (isDark) Color.White else Color.Black
    val inputBg = if (isDark) Color(0x28FFFFFF) else Color(0x14000000)

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
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(BorderStroke(0.75.dp, cardBorder), RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CupertinoActiveBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CupertinoActiveBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = if (inspectionResult == null) "Decrypt Backup" else "Select What to Restore",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    if (inspectionResult == null) {
                        // STEP 1: Enter password & inspect
                        Text(
                            text = "Enter the password used to encrypt this backup file.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = textColor.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(inputBg)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (password.isEmpty()) {
                                Text(
                                    text = "Enter password",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = textColor.copy(alpha = 0.4f)
                                )
                            }
                            BasicTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMessage = null
                                },
                                textStyle = TextStyle(color = textColor, fontSize = 15.sp),
                                visualTransformation = PasswordVisualTransformation(),
                                cursorBrush = SolidColor(CupertinoActiveBlue),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
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
                                enabled = !isInspecting,
                                onClick = {
                                    val cleanPassword = password.trim()
                                    if (cleanPassword.isBlank()) {
                                        haptics.error()
                                        errorMessage = "Password cannot be blank"
                                        return@TextButton
                                    }
                                    isInspecting = true
                                    errorMessage = null
                                    onInspectBackup(cleanPassword) { result ->
                                        isInspecting = false
                                        if (result.isSuccess) {
                                            val inspection = result.getOrThrow()
                                            inspectionResult = inspection
                                            restoreLogins = inspection.hasLogins
                                            restoreServices = inspection.hasServices
                                            restoreSecurity = inspection.hasSecurity
                                            restorePreferences = inspection.hasPreferences
                                            haptics.selection()
                                        } else {
                                            haptics.error()
                                            errorMessage = "Invalid password or corrupt backup file"
                                        }
                                    }
                                }
                            ) {
                                if (isInspecting) {
                                    CircularProgressIndicator(
                                        color = CupertinoActiveBlue,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Next",
                                        fontWeight = FontWeight.Bold,
                                        color = CupertinoActiveBlue
                                    )
                                }
                            }
                        }
                    } else {
                        // STEP 2: Choose categories to restore
                        val inspection = inspectionResult!!
                        val dateStr = remember(inspection.timestamp) {
                            SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(inspection.timestamp))
                        }

                        Text(
                            text = "Backup Date: \$dateStr\n\${inspection.serviceCount} services, \${inspection.sessionCount} sessions",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textColor.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(inputBg)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (inspection.hasLogins) {
                                BackupToggleRow(
                                    icon = Icons.Default.Security,
                                    title = "Web Logins & Cookies",
                                    subtitle = "Restore logged-in sessions",
                                    checked = restoreLogins,
                                    onCheckedChange = { restoreLogins = it },
                                    isDark = isDark
                                )
                            }
                            if (inspection.hasServices) {
                                BackupToggleRow(
                                    icon = Icons.Default.Layers,
                                    title = "AI Services & Profiles",
                                    subtitle = "Restore accounts & custom services",
                                    checked = restoreServices,
                                    onCheckedChange = { restoreServices = it },
                                    isDark = isDark
                                )
                            }
                            if (inspection.hasSecurity) {
                                BackupToggleRow(
                                    icon = Icons.Default.Lock,
                                    title = "PIN Locks & Security",
                                    subtitle = "Restore PINs & Master Key",
                                    checked = restoreSecurity,
                                    onCheckedChange = { restoreSecurity = it },
                                    isDark = isDark
                                )
                            }
                            if (inspection.hasPreferences) {
                                BackupToggleRow(
                                    icon = Icons.Default.Settings,
                                    title = "App Preferences",
                                    subtitle = "Restore themes & toggles",
                                    checked = restorePreferences,
                                    onCheckedChange = { restorePreferences = it },
                                    isDark = isDark
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { inspectionResult = null }) {
                                Text("Back", color = CupertinoActiveBlue)
                            }

                            TextButton(
                                onClick = {
                                    if (!restoreLogins && !restoreServices && !restoreSecurity && !restorePreferences) {
                                        haptics.error()
                                        errorMessage = "Select at least one category to restore"
                                        return@TextButton
                                    }
                                    haptics.impactLight()
                                    onConfirmRestore(
                                        password.trim(),
                                        RestoreOptions(
                                            restoreLogins = restoreLogins,
                                            restoreServices = restoreServices,
                                            restoreSecurity = restoreSecurity,
                                            restorePreferences = restorePreferences
                                        )
                                    )
                                }
                            ) {
                                Text(
                                    text = "Restore",
                                    fontWeight = FontWeight.Bold,
                                    color = CupertinoActiveBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color.Black
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) CupertinoActiveBlue else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = textColor.copy(alpha = 0.5f)
                )
            }
        }
        CupertinoSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Legacy wrapper maintained for backward compatibility.
 */
@Composable
fun CupertinoBackupPasswordDialog(
    isRestore: Boolean,
    onDismissRequest: () -> Unit,
    onSubmit: (String) -> Unit,
    isDark: Boolean = true
) {
    if (isRestore) {
        CupertinoRestoreDialog(
            onDismissRequest = onDismissRequest,
            onInspectBackup = { _, onResult ->
                onResult(
                    Result.success(
                        BackupInspection(
                            timestamp = System.currentTimeMillis(),
                            hasLogins = true,
                            hasServices = true,
                            hasSecurity = true,
                            hasPreferences = true,
                            serviceCount = 1,
                            sessionCount = 1
                        )
                    )
                )
            },
            onConfirmRestore = { pwd, _ -> onSubmit(pwd) },
            isDark = isDark
        )
    } else {
        CupertinoBackupOptionsDialog(
            onDismissRequest = onDismissRequest,
            onSubmit = { pwd, _ -> onSubmit(pwd) },
            isDark = isDark
        )
    }
}
