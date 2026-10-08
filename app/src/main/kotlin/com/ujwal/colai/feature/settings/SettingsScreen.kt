package com.ujwal.colai.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.ujwal.colai.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.BackupManager
import com.ujwal.colai.core.security.EncryptedStorage
import com.ujwal.colai.core.ui.components.ActionSheetAction
import com.ujwal.colai.core.ui.components.CupertinoActionSheetDialog
import com.ujwal.colai.core.ui.components.CupertinoActiveBlue
import com.ujwal.colai.feature.widget.ServiceWidgetProvider
import com.ujwal.colai.feature.widget.ServiceWidgetMediumProvider
import com.ujwal.colai.core.ui.components.CupertinoBackupOptionsDialog
import com.ujwal.colai.core.ui.components.CupertinoRestoreDialog
import com.ujwal.colai.core.ui.components.CupertinoAlertDialog
import com.ujwal.colai.util.AppRestartHelper
import com.ujwal.colai.core.ui.components.CupertinoDarkBg
import com.ujwal.colai.core.ui.components.CupertinoFormRow
import com.ujwal.colai.core.ui.components.CupertinoFormSection
import com.ujwal.colai.core.ui.components.CupertinoLightBg
import com.ujwal.colai.core.ui.components.CupertinoNavigationBar
import com.ujwal.colai.core.ui.components.CupertinoSlidingSegmentedControl
import com.ujwal.colai.core.ui.components.CupertinoSwitch
import com.ujwal.colai.core.ui.components.CupertinoSystemGrey
import com.ujwal.colai.util.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * EXACT Cupertino SettingsScreen:
 * - APPEARANCE: Dark Mode toggle, Contrast segmented control (Relaxed / High)
 * - BROWSING: Enable SSO Login toggle with helper explanation
 * - MANAGEMENT:
 *   - Sessions -> Manage (chevron) -> opens SessionsManagementScreen
 *   - Session Locks -> Configure (chevron) -> opens SessionLocksScreen
 *   - Notifications -> Configure (chevron) -> opens NotificationSettingsScreen
 *   - About ColAI -> v1.0.1 (chevron) -> opens AboutScreen
 * - BACKUP & RESTORE:
 *   - Create Encrypted Backup
 *   - Restore from Backup
 * - WIDGET CONFIGURATION:
 *   - Widget Opens -> displays current selection, opens Cupertino Action Sheet to select target service & session
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenSessionsManagement: () -> Unit,
    onOpenSessionLocks: () -> Unit,
    onOpenNotificationsSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onThemeChanged: ((String) -> Unit)? = null,
    onContrastChanged: ((String) -> Unit)? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val storage = remember { EncryptedStorage.getInstance(context) }
    val db = remember { AppDatabase.getDatabase(context) }

    var contrastLevel by remember { mutableStateOf(storage.getContrastLevel()) }

    var services by remember { mutableStateOf<List<AIService>>(emptyList()) }
    var allSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var showWidgetPicker by remember { mutableStateOf(false) }

    var pendingBackupUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showBackupPasswordDialog by remember { mutableStateOf(false) }
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var restoredSessionCount by remember { mutableStateOf(0) }
    var closingCountdown by remember { mutableStateOf<Int?>(null) }
    var restoreCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(closingCountdown, restoreCompleted) {
        val count = closingCountdown ?: return@LaunchedEffect
        if (count > 1) {
            delay(1000L)
            closingCountdown = count - 1
        } else if (count == 1) {
            if (restoreCompleted) {
                delay(1000L)
                closingCountdown = 0
                AppRestartHelper.restartApp(context)
            }
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            pendingBackupUri = uri
            showBackupPasswordDialog = true
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestorePasswordDialog = true
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            services = db.serviceDao().getAllServicesList()
            allSessions = db.sessionDao().getAllSessionsList()
        }
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
                title = "Settings",
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
            // SECTION 1: APPEARANCE
            CupertinoFormSection(
                header = "APPEARANCE",
                isDark = isDarkTheme
            ) {
                // Dark Mode Switch - Instant reactivity
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Dark Mode",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    isDark = isDarkTheme
                ) {
                    CupertinoSwitch(
                        checked = isDarkTheme,
                        onCheckedChange = { checked ->
                            val modeKey = if (checked) "dark" else "light"
                            storage.setThemeMode(modeKey)
                            onThemeChanged?.invoke(modeKey)
                        }
                    )
                }

                // Contrast Segmented Control (Relaxed / High)
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Contrast",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    showDivider = false,
                    isDark = isDarkTheme
                ) {
                    CupertinoSlidingSegmentedControl(
                        items = listOf("relaxed" to "Relaxed", "high" to "High"),
                        selectedItem = contrastLevel,
                        onItemSelected = { level ->
                            contrastLevel = level
                            storage.setContrastLevel(level)
                            onContrastChanged?.invoke(level)
                        },
                        isDark = isDarkTheme
                    )
                }
            }

            // SECTION 2: MANAGEMENT
            CupertinoFormSection(
                header = "MANAGEMENT",
                isDark = isDarkTheme
            ) {
                // Sessions -> Manage
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Sessions",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    onClick = onOpenSessionsManagement,
                    isDark = isDarkTheme
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Manage",
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

                // Session Locks -> Configure
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Session Locks",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    onClick = onOpenSessionLocks,
                    isDark = isDarkTheme
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (storage.hasMasterRecoveryKey()) "Secured" else "Configure",
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

                // Share Personal Cookies
                var sharedPersonalCookies by remember { mutableStateOf(storage.isSharedPersonalCookiesEnabled()) }
                CupertinoFormRow(
                    prefix = {
                        Column {
                            Text(
                                text = "Share Personal Cookies",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                color = if (isDarkTheme) Color.White else Color.Black
                            )
                            Text(
                                text = "Share Google/SSO login across default personal sessions",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = CupertinoSystemGrey
                            )
                        }
                    },
                    isDark = isDarkTheme
                ) {
                    CupertinoSwitch(
                        checked = sharedPersonalCookies,
                        onCheckedChange = { checked ->
                            sharedPersonalCookies = checked
                            storage.setSharedPersonalCookiesEnabled(checked)
                            haptics.selection()
                        }
                    )
                }

                // Notifications -> Configure
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    onClick = onOpenNotificationsSettings,
                    isDark = isDarkTheme
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Configure",
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

                // About ColAI -> v1.0.1
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "About ColAI",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    onClick = onOpenAbout,
                    showDivider = false,
                    isDark = isDarkTheme
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = CupertinoSystemGrey
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

            // SECTION 4: WIDGET CONFIGURATION
            if (services.isNotEmpty()) {
                val widgetServiceId = storage.getWidgetServiceId()
                val configuredService = if (!widgetServiceId.isNullOrBlank()) {
                    services.find { it.id == widgetServiceId }
                } else null ?: services.find { it.widgetSessionId != null } ?: services.firstOrNull()

                val widgetSessionId = storage.getWidgetSessionId() ?: configuredService?.widgetSessionId
                val configuredSession = if (widgetSessionId != null) {
                    allSessions.find { it.id == widgetSessionId }
                } else {
                    allSessions.find { it.serviceId == configuredService?.id }
                }

                val displayText = if (configuredService != null && configuredSession != null) {
                    "${configuredService.name} (${configuredSession.accountName})"
                } else if (configuredService != null) {
                    "${configuredService.name} (No Sessions)"
                } else {
                    "None"
                }

                CupertinoFormSection(
                    header = "WIDGET CONFIGURATION",
                    isDark = isDarkTheme
                ) {
                    CupertinoFormRow(
                        prefix = {
                            Text(
                                text = "Widget Opens",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                color = if (isDarkTheme) Color.White else Color.Black
                            )
                        },
                        showDivider = false,
                        onClick = {
                            showWidgetPicker = true
                        },
                        isDark = isDarkTheme
                    ) {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    }
                }
            }

            // SECTION 5: BACKUP & RESTORE
            CupertinoFormSection(
                header = "BACKUP & RESTORE",
                isDark = isDarkTheme
            ) {
                // Export Backup
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Create Encrypted Backup",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    onClick = {
                        haptics.selection()
                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                        createBackupLauncher.launch("colai_backup_$timestamp.colai")
                    },
                    isDark = isDarkTheme
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = CupertinoSystemGrey,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Restore Backup
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Restore from Backup",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    onClick = {
                        haptics.selection()
                        restoreBackupLauncher.launch(arrayOf("*/*"))
                    },
                    showDivider = false,
                    isDark = isDarkTheme
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = CupertinoSystemGrey,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // SUPPORT COLAI DONATION BANNER
            DonationBanner(isDark = isDarkTheme)

            Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
        }
    }

    // Cupertino Action Sheet for Widget Target Selection
    if (showWidgetPicker) {
        val pickerActions = mutableListOf<ActionSheetAction>()

        services.forEach { service ->
            val serviceSessions = allSessions.filter { it.serviceId == service.id }
            if (serviceSessions.isEmpty()) {
                pickerActions.add(
                    ActionSheetAction(
                        title = "${service.name} (No Sessions)",
                        onClick = {}
                    )
                )
            } else {
                serviceSessions.forEach { session ->
                    pickerActions.add(
                        ActionSheetAction(
                            title = "${service.name} - ${session.accountName}",
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val allDbServices = db.serviceDao().getAllServicesList()
                                    // Clear widgetSessionId from all services in DB
                                    allDbServices.forEach { s ->
                                        if (s.widgetSessionId != null) {
                                            db.serviceDao().updateService(s.copy(widgetSessionId = null))
                                        }
                                    }
                                    // Set widgetSessionId on selected service
                                    val updated = service.copy(widgetSessionId = session.id)
                                    db.serviceDao().updateService(updated)
                                    storage.setWidgetTarget(service.id, session.id)
                                    storage.setActiveServiceId(service.id)
                                    storage.setActiveSessionId(session.id)

                                    val freshServices = db.serviceDao().getAllServicesList()
                                    val freshSessions = db.sessionDao().getAllSessionsList()
                                    withContext(Dispatchers.Main) {
                                        services = freshServices
                                        allSessions = freshSessions
                                        showWidgetPicker = false
                                        haptics.success()
                                    }
                                    ServiceWidgetProvider.updateAll(context)
                                    ServiceWidgetMediumProvider.updateAll(context)
                                }
                            }
                        )
                    )
                }
            }
        }

        CupertinoActionSheetDialog(
            onDismissRequest = { showWidgetPicker = false },
            title = "Select Widget Target",
            message = "Which service and account should the widget open?",
            actions = pickerActions,
            isDark = isDarkTheme
        )
    }

    // Encrypted Backup Export Options & Password Dialog
    if (showBackupPasswordDialog && pendingBackupUri != null) {
        CupertinoBackupOptionsDialog(
            onDismissRequest = {
                showBackupPasswordDialog = false
                pendingBackupUri = null
            },
            onSubmit = { password, options ->
                val uri = pendingBackupUri ?: return@CupertinoBackupOptionsDialog
                showBackupPasswordDialog = false
                scope.launch {
                    val stream = context.contentResolver.openOutputStream(uri)
                    if (stream != null) {
                        val result = BackupManager.createBackup(context, password, stream, options)
                        if (result.isSuccess) {
                            haptics.success()
                            Toast.makeText(context, "Encrypted backup saved successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            haptics.error()
                            Toast.makeText(context, "Backup failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    pendingBackupUri = null
                }
            },
            isDark = isDarkTheme
        )
    }

    // Encrypted Backup Restore Password & Selective Restore Dialog
    if (showRestorePasswordDialog && pendingRestoreUri != null) {
        CupertinoRestoreDialog(
            onDismissRequest = {
                showRestorePasswordDialog = false
                pendingRestoreUri = null
            },
            onInspectBackup = { password, onResult ->
                val uri = pendingRestoreUri ?: return@CupertinoRestoreDialog
                scope.launch {
                    val stream = context.contentResolver.openInputStream(uri)
                    if (stream != null) {
                        val result = BackupManager.inspectBackup(context, password, stream)
                        onResult(result)
                    } else {
                        onResult(Result.failure(Exception("Cannot open backup file")))
                    }
                }
            },
            onConfirmRestore = { password, options ->
                val uri = pendingRestoreUri ?: return@CupertinoRestoreDialog
                showRestorePasswordDialog = false
                closingCountdown = 3
                restoreCompleted = false
                scope.launch {
                    val stream = context.contentResolver.openInputStream(uri)
                    if (stream != null) {
                        val result = BackupManager.restoreBackup(context, password, stream, options)
                        if (result.isSuccess) {
                            haptics.success()
                            withContext(Dispatchers.IO) {
                                services = db.serviceDao().getAllServicesList()
                                allSessions = db.sessionDao().getAllSessionsList()
                            }
                            restoreCompleted = true
                        } else {
                            haptics.error()
                            closingCountdown = null
                            restoreCompleted = false
                            Toast.makeText(context, "Restore failed: Invalid password or corrupt file", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        closingCountdown = null
                        restoreCompleted = false
                    }
                    pendingRestoreUri = null
                }
            },
            isDark = isDarkTheme
        )
    }

    // App Restart Prompt Dialog after restoring web sessions/logins
    if (showRestartDialog && closingCountdown == null) {
        CupertinoAlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = "Restart Required",
            message = "Successfully restored $restoredSessionCount sessions and logins. To initialize your accounts and cookies, ColAI needs to restart now.",
            confirmTitle = "Restart Now",
            cancelTitle = "Later",
            onConfirm = {
                showRestartDialog = false
                AppRestartHelper.restartApp(context)
            },
            isDark = isDarkTheme
        )
    }

    // Closing the app in 3, 2, 1 Countdown Overlay Popup
    if (closingCountdown != null) {
        val count = closingCountdown ?: 1
        val cardBg = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFF9F9FB)
        val cardBorder = if (isDarkTheme) Color(0x33FFFFFF) else Color(0x18000000)
        val textColor = if (isDarkTheme) Color.White else Color.Black

        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBg)
                        .border(BorderStroke(0.75.dp, cardBorder), RoundedCornerShape(24.dp))
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CupertinoActiveBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (count > 0) "$count" else "...",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                ),
                                color = CupertinoActiveBlue
                            )
                        }

                        Text(
                            text = if (count > 0) "Closing the app in $count" else "Restarting...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.4).sp
                            ),
                            color = textColor,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (!restoreCompleted) "Restoring sessions and web logins in the background..." else "Finishing up...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = textColor.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean iOS Liquid Glass donation banner supporting independent development.
 */
@Composable
fun DonationBanner(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val isHighContrast = com.ujwal.colai.core.ui.theme.LocalContrastLevel.current == "high"

    val cardBg = if (isDark) {
        if (isHighContrast) Color(0xFF141416) else Color(0xFF1C1C1E)
    } else {
        if (isHighContrast) Color(0xFFF2F2F7) else Color(0xFFF9F9FB)
    }
    val borderColor = if (isDark) Color(0x33FF2D55) else Color(0x22FF2D55)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF2D55).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Support ColAI",
                        tint = Color(0xFFFF2D55),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Support ColAI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = if (isDark) Color.White else Color.Black
                )
            }

            Text(
                text = "ColAI is ad free and always will be. But we need your support to continue building independent, private AI tools.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = if (isDark) Color(0xCCFFFFFF) else Color(0xCC000000)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF2D55))
                    .clickable {
                        haptics.impactLight()
                        val donateIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://buymemomo.com/ujwal")
                        )
                        context.startActivity(donateIntent)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "Donate on buymemomo.com",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

