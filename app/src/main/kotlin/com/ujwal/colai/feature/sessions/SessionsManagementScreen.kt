package com.ujwal.colai.feature.sessions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import com.ujwal.colai.core.ui.components.CupertinoActiveBlue
import com.ujwal.colai.core.ui.components.CupertinoSetPinDialog
import com.ujwal.colai.core.ui.components.CupertinoAlertDialog
import com.ujwal.colai.core.ui.components.CupertinoDarkBg
import com.ujwal.colai.core.ui.components.CupertinoDestructiveRed
import com.ujwal.colai.core.ui.components.CupertinoFormRow
import com.ujwal.colai.core.ui.components.CupertinoFormSection
import com.ujwal.colai.core.ui.components.CupertinoLightBg
import com.ujwal.colai.core.ui.components.CupertinoNavigationBar
import com.ujwal.colai.core.ui.components.CupertinoSystemGrey
import com.ujwal.colai.feature.home.ServiceCardIcon
import com.ujwal.colai.util.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EXACT Cupertino SessionsManagementScreen:
 * - Inset grouped sections per AI service
 * - Each row: 32x32 rounded icon, account name, last used date, default star, delete trash icon
 * - Destructive "Delete All Sessions" button at bottom
 * - Cupertino confirmation dialogs
 */
@Composable
fun SessionsManagementScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    val servicesFlow = remember { db.serviceDao().getAllServices() }
    val sessionsFlow = remember { db.sessionDao().getAllSessions() }

    val services by servicesFlow.collectAsState(initial = emptyList())
    val sessions by sessionsFlow.collectAsState(initial = emptyList())

    val storage = remember { EncryptedStorage.getInstance(context) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }
    var sessionForPin by remember { mutableStateOf<Session?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val isHighContrast = com.ujwal.colai.core.ui.theme.LocalContrastLevel.current == "high"
    val scaffoldBg = if (isDarkTheme) {
        if (isHighContrast) Color.Black else CupertinoDarkBg
    } else {
        if (isHighContrast) Color.White else CupertinoLightBg
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scaffoldBg,
        topBar = {
            CupertinoNavigationBar(
                title = "Manage Sessions",
                leading = {
                    IconButton(
                        onClick = {
                            haptics.selection()
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDarkTheme) Color.White else Color.Black
                        )
                    }
                },
                textColor = if (isDarkTheme) Color.White else Color.Black
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            services.forEach { service ->
                val serviceSessions = sessions.filter { it.serviceId == service.id }
                if (serviceSessions.isNotEmpty()) {
                    CupertinoFormSection(
                        header = service.name.uppercase(),
                        isDark = isDarkTheme
                    ) {
                        serviceSessions.forEachIndexed { index, session ->
                            CupertinoFormRow(
                                prefix = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // 32x32 rounded logo
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp))
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDarkTheme) Color(0xFF2C2C2E) else Color.White)
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ServiceCardIcon(
                                                service = service,
                                                isDark = isDarkTheme
                                            )
                                        }

                                        val isLocked = storage.isSessionPinLocked(session.id)
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = session.accountName,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = if (isDarkTheme) Color.White else Color.Black
                                                )
                                                if (isLocked) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = "PIN Protected",
                                                        tint = CupertinoActiveBlue,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Last used: ${formatLastUsed(session.lastAccessed)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = CupertinoSystemGrey
                                            )
                                        }
                                    }
                                },
                                showDivider = index < serviceSessions.size - 1,
                                isDark = isDarkTheme
                            ) {
                                val isLocked = storage.isSessionPinLocked(session.id)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (session.isDefault) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Default",
                                            tint = Color(0xFFFFCC00),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            haptics.selection()
                                            sessionForPin = session
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = if (isLocked) "Change PIN" else "Set PIN",
                                            tint = if (isLocked) CupertinoActiveBlue else CupertinoSystemGrey,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            haptics.impactHeavy()
                                            sessionToDelete = session
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = CupertinoDestructiveRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delete All Sessions Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = {
                        haptics.impactHeavy()
                        showDeleteAllDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CupertinoDestructiveRed)
                ) {
                    Text(
                        text = "Delete All Sessions",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
        }
    }

    // Confirm Single Delete Dialog
    sessionToDelete?.let { session ->
        CupertinoAlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = "Delete Session?",
            message = "This will permanently delete authentication for \"${session.accountName}\" on this device.",
            confirmTitle = "Delete",
            cancelTitle = "Cancel",
            isDestructive = true,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    db.sessionDao().deleteSession(session)
                }
                sessionToDelete = null
            },
            isDark = isDarkTheme
        )
    }

    // Confirm Delete All Dialog
    if (showDeleteAllDialog) {
        CupertinoAlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = "Clear All Data?",
            message = "This will log you out of all AI services and delete all cached session data. This cannot be undone.",
            confirmTitle = "Delete All",
            cancelTitle = "Cancel",
            isDestructive = true,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    db.sessionDao().deleteAllSessions()
                }
                showDeleteAllDialog = false
            },
            isDark = isDarkTheme
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
            isDark = isDarkTheme
        )
    }
}

private fun formatLastUsed(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val diffMin = diffMs / (1000 * 60)
    val diffHour = diffMin / 60

    return when {
        diffMin < 60 -> "${diffMin.coerceAtLeast(1)}m ago"
        diffHour < 24 -> "${diffHour}h ago"
        else -> SimpleDateFormat("M/d", Locale.getDefault()).format(Date(timestamp))
    }
}
