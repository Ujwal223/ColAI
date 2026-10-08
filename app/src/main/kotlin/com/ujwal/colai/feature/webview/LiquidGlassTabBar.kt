package com.ujwal.colai.feature.webview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import com.ujwal.colai.core.ui.components.ActionSheetAction
import com.ujwal.colai.core.ui.components.CupertinoActionSheetDialog
import com.ujwal.colai.core.ui.components.CupertinoActiveBlue
import com.ujwal.colai.core.ui.components.CupertinoAlertDialog
import com.ujwal.colai.core.ui.components.CupertinoSetPinDialog
import com.ujwal.colai.util.rememberHaptics
import kotlinx.coroutines.launch

/**
 * EXACT Cupertino Top Navigation Bar:
 * - Leading: chevron_back (if canGoBack -> goBack; else onBackToHome)
 * - Middle: "ColAI"
 * - Trailing: refresh icon with rotation animation during reload
 * - Dynamic background & text color adaptation
 * - Integrated 2.5dp linear progress indicator right below the bar
 */
@Composable
fun CupertinoWebViewTopBar(
    canGoBack: Boolean,
    isLoading: Boolean,
    progress: Float,
    isDark: Boolean,
    activeBgColor: Color?,
    activeTextColor: Color?,
    onGoBack: () -> Unit,
    onBackToHome: () -> Unit,
    onReload: () -> Unit,
    serviceName: String = "ColAI",
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(durationMillis = 800, easing = LinearEasing)
            )
        }
    }

    val bgColor = activeBgColor ?: (if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF))
    val isBgDark = ThemeColorAdapter.isColorDark(bgColor)
    val textColor = activeTextColor ?: (if (isBgDark) Color.White else Color(0xFF111114))
    val dividerColor = (if (isBgDark) Color.White else Color.Black).copy(alpha = 0.04f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Leading Chevron Back Button
            IconButton(
                onClick = {
                    haptics.selection()
                    onBackToHome()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Middle Title: AI Service Name or "ColAI"
            Text(
                text = serviceName,
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )

            // Trailing Refresh Button with rotation
            IconButton(
                onClick = {
                    haptics.selection()
                    scope.launch {
                        rotation.animateTo(
                            targetValue = rotation.value + 360f,
                            animationSpec = tween(durationMillis = 700, easing = LinearEasing)
                        )
                    }
                    onReload()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = textColor,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation.value)
                )
            }
        }

        // Linear Progress Bar (2.5dp height right below top bar when progress > 0 and < 1)
        if (isLoading && progress in 0.01f..0.99f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = CupertinoActiveBlue,
                trackColor = Color.Transparent
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
    }
}

/**
 * EXACT Cupertino Bottom Session Toolbar:
 * - SingleChildScrollView horizontal
 * - Session tabs with active accent fill (12dp radius) and ghost outline for inactive
 * - Long-press on session tab shows CupertinoActionSheet to delete session
 * - Trailing Add Session button (CupertinoIcons.add_circled) opens "New Account Session" dialog
 */
@Composable
fun CupertinoSessionBottomBar(
    service: AIService?,
    activeSession: Session?,
    sessions: List<Session>,
    isDark: Boolean,
    activeBgColor: Color?,
    activePrimaryColor: Color?,
    activeTextColor: Color?,
    onSelectSession: (Session) -> Unit,
    onAddSession: (String) -> Unit,
    onDeleteSession: (Session) -> Unit,
    onClearSessionData: ((Session) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val storage = remember { EncryptedStorage.getInstance(context) }

    var showAddDialog by remember { mutableStateOf(false) }
    var sessionForActions by remember { mutableStateOf<Session?>(null) }
    var sessionForPin by remember { mutableStateOf<Session?>(null) }
    var sessionToClearData by remember { mutableStateOf<Session?>(null) }
    var newAccountNameInput by remember { mutableStateOf("") }

    val barBg = activeBgColor ?: (if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF))
    val isBarDark = ThemeColorAdapter.isColorDark(barBg)
    val contentColor = activeTextColor ?: (if (isBarDark) Color.White else Color(0xFF111114))
    val primaryColor = activePrimaryColor ?: CupertinoActiveBlue
    val topBorderColor = (if (isBarDark) Color.White else Color.Black).copy(alpha = 0.04f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = topBorderColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sessions.forEach { session ->
                val isActive = session.id == activeSession?.id
                val isLocked = remember(session.id, sessionForPin) {
                    storage.isSessionPinLocked(session.id)
                }
                val tabShape = RoundedCornerShape(12.dp)

                val tabBg = if (isActive) primaryColor else (if (isBarDark) Color(0x1AFFFFFF) else Color(0x0C000000))
                val tabBorder = if (isActive) null else (if (isBarDark) Color(0x33FFFFFF) else Color(0x1F000000))
                val tabText = if (isActive) Color.White else contentColor.copy(alpha = 0.85f)

                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(tabShape)
                        .then(
                            if (tabBorder != null) {
                                Modifier.border(width = 1.dp, color = tabBorder, shape = tabShape)
                            } else Modifier
                        )
                        .background(tabBg)
                        .pointerInput(session.id) {
                            detectTapGestures(
                                onTap = {
                                    haptics.selection()
                                    onSelectSession(session)
                                },
                                onLongPress = {
                                    haptics.impactMedium()
                                    sessionForActions = session
                                }
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = session.accountName,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = tabText
                        )
                        if (isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "PIN Locked",
                                tint = tabText.copy(alpha = 0.75f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Add Session Button (28dp)
            IconButton(
                onClick = {
                    haptics.impactLight()
                    newAccountNameInput = ""
                    showAddDialog = true
                },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "New Session",
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // Add Session Cupertino AlertDialog
    if (showAddDialog) {
        CupertinoAlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newAccountNameInput = ""
            },
            title = "New Account Session",
            confirmTitle = "Create",
            cancelTitle = "Cancel",
            onConfirm = {
                val trimmed = newAccountNameInput.trim()
                if (trimmed.isNotBlank()) {
                    onAddSession(trimmed)
                    showAddDialog = false
                    newAccountNameInput = ""
                }
            },
            isDark = isDark,
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (newAccountNameInput.isEmpty()) {
                        Text(
                            text = "Account Name (e.g. Work, Personal)",
                            style = TextStyle(fontSize = 13.sp, color = Color(0xFF8E8E93))
                        )
                    }
                    BasicTextField(
                        value = newAccountNameInput,
                        onValueChange = { newAccountNameInput = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(CupertinoActiveBlue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    // Session Long-Press Cupertino Action Sheet
    sessionForActions?.let { session ->
        val isLocked = storage.isSessionPinLocked(session.id)
        val actions = buildList {
            add(
                ActionSheetAction(
                    title = if (isLocked) "Manage / Remove PIN Lock" else "Add PIN Lock",
                    onClick = {
                        sessionForActions = null
                        sessionForPin = session
                    }
                )
            )
            add(
                ActionSheetAction(
                    title = "Clear Cookies & Storage",
                    isDestructive = true,
                    onClick = {
                        sessionForActions = null
                        sessionToClearData = session
                    }
                )
            )
            if (sessions.size > 1) {
                add(
                    ActionSheetAction(
                        title = "Delete Account Session",
                        isDestructive = true,
                        onClick = {
                            sessionForActions = null
                            onDeleteSession(session)
                        }
                    )
                )
            }
        }

        CupertinoActionSheetDialog(
            onDismissRequest = { sessionForActions = null },
            title = "Account: ${session.accountName}",
            message = "Manage security lock, storage isolation, and account state",
            actions = actions,
            isDark = isDark
        )
    }

    // Clear Session Data Confirmation Alert
    sessionToClearData?.let { session ->
        CupertinoAlertDialog(
            onDismissRequest = { sessionToClearData = null },
            title = "Clear Account Data?",
            message = "This will wipe all cookies, cached files, and local storage for \"${session.accountName}\". Other accounts will remain completely untouched.",
            confirmTitle = "Clear Data",
            cancelTitle = "Cancel",
            isDestructive = true,
            onConfirm = {
                onClearSessionData?.invoke(session)
                sessionToClearData = null
            },
            isDark = isDark
        )
    }

    // Set/Change PIN Dialog
    sessionForPin?.let { targetSession ->
        CupertinoSetPinDialog(
            sessionName = targetSession.accountName,
            hasExistingPin = storage.isSessionPinLocked(targetSession.id),
            onDismissRequest = { sessionForPin = null },
            onSavePin = { pin ->
                if (pin != null) {
                    storage.setSessionPin(targetSession.id, pin)
                } else {
                    storage.removeSessionPin(targetSession.id)
                }
                sessionForPin = null
            },
            onVerifyCurrentPin = { pin -> storage.verifySessionPin(targetSession.id, pin) },
            onVerifyRecoveryKey = { key -> storage.verifyMasterRecoveryKey(key) },
            isDark = isDark
        )
    }
}
