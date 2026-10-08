package com.ujwal.colai.feature.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.ui.components.CupertinoActiveBlue
import com.ujwal.colai.core.ui.components.CupertinoDarkBg
import com.ujwal.colai.core.ui.components.CupertinoFormRow
import com.ujwal.colai.core.ui.components.CupertinoFormSection
import com.ujwal.colai.core.ui.components.CupertinoLightBg
import com.ujwal.colai.core.ui.components.CupertinoNavigationBar
import com.ujwal.colai.core.ui.components.CupertinoSwitch
import com.ujwal.colai.util.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * EXACT Cupertino NotificationSettingsScreen:
 * - APP PERMISSION section with system permission status and request action
 * - Per-service notification toggles
 */
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    val servicesFlow = remember { db.serviceDao().getAllServices() }
    val services by servicesFlow.collectAsState(initial = emptyList())

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val scaffoldBg = if (isDarkTheme) CupertinoDarkBg else CupertinoLightBg

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scaffoldBg,
        topBar = {
            CupertinoNavigationBar(
                title = "Notifications",
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
            // APP PERMISSION Section
            CupertinoFormSection(
                header = "APP PERMISSION",
                footer = if (hasNotificationPermission) {
                    "The app has permission to send notifications."
                } else {
                    "The app needs system permission to send notifications."
                },
                isDark = isDarkTheme
            ) {
                CupertinoFormRow(
                    prefix = {
                        Text(
                            text = "Allow Notifications",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    showDivider = false,
                    isDark = isDarkTheme
                ) {
                    TextButton(
                        onClick = {
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    ) {
                        Text(
                            text = if (hasNotificationPermission) "Allowed" else "Request Access",
                            color = CupertinoActiveBlue,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
                        )
                    }
                }
            }

            // Per-Service Notification Switches
            services.forEach { service ->
                CupertinoFormSection(
                    header = service.name.uppercase(),
                    isDark = isDarkTheme
                ) {
                    CupertinoFormRow(
                        prefix = {
                            Text(
                                text = "Receive Notifications from ${service.name}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = if (isDarkTheme) Color.White else Color.Black
                            )
                        },
                        showDivider = false,
                        isDark = isDarkTheme
                    ) {
                        CupertinoSwitch(
                            checked = service.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch(Dispatchers.IO) {
                                    db.serviceDao().updateService(service.copy(notificationsEnabled = enabled))
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
