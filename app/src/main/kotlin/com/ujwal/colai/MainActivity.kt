package com.ujwal.colai

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import com.ujwal.colai.core.ui.components.CupertinoPinLockDialog
import com.ujwal.colai.core.ui.components.CupertinoResetWithRecoveryKeyDialog
import com.ujwal.colai.core.ui.components.CupertinoShareTargetSheet
import com.ujwal.colai.core.ui.components.SharedPayload
import com.ujwal.colai.core.ui.theme.ColAISprings
import com.ujwal.colai.core.ui.theme.ColAITheme
import com.ujwal.colai.feature.about.AboutScreen
import com.ujwal.colai.feature.home.AddServiceScreen
import com.ujwal.colai.feature.home.HomeScreen
import com.ujwal.colai.feature.home.HomeViewModel
import com.ujwal.colai.feature.notifications.NotificationSettingsScreen
import com.ujwal.colai.feature.onboarding.OnboardingScreen
import com.ujwal.colai.feature.onboarding.SetupScreen
import com.ujwal.colai.feature.sessions.SessionsManagementScreen
import com.ujwal.colai.feature.settings.SessionLocksScreen
import com.ujwal.colai.feature.settings.SettingsScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ujwal.colai.feature.webview.CupertinoOAuthPopupOverlay
import com.ujwal.colai.feature.webview.CupertinoSessionBottomBar
import com.ujwal.colai.feature.webview.CupertinoWebViewTopBar
import com.ujwal.colai.feature.webview.GeckoPromptDelegateRegistry
import com.ujwal.colai.feature.webview.GeckoViewContainer
import com.ujwal.colai.feature.webview.PendingSharedFilesBanner
import com.ujwal.colai.feature.webview.WebSessionViewModel
import com.ujwal.colai.util.DeepLinkResolver
import com.ujwal.colai.util.DeepLinkTarget

/**
 * Screen navigation destinations within ColAI.
 */
sealed interface AppScreen {
    data object Onboarding : AppScreen
    data object Setup : AppScreen
    data object Home : AppScreen
    data object AddService : AppScreen
    data object Settings : AppScreen
    data object SessionLocks : AppScreen
    data object SessionsManagement : AppScreen
    data object NotificationSettings : AppScreen
    data object About : AppScreen
    data class Web(val serviceId: String, val sessionId: String?) : AppScreen
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels { HomeViewModel.Factory(application) }
    private val webSessionViewModel: WebSessionViewModel by viewModels()

    private var pendingDeepLink by mutableStateOf<DeepLinkTarget?>(null)
    private var pendingSharePayload by mutableStateOf<SharedPayload?>(null)

    // Activity Result Launchers for GeckoView file attachment & permissions
    private var pendingFilePickerCallback: ((List<Uri>) -> Unit)? = null
    private val multipleFilesPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
        }
        pendingFilePickerCallback?.invoke(uris)
        pendingFilePickerCallback = null
    }

    private val singleFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
        }
        pendingFilePickerCallback?.invoke(if (uri != null) listOf(uri) else emptyList())
        pendingFilePickerCallback = null
    }

    private var pendingPermissionCallback: ((Boolean) -> Unit)? = null
    private val multiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results: Map<String, Boolean> ->
        val allGranted = results.values.all { it }
        pendingPermissionCallback?.invoke(allGranted)
        pendingPermissionCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Register GeckoView prompt & permission handlers to Activity Result Launchers
        GeckoPromptDelegateRegistry.register(
            filePicker = { mimeTypes, isMultiple, onResult ->
                pendingFilePickerCallback = onResult
                try {
                    val types = if (mimeTypes.isEmpty()) arrayOf("*/*") else mimeTypes
                    if (isMultiple) {
                        multipleFilesPickerLauncher.launch(types)
                    } else {
                        singleFilePickerLauncher.launch(types)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to launch file picker", e)
                    onResult(emptyList())
                }
            },
            permissionRequester = { permissions, onResult ->
                pendingPermissionCallback = onResult
                try {
                    multiplePermissionsLauncher.launch(permissions)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to launch permission requester", e)
                    onResult(false)
                }
            }
        )

        handleIncomingIntent(intent)

        val storage = EncryptedStorage.getInstance(applicationContext)
        val initialScreen = if (storage.isOnboardingCompleted()) {
            AppScreen.Home
        } else {
            AppScreen.Onboarding
        }

        val database = AppDatabase.getDatabase(applicationContext)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var currentThemeMode by remember { mutableStateOf(storage.getThemeMode()) }
            var currentContrastLevel by remember { mutableStateOf(storage.getContrastLevel()) }

            val isDark = when (currentThemeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            ColAITheme(darkTheme = isDark, contrastLevel = currentContrastLevel) {
                var currentScreen by remember { mutableStateOf<AppScreen>(initialScreen) }
                var pendingSessionToUnlock by remember { mutableStateOf<Pair<AIService, Session?>?>(null) }
                var pendingSharePayloadForUnlock by remember { mutableStateOf<SharedPayload?>(null) }
                var sessionToResetWithRecovery by remember { mutableStateOf<Pair<String, String>?>(null) }

                val allSessions by remember { database.sessionDao().getAllSessions() }.collectAsState(initial = emptyList())
                val homeUiState by homeViewModel.uiState.collectAsState()
                val allServices = homeUiState.services

                LaunchedEffect(pendingDeepLink) {
                    val target = pendingDeepLink ?: return@LaunchedEffect
                    when (target) {
                        is DeepLinkTarget.OpenService -> {
                            storage.setOnboardingCompleted(true)
                            val serviceId = target.serviceId
                            storage.setActiveServiceId(serviceId)

                            val targetService = database.serviceDao().getServiceByIdSync(serviceId)
                                ?: allServices.find { it.id == serviceId }
                            val targetSessionId = target.sessionId
                                ?: (if (serviceId == storage.getWidgetServiceId()) storage.getWidgetSessionId() else null)
                                ?: targetService?.widgetSessionId
                                ?: database.sessionDao().getDefaultSessionForService(serviceId)?.id
                                ?: database.sessionDao().getSessionsForServiceList(serviceId).firstOrNull()?.id

                            if (targetSessionId != null) {
                                storage.setActiveSessionId(targetSessionId)
                            }

                            if (targetSessionId != null && storage.isSessionPinLocked(targetSessionId)) {
                                if (targetService != null) {
                                    val session = database.sessionDao().getSessionByIdSync(targetSessionId)
                                    pendingSessionToUnlock = Pair(targetService, session)
                                }
                            } else {
                                webSessionViewModel.initialize(
                                    serviceId = serviceId,
                                    sessionId = targetSessionId,
                                    targetUrl = target.targetUrl,
                                    initialAction = target.initialAction
                                )
                                currentScreen = AppScreen.Web(serviceId, targetSessionId)
                            }
                        }
                        is DeepLinkTarget.Voice -> {
                            storage.setOnboardingCompleted(true)
                            val serviceId = target.serviceId ?: storage.getWidgetServiceId() ?: storage.getActiveServiceId() ?: "chatgpt"
                            storage.setActiveServiceId(serviceId)

                            val targetService = database.serviceDao().getServiceByIdSync(serviceId)
                                ?: allServices.find { it.id == serviceId }
                            val targetSessionId = target.sessionId
                                ?: (if (serviceId == storage.getWidgetServiceId()) storage.getWidgetSessionId() else null)
                                ?: targetService?.widgetSessionId
                                ?: database.sessionDao().getDefaultSessionForService(serviceId)?.id
                                ?: database.sessionDao().getSessionsForServiceList(serviceId).firstOrNull()?.id

                            if (targetSessionId != null) {
                                storage.setActiveSessionId(targetSessionId)
                            }

                            if (targetSessionId != null && storage.isSessionPinLocked(targetSessionId)) {
                                if (targetService != null) {
                                    val session = database.sessionDao().getSessionByIdSync(targetSessionId)
                                    pendingSessionToUnlock = Pair(targetService, session)
                                }
                            } else {
                                webSessionViewModel.initialize(
                                    serviceId = serviceId,
                                    sessionId = targetSessionId,
                                    initialAction = "mic"
                                )
                                currentScreen = AppScreen.Web(serviceId, targetSessionId)
                            }
                        }
                        is DeepLinkTarget.Search -> {
                            if (!target.query.isNullOrBlank()) {
                                homeViewModel.onSearchQueryChanged(target.query)
                            }
                            currentScreen = AppScreen.Home
                        }
                    }
                    pendingDeepLink = null
                }

                val webUiState by webSessionViewModel.uiState.collectAsState()

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn(animationSpec = ColAISprings.Fluid) togetherWith fadeOut(animationSpec = ColAISprings.Fluid) },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        is AppScreen.Onboarding -> {
                            OnboardingScreen(
                                onFinish = {
                                    currentScreen = AppScreen.Setup
                                }
                            )
                        }

                        is AppScreen.Setup -> {
                            SetupScreen(
                                onSetupComplete = {
                                    homeViewModel.refresh()
                                    currentScreen = AppScreen.Home
                                }
                            )
                        }

                        is AppScreen.Home -> {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onLaunchService = { service, session ->
                                    val targetSessionId = session?.id ?: service.widgetSessionId
                                    if (targetSessionId != null && storage.isSessionPinLocked(targetSessionId)) {
                                        pendingSessionToUnlock = Pair(service, session)
                                    } else {
                                        webSessionViewModel.initialize(
                                            serviceId = service.id,
                                            sessionId = session?.id
                                        )
                                        currentScreen = AppScreen.Web(service.id, session?.id)
                                    }
                                },
                                onOpenManageSessions = {
                                    currentScreen = AppScreen.SessionsManagement
                                },
                                onOpenAddService = {
                                    currentScreen = AppScreen.AddService
                                },
                                onOpenSettings = {
                                    currentScreen = AppScreen.Settings
                                },
                                onToggleTheme = {
                                    val effectiveDark = when (currentThemeMode) {
                                        "dark" -> true
                                        "light" -> false
                                        else -> systemDark
                                    }
                                    val next = if (effectiveDark) "light" else "dark"
                                    currentThemeMode = next
                                    storage.setThemeMode(next)
                                },
                                isDarkTheme = isDark
                            )
                        }

                        is AppScreen.AddService -> {
                            BackHandler { currentScreen = AppScreen.Home }
                            AddServiceScreen(
                                onNavigateBack = { currentScreen = AppScreen.Home },
                                onServiceCreated = { createdService ->
                                    homeViewModel.refresh()
                                    webSessionViewModel.initialize(
                                        serviceId = createdService.id,
                                        sessionId = createdService.widgetSessionId
                                    )
                                    currentScreen = AppScreen.Web(
                                        serviceId = createdService.id,
                                        sessionId = createdService.widgetSessionId
                                    )
                                },
                                isDarkTheme = isDark
                            )
                        }

                        is AppScreen.Settings -> {
                            BackHandler { currentScreen = AppScreen.Home }
                            SettingsScreen(
                                onNavigateBack = { currentScreen = AppScreen.Home },
                                onOpenSessionsManagement = {
                                    currentScreen = AppScreen.SessionsManagement
                                },
                                onOpenSessionLocks = {
                                    currentScreen = AppScreen.SessionLocks
                                },
                                onOpenNotificationsSettings = {
                                    currentScreen = AppScreen.NotificationSettings
                                },
                                onOpenAbout = {
                                    currentScreen = AppScreen.About
                                },
                                onThemeChanged = { newMode ->
                                    currentThemeMode = newMode
                                },
                                onContrastChanged = { newContrast ->
                                    currentContrastLevel = newContrast
                                },
                                isDarkTheme = isDark
                            )
                        }

                        is AppScreen.SessionLocks -> {
                            BackHandler { currentScreen = AppScreen.Settings }
                            SessionLocksScreen(
                                onNavigateBack = { currentScreen = AppScreen.Settings },
                                isDarkTheme = isDark
                            )
                        }

                        is AppScreen.SessionsManagement -> {
                            BackHandler { currentScreen = AppScreen.Settings }
                            SessionsManagementScreen(
                                onNavigateBack = { currentScreen = AppScreen.Settings },
                                isDarkTheme = isDark
                            )
                        }

                        is AppScreen.NotificationSettings -> {
                            BackHandler { currentScreen = AppScreen.Settings }
                            NotificationSettingsScreen(
                                onNavigateBack = { currentScreen = AppScreen.Settings },
                                isDarkTheme = isDark
                            )
                        }

                        is AppScreen.About -> {
                            BackHandler { currentScreen = AppScreen.Settings }
                            AboutScreen(
                                onNavigateBack = { currentScreen = AppScreen.Settings },
                                isDarkTheme = isDark
                            )
                        }

                        is AppScreen.Web -> {
                            BackHandler {
                                if (webSessionViewModel.uiState.value.canGoBack) {
                                    webSessionViewModel.goBack()
                                } else {
                                    currentScreen = AppScreen.Home
                                }
                            }

                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                topBar = {
                                    if (webUiState.popupGeckoSession == null) {
                                        val fallbackSurface = com.ujwal.colai.feature.webview.ServiceBrandColors.getBrandSurfaceColor(
                                            webUiState.currentService?.id ?: "",
                                            isDark
                                        )
                                        CupertinoWebViewTopBar(
                                            canGoBack = webUiState.canGoBack,
                                            isLoading = webUiState.isLoading,
                                            progress = webUiState.progress,
                                            isDark = isDark,
                                            activeBgColor = webUiState.activeThemeColor ?: fallbackSurface,
                                            activeTextColor = null,
                                            serviceName = webUiState.currentService?.name ?: "ColAI",
                                            onGoBack = webSessionViewModel::goBack,
                                            onBackToHome = { currentScreen = AppScreen.Home },
                                            onReload = {
                                                if (webUiState.isLoading) webSessionViewModel.stop()
                                                else webSessionViewModel.reload()
                                            }
                                        )
                                    }
                                },
                                bottomBar = {
                                    if (webUiState.popupGeckoSession == null) {
                                        val fallbackSurface = com.ujwal.colai.feature.webview.ServiceBrandColors.getBrandSurfaceColor(
                                            webUiState.currentService?.id ?: "",
                                            isDark
                                        )
                                        val brandAccent = webUiState.currentService?.let {
                                            com.ujwal.colai.feature.webview.ServiceBrandColors.getBrandColor(it.id)
                                        }
                                        CupertinoSessionBottomBar(
                                            service = webUiState.currentService,
                                            activeSession = webUiState.currentSession,
                                            sessions = webUiState.availableSessions,
                                            isDark = isDark,
                                            activeBgColor = webUiState.activeThemeColor ?: fallbackSurface,
                                            activePrimaryColor = brandAccent,
                                            activeTextColor = null,
                                            onSelectSession = { session ->
                                                if (storage.isSessionPinLocked(session.id) && session.id != webUiState.currentSession?.id) {
                                                    webUiState.currentService?.let { currentSvc ->
                                                        pendingSessionToUnlock = Pair(currentSvc, session)
                                                    }
                                                } else {
                                                    webSessionViewModel.switchSession(session)
                                                }
                                            },
                                            onAddSession = { name ->
                                                webSessionViewModel.createSession(name)
                                            },
                                            onDeleteSession = { session ->
                                                webSessionViewModel.deleteSession(session)
                                            },
                                            onClearSessionData = { session ->
                                                webSessionViewModel.clearSessionData(session)
                                            }
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                        .consumeWindowInsets(innerPadding)
                                        .imePadding()
                                ) {
                                    val activeDisplaySession = webUiState.popupGeckoSession ?: webUiState.geckoSession
                                    GeckoViewContainer(
                                        session = activeDisplaySession,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (webUiState.popupGeckoSession != null) {
                                        CupertinoOAuthPopupOverlay(
                                            isDark = isDark,
                                            onDismissRequest = {
                                                webSessionViewModel.dismissOAuthPopup(reloadParent = false)
                                            },
                                            modifier = Modifier.align(Alignment.TopCenter)
                                        )
                                    }

                                    if (webUiState.hasPendingSharedFiles) {
                                        PendingSharedFilesBanner(
                                            count = webUiState.pendingSharedFileCount,
                                            isDark = isDark,
                                            onDismiss = { webSessionViewModel.clearPendingSharedFiles() },
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // PIN Unlock Dialog
                pendingSessionToUnlock?.let { (service, session) ->
                    val targetSession = session ?: webUiState.availableSessions.find { it.isDefault } ?: webUiState.availableSessions.firstOrNull()
                    val sName = targetSession?.accountName ?: service.name
                    val sId = targetSession?.id ?: service.widgetSessionId
                    if (sId != null) {
                        CupertinoPinLockDialog(
                            sessionName = sName,
                            onDismissRequest = { pendingSessionToUnlock = null },
                            onVerifyPin = { pin -> storage.verifySessionPin(sId, pin) },
                            onForgotPin = if (storage.hasMasterRecoveryKey()) {
                                { sessionToResetWithRecovery = Pair(sId, sName) }
                            } else null,
                            onSuccess = {
                                val sharePayload = pendingSharePayloadForUnlock
                                pendingSharePayloadForUnlock = null
                                pendingSessionToUnlock = null
                                if (currentScreen is AppScreen.Web && targetSession != null && sharePayload == null) {
                                    webSessionViewModel.switchSession(targetSession)
                                } else {
                                    webSessionViewModel.initialize(
                                        serviceId = service.id,
                                        sessionId = sId,
                                        sharedText = sharePayload?.text,
                                        sharedFiles = sharePayload?.uris
                                    )
                                    currentScreen = AppScreen.Web(service.id, sId)
                                }
                            },
                            isDark = isDark
                        )
                    } else {
                        pendingSessionToUnlock = null
                    }
                }

                // Master Recovery Key Reset Dialog (Forgot PIN)
                sessionToResetWithRecovery?.let { (sId, sName) ->
                    CupertinoResetWithRecoveryKeyDialog(
                        sessionName = sName,
                        onDismissRequest = { sessionToResetWithRecovery = null },
                        onVerifyRecoveryKey = { key -> storage.verifyMasterRecoveryKey(key) },
                        onRecoverySuccess = {
                            storage.removeSessionPin(sId)
                            sessionToResetWithRecovery = null
                            pendingSessionToUnlock = null
                        },
                        isDark = isDark
                    )
                }

                // Incoming Share Intent Chooser Sheet
                pendingSharePayload?.let { payload ->
                    CupertinoShareTargetSheet(
                        payload = payload,
                        services = allServices,
                        sessions = allSessions,
                        onDismissRequest = { pendingSharePayload = null },
                        onTargetSelected = { selectedService, selectedSession ->
                            storage.setOnboardingCompleted(true)
                            storage.setActiveServiceId(selectedService.id)
                            storage.setActiveSessionId(selectedSession.id)
                            if (storage.isSessionPinLocked(selectedSession.id)) {
                                pendingSharePayloadForUnlock = payload
                                pendingSessionToUnlock = Pair(selectedService, selectedSession)
                            } else {
                                webSessionViewModel.initialize(
                                    serviceId = selectedService.id,
                                    sessionId = selectedSession.id,
                                    sharedText = payload.text,
                                    sharedFiles = payload.uris
                                )
                                currentScreen = AppScreen.Web(selectedService.id, selectedSession.id)
                            }
                            pendingSharePayload = null
                        },
                        isDark = isDark
                    )
                }


            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        GeckoPromptDelegateRegistry.unregister()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val uris = when (intent.action) {
                Intent.ACTION_SEND -> {
                    val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    if (uri != null) listOf(uri) else emptyList()
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    val list: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                    }
                    list ?: emptyList()
                }
                else -> emptyList()
            }

            val safeUris = copyIncomingUrisToCache(uris)
            if (safeUris.isNotEmpty()) {
                try {
                    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    val firstUri = safeUris.first()
                    val contentUri = if (firstUri.scheme == "file") {
                        val file = File(firstUri.path ?: "")
                        FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                    } else firstUri

                    val clip = android.content.ClipData.newUri(contentResolver, "Shared Image", contentUri)
                    grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipboard?.setPrimaryClip(clip)
                } catch (e: Exception) {
                    Log.w("MainActivity", "Could not copy shared image to clipboard", e)
                }
            }
            if (!text.isNullOrBlank() || safeUris.isNotEmpty()) {
                pendingSharePayload = SharedPayload(text = text, uris = safeUris)
                return
            }
        }

        val target = DeepLinkResolver.resolveIntent(intent)
        if (target != null) {
            pendingDeepLink = target
        }
    }

    private fun copyIncomingUrisToCache(uris: List<Uri>): List<Uri> {
        if (uris.isEmpty()) return emptyList()
        val safeUris = mutableListOf<Uri>()
        val shareDir = File(cacheDir, "shared").apply { mkdirs() }

        uris.forEachIndexed { index, uri ->
            try {
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    val appDataDir = applicationInfo.dataDir
                    // Block internal app directory access from external file URIs
                    if (file.canonicalPath.startsWith(appDataDir)) {
                        Log.w("MainActivity", "Blocked attempt to access app private storage: ${file.canonicalPath}")
                        return@forEachIndexed
                    }
                    if (file.exists() && file.canRead()) {
                        safeUris.add(uri)
                        return@forEachIndexed
                    }
                }

                var fileName = "shared_${System.currentTimeMillis()}_$index.jpg"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (colIndex != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(colIndex)
                        if (!name.isNullOrBlank()) fileName = name
                    }
                }

                val sanitizedName = File(fileName).name.replace("[/\\\\?%*:|\"<>]".toRegex(), "_").trim('.')
                val safeName = sanitizedName.ifBlank { "shared_${System.currentTimeMillis()}_$index.jpg" }
                val destFile = File(shareDir, "${System.currentTimeMillis()}_$safeName")
                contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (destFile.exists() && destFile.length() > 0) {
                    safeUris.add(Uri.fromFile(destFile))
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to cache incoming share URI: $uri", e)
            }
        }
        return safeUris
    }
}
