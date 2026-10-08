package com.ujwal.colai.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
import com.ujwal.colai.core.engine.GeckoSessionPool
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import com.ujwal.colai.core.ui.components.CupertinoActiveBlue
import com.ujwal.colai.core.ui.components.CupertinoAlertDialog
import com.ujwal.colai.core.ui.components.CupertinoDarkBg
import com.ujwal.colai.core.ui.components.CupertinoDestructiveRed
import com.ujwal.colai.core.ui.components.CupertinoFormRow
import com.ujwal.colai.core.ui.components.CupertinoFormSection
import com.ujwal.colai.core.ui.components.CupertinoLightBg
import com.ujwal.colai.core.ui.components.CupertinoNavigationBar
import com.ujwal.colai.core.ui.components.CupertinoResetWithRecoveryKeyDialog
import com.ujwal.colai.core.ui.components.CupertinoSetPinDialog
import com.ujwal.colai.core.ui.components.CupertinoSetupRecoveryKeyDialog
import com.ujwal.colai.core.ui.components.CupertinoSwitch
import com.ujwal.colai.core.ui.components.CupertinoSystemGrey
import com.ujwal.colai.feature.home.ServiceCardIcon
import com.ujwal.colai.util.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dedicated Cupertino Settings Screen for managing Container Session Locks,
 * Master Recovery Key configuration, and Per-Account privacy controls.
 */
@Composable
fun SessionLocksScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val storage = remember { EncryptedStorage.getInstance(context) }
    val db = remember { AppDatabase.getDatabase(context) }

    val servicesFlow = remember { db.serviceDao().getAllServices() }
    val sessionsFlow = remember { db.sessionDao().getAllSessions() }

    val services by servicesFlow.collectAsState(initial = emptyList())
    val sessions by sessionsFlow.collectAsState(initial = emptyList())

    var hasMasterKey by remember { mutableStateOf(storage.hasMasterRecoveryKey()) }
    var showSetupMasterKeyDialog by remember { mutableStateOf(false) }

    var targetSessionForPin by remember { mutableStateOf<Session?>(null) }
    var targetSessionToReset by remember { mutableStateOf<Session?>(null) }
    var sessionToClearData by remember { mutableStateOf<Session?>(null) }

    // Content blocking state tracker for UI reactivity
    var contentBlockingToggles by remember {
        mutableStateOf(sessions.associate { it.id to storage.isSessionContentBlockingEnabled(it.id) })
    }

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
                title = "Session Locks",
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
            // SECTION 1: MASTER RECOVERY KEY
            CupertinoFormSection(
                header = "SECURITY RECOVERY",
                isDark = isDarkTheme
            ) {
                CupertinoFormRow(
                    prefix = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CupertinoActiveBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Master Recovery Key",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = if (isDarkTheme) Color.White else Color.Black
                                )
                                Text(
                                    text = if (hasMasterKey) "Configured (universal lock reset)" else "Not Set (tap to configure)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = if (hasMasterKey) Color(0xFF34C759) else Color(0xFFFF9500)
                                )
                            }
                        }
                    },
                    onClick = {
                        haptics.selection()
                        showSetupMasterKeyDialog = true
                    },
                    showDivider = false,
                    isDark = isDarkTheme
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (hasMasterKey) "Change" else "Configure",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = CupertinoActiveBlue
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = CupertinoSystemGrey,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // SECTION 2: PER-SESSION LOCKS & DATA CONTROLS
            services.forEach { service ->
                val serviceSessions = sessions.filter { it.serviceId == service.id }
                if (serviceSessions.isNotEmpty()) {
                    CupertinoFormSection(
                        header = "${service.name.uppercase()} SESSIONS",
                        isDark = isDarkTheme
                    ) {
                        serviceSessions.forEachIndexed { index, session ->
                            val isLocked = storage.isSessionPinLocked(session.id)
                            val isContentBlocking = storage.isSessionContentBlockingEnabled(session.id)

                            CupertinoFormRow(
                                prefix = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
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
                                                text = if (isLocked) "Protected with PIN" else "Unlocked",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = if (isLocked) CupertinoActiveBlue else CupertinoSystemGrey
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    haptics.selection()
                                    targetSessionForPin = session
                                },
                                showDivider = index < serviceSessions.size - 1,
                                isDark = isDarkTheme
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Clear Data Icon
                                    IconButton(
                                        onClick = {
                                            haptics.impactMedium()
                                            sessionToClearData = session
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CleaningServices,
                                            contentDescription = "Clear Session Data",
                                            tint = CupertinoSystemGrey,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Lock Toggle Icon
                                    IconButton(
                                        onClick = {
                                            haptics.impactLight()
                                            targetSessionForPin = session
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
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
        }
    }

    // Setup Master Recovery Key Dialog
    if (showSetupMasterKeyDialog) {
        CupertinoSetupRecoveryKeyDialog(
            onDismissRequest = { showSetupMasterKeyDialog = false },
            onKeySaved = { key ->
                storage.setMasterRecoveryKey(key)
                hasMasterKey = true
                showSetupMasterKeyDialog = false
            },
            isDark = isDarkTheme
        )
    }

    // Set / Change / Remove PIN Dialog (With Strict Lock Verification & Recovery)
    targetSessionForPin?.let { targetSession ->
        CupertinoSetPinDialog(
            sessionName = targetSession.accountName,
            hasExistingPin = storage.isSessionPinLocked(targetSession.id),
            onDismissRequest = { targetSessionForPin = null },
            onSavePin = { newPin ->
                storage.setSessionPin(targetSession.id, newPin)
                targetSessionForPin = null
            },
            onVerifyCurrentPin = { inputPin ->
                storage.verifySessionPin(targetSession.id, inputPin)
            },
            onVerifyRecoveryKey = { inputKey ->
                storage.verifyMasterRecoveryKey(inputKey)
            },
            isDark = isDarkTheme
        )
    }

    // Reset with Master Recovery Key Dialog
    targetSessionToReset?.let { session ->
        CupertinoResetWithRecoveryKeyDialog(
            sessionName = session.accountName,
            onDismissRequest = { targetSessionToReset = null },
            onVerifyRecoveryKey = { key -> storage.verifyMasterRecoveryKey(key) },
            onRecoverySuccess = {
                storage.setSessionPin(session.id, null)
                targetSessionToReset = null
            },
            isDark = isDarkTheme
        )
    }

    // Confirm Clear Data Dialog for Single Account (Item 2)
    sessionToClearData?.let { session ->
        CupertinoAlertDialog(
            onDismissRequest = { sessionToClearData = null },
            title = "Clear Data for \"${session.accountName}\"?",
            message = "This will erase cookies, cache, and local storage for this container only. Other accounts remain untouched.",
            confirmTitle = "Clear Data",
            cancelTitle = "Cancel",
            isDestructive = true,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    GeckoSessionPool.getInstance().clearContainerStorage(session.id)
                    storage.clearSessionState(session.id)
                }
                sessionToClearData = null
            },
            isDark = isDarkTheme
        )
    }
}
