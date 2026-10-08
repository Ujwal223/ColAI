package com.ujwal.colai.feature.sessions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import com.ujwal.colai.core.ui.components.CupertinoActiveBlue
import com.ujwal.colai.core.ui.components.CupertinoPinLockDialog
import com.ujwal.colai.core.ui.components.CupertinoSetPinDialog
import com.ujwal.colai.core.ui.components.LiquidGlassBox
import com.ujwal.colai.core.ui.theme.ColAITheme
import com.ujwal.colai.core.ui.theme.SquircleShape
import com.ujwal.colai.feature.home.ServiceIcon
import com.ujwal.colai.util.rememberHaptics

/**
 * Liquid Glass Bottom Sheet for managing multi-account container profiles for an AI Service.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionManagementSheet(
    service: AIService,
    viewModel: SessionsViewModel,
    onDismissRequest: () -> Unit,
    onSessionSelected: (Session) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storage = remember { EncryptedStorage.getInstance(context) }
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()

    var newAccountName by remember { mutableStateOf("") }
    var sessionToRename by remember { mutableStateOf<Session?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var sessionForPin by remember { mutableStateOf<Session?>(null) }
    var sessionToUnlock by remember { mutableStateOf<Session?>(null) }

    LaunchedEffect(service.id) {
        viewModel.loadSessionsForService(service.id)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = ColAITheme.glassColors.surfaceElevated,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header: Service Icon, Title, and Container Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ServiceIcon(service = service, size = 38.dp)
                    Column {
                        Text(
                            text = service.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "${sessions.size} Isolated Containers",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptics.selection()
                        onDismissRequest()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Container list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = sessions,
                    key = { it.id }
                ) { session ->
                    val isActive = session.id == activeSessionId

                    val isLocked = remember(session.id) {
                        storage.isSessionPinLocked(session.id)
                    }

                    LiquidGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SquircleShape(16.dp),
                        backgroundColor = if (isActive) ColAITheme.glassColors.surfaceElevated else ColAITheme.glassColors.surface,
                        borderWidth = if (isActive) 1.dp else 0.5.dp,
                        onClick = {
                            if (isLocked && !isActive) {
                                sessionToUnlock = session
                            } else {
                                haptics.selection()
                                viewModel.switchActiveSession(service.id, session.id)
                                onSessionSelected(session)
                                onDismissRequest()
                            }
                        },
                        onLongClick = {
                            haptics.impactMedium()
                            sessionForPin = session
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Active Indicator + Session Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) Color(0xFF10B981) else Color.White.copy(alpha = 0.25f))
                                )

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = session.accountName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (isLocked) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "PIN Locked",
                                                tint = CupertinoActiveBlue,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        if (session.isDefault) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(SquircleShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Default",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Right: Action Buttons (Star default, Edit rename, Delete)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Default Toggle
                                IconButton(
                                    onClick = {
                                        haptics.selection()
                                        viewModel.setDefaultSession(service.id, session.id)
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (session.isDefault) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Set Default",
                                        tint = if (session.isDefault) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Lock / PIN Button
                                IconButton(
                                    onClick = {
                                        haptics.impactLight()
                                        sessionForPin = session
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = if (isLocked) "Change PIN" else "Set PIN",
                                        tint = if (isLocked) CupertinoActiveBlue else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                // Rename Button
                                IconButton(
                                    onClick = {
                                        haptics.impactLight()
                                        sessionToRename = session
                                        renameInputText = session.accountName
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                // Delete Button (allow only if more than 1 session)
                                if (sessions.size > 1) {
                                    IconButton(
                                        onClick = {
                                            haptics.impactHeavy()
                                            viewModel.deleteSession(service.id, session.id)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add New Container Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = SquircleShape(14.dp),
                    backgroundColor = Color.White.copy(alpha = 0.06f),
                    borderWidth = 0.5.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (newAccountName.isEmpty()) {
                            Text(
                                text = "New container name (e.g. Work)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                        BasicTextField(
                            value = newAccountName,
                            onValueChange = { newAccountName = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                LiquidGlassBox(
                    shape = SquircleShape(14.dp),
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        val name = newAccountName.trim()
                        if (name.isNotBlank()) {
                            viewModel.createNewSession(
                                serviceId = service.id,
                                name = name,
                                onCreated = { createdSession ->
                                    haptics.impactLight()
                                    newAccountName = ""
                                },
                                onError = { errorMsg ->
                                    haptics.error()
                                    android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Rename Dialog
    sessionToRename?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = {
                Text(text = "Rename Container", style = MaterialTheme.typography.titleMedium, color = Color.White)
            },
            text = {
                LiquidGlassBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = SquircleShape(12.dp),
                    backgroundColor = Color.White.copy(alpha = 0.08f)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                        BasicTextField(
                            value = renameInputText,
                            onValueChange = { renameInputText = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = renameInputText.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.renameSession(
                                session = session,
                                newName = trimmed,
                                onSuccess = {
                                    sessionToRename = null
                                },
                                onError = { errorMsg ->
                                    haptics.error()
                                    android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = ColAITheme.glassColors.surfaceElevated
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
                viewModel.loadSessionsForService(service.id)
            },
            onVerifyCurrentPin = { pin -> storage.verifySessionPin(targetSession.id, pin) },
            onVerifyRecoveryKey = { key -> storage.verifyMasterRecoveryKey(key) },
            isDark = true
        )
    }

    // Unlock PIN Dialog
    sessionToUnlock?.let { targetSession ->
        CupertinoPinLockDialog(
            sessionName = targetSession.accountName,
            onDismissRequest = { sessionToUnlock = null },
            onVerifyPin = { pin -> storage.verifySessionPin(targetSession.id, pin) },
            onSuccess = {
                sessionToUnlock = null
                viewModel.switchActiveSession(service.id, targetSession.id)
                onSessionSelected(targetSession)
                onDismissRequest()
            },
            isDark = true
        )
    }
}
