package com.ujwal.colai.feature.webview

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.data.DefaultServicesRepository
import com.ujwal.colai.core.engine.GeckoSessionPool
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebResponse
import com.ujwal.colai.core.engine.GeckoRuntimeManager
import com.ujwal.colai.core.engine.UserAgentGenerator

private const val TAG = "WebSessionViewModel"

/**
 * UI State for the GeckoView web session screen.
 */
data class WebUiState(
    val currentService: AIService? = null,
    val currentSession: Session? = null,
    val availableSessions: List<Session> = emptyList(),
    val allServices: List<AIService> = emptyList(),
    val currentUrl: String = "",
    val currentTitle: String = "",
    val progress: Float = 0f,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = true,
    val activeThemeColor: Color? = null,
    val rawThemeColor: String? = null,
    val geckoSession: GeckoSession? = null,
    val popupGeckoSession: GeckoSession? = null,
    val hasPendingSharedFiles: Boolean = false,
    val pendingSharedFileCount: Int = 0
)

/**
 * Canonical default accent and surface colors for supported AI providers.
 */
object ServiceBrandColors {
    val CHATGPT = Color(0xFF10A37F)
    val CLAUDE = Color(0xFFD97706)
    val DEEPSEEK = Color(0xFF4D6BFE)
    val GROK = Color(0xFF2A2E39)
    val GEMINI = Color(0xFF4285F4)
    val PERPLEXITY = Color(0xFF20808D)
    val DEFAULT_SURFACE = Color(0xFF0F1117)

    fun getBrandColor(serviceId: String): Color {
        return when (serviceId.lowercase()) {
            DefaultServicesRepository.ID_CHATGPT -> CHATGPT
            DefaultServicesRepository.ID_CLAUDE -> CLAUDE
            DefaultServicesRepository.ID_DEEPSEEK -> DEEPSEEK
            DefaultServicesRepository.ID_GROK -> GROK
            DefaultServicesRepository.ID_GEMINI -> GEMINI
            DefaultServicesRepository.ID_PERPLEXITY -> PERPLEXITY
            else -> DEFAULT_SURFACE
        }
    }

    fun getBrandSurfaceColor(serviceId: String, isDark: Boolean): Color {
        return if (isDark) {
            when (serviceId.lowercase()) {
                DefaultServicesRepository.ID_CHATGPT -> Color(0xFF212121)
                DefaultServicesRepository.ID_CLAUDE -> Color(0xFF262522)
                DefaultServicesRepository.ID_GEMINI -> Color(0xFF131314)
                DefaultServicesRepository.ID_DEEPSEEK -> Color(0xFF18191C)
                DefaultServicesRepository.ID_GROK -> Color(0xFF000000)
                DefaultServicesRepository.ID_PERPLEXITY -> Color(0xFF191A1A)
                else -> Color(0xFF121214)
            }
        } else {
            when (serviceId.lowercase()) {
                DefaultServicesRepository.ID_CLAUDE -> Color(0xFFFAF9F5)
                DefaultServicesRepository.ID_PERPLEXITY -> Color(0xFFF3F3EE)
                else -> Color(0xFFFFFFFF)
            }
        }
    }
}

/**
 * ViewModel managing active GeckoSession container, browser navigation delegates,
 * tab switching, and 120Hz real-time web theme color adaptation.
 */
class WebSessionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val serviceDao = database.serviceDao()
    private val sessionDao = database.sessionDao()
    private val sessionPool = GeckoSessionPool.getInstance()

    val contentBlocker = GeckoContentBlocker()

    private val _uiState = MutableStateFlow(WebUiState())
    val uiState: StateFlow<WebUiState> = _uiState.asStateFlow()

    private var activeGeckoSession: GeckoSession? = null
    private var oauthPopupSession: GeckoSession? = null

    // Navigation delegate with off-service link interception and OAuth popup protection
    private val navigationDelegate = object : GeckoSession.NavigationDelegate {

        override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
            _uiState.update { it.copy(canGoBack = canGoBack) }
        }

        override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
            _uiState.update { it.copy(canGoForward = canGoForward) }
        }

        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
            hasUserGesture: Boolean
        ) {
            if (!url.isNullOrBlank()) {
                _uiState.update { it.copy(currentUrl = url) }
            }
        }

        override fun onLoadRequest(
            session: GeckoSession,
            request: GeckoSession.NavigationDelegate.LoadRequest
        ): GeckoResult<AllowOrDeny> {
            val uri = request.uri
            val service = _uiState.value.currentService
            if (service != null && shouldOpenInExternalBrowser(service, uri, request)) {
                Log.i(TAG, "Intercepted off-service link, opening in default browser: $uri")
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    getApplication<Application>().startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch external browser for link $uri", e)
                }
                return GeckoResult.fromValue(AllowOrDeny.DENY)
            }
            return GeckoResult.fromValue(AllowOrDeny.ALLOW)
        }

        override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
            val service = _uiState.value.currentService
            Log.i(TAG, "onNewSession requested with uri: $uri")

            val parsedUri = try { Uri.parse(uri) } catch (_: Exception) { null }
            val host = parsedUri?.host?.lowercase().orEmpty()

            // If this is an OAuth / SSO flow, create and present a real child popup session
            // sharing the SAME contextId so cookies and session tokens flow seamlessly.
            val isAuth = (parsedUri != null && isAuthOrSsoUrl(parsedUri, host)) ||
                         uri.isBlank() || uri == "about:blank" ||
                         host.contains("accounts.google") || host.contains("appleid.apple") ||
                         host.contains("login.microsoftonline") || host.contains("x.com") ||
                         host.contains("twitter.com") || host.contains("auth.anthropic")

            if (isAuth) {
                val currentContextId = resolveEffectiveContextId(_uiState.value.currentSession)
                if (currentContextId != null) {
                    Log.i(TAG, "Creating child OAuth popup GeckoSession for container: $currentContextId, uri: $uri")
                    val popupSettings = GeckoSessionSettings.Builder()
                        .contextId(currentContextId)
                        .usePrivateMode(false)
                        .useTrackingProtection(false)
                        .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
                        .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
                        .userAgentOverride(UserAgentGenerator.GOOGLE_AUTH_USER_AGENT)
                        .allowJavascript(true)
                        .build()

                    val popup = GeckoSession(popupSettings)
                    var isDismissed = false
                    var lastPopupUrl = uri

                    val safeDismissPopup: (Boolean) -> Unit = { reloadParent ->
                        if (!isDismissed) {
                            isDismissed = true
                            dismissOAuthPopup(reloadParent = reloadParent)
                        }
                    }

                    popup.contentDelegate = object : GeckoSession.ContentDelegate {
                        override fun onCloseRequest(targetSession: GeckoSession) {
                            Log.i(TAG, "OAuth popup onCloseRequest received (window.close). Dismissing popup.")
                            safeDismissPopup(false)

                            // Parent window verification:
                            // If Claude or another SPA sent postMessage to window.opener, it will navigate itself.
                            // If the parent window is still lingering on /login or /auth after 1s,
                            // refresh or reload the service URL so newly established cookies take effect.
                            viewModelScope.launch(Dispatchers.Main) {
                                kotlinx.coroutines.delay(1000)
                                val currentParentUrl = _uiState.value.currentUrl.lowercase()
                                val svc = _uiState.value.currentService
                                if (currentParentUrl.contains("/login") || currentParentUrl.contains("/signin") || currentParentUrl.contains("/auth")) {
                                    Log.i(TAG, "Parent session still on login page ($currentParentUrl), refreshing to apply cookies")
                                    if (svc != null) {
                                        activeGeckoSession?.loadUri(svc.url)
                                    } else {
                                        activeGeckoSession?.reload()
                                    }
                                }
                            }
                        }
                    }

                    popup.navigationDelegate = object : GeckoSession.NavigationDelegate {
                        override fun onLocationChange(
                            targetSession: GeckoSession,
                            newUrl: String?,
                            perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                            hasUserGesture: Boolean
                        ) {
                            Log.d(TAG, "OAuth popup onLocationChange: $newUrl")
                            if (!newUrl.isNullOrBlank()) {
                                lastPopupUrl = newUrl
                            }
                        }

                        override fun onLoadRequest(
                            targetSession: GeckoSession,
                            request: GeckoSession.NavigationDelegate.LoadRequest
                        ): GeckoResult<AllowOrDeny> {
                            return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                        }

                        override fun onNewSession(
                            targetSession: GeckoSession,
                            subUri: String
                        ): GeckoResult<GeckoSession>? {
                            Log.d(TAG, "OAuth popup requested sub-session: $subUri")
                            targetSession.loadUri(subUri)
                            return null
                        }
                    }

                    popup.progressDelegate = object : GeckoSession.ProgressDelegate {
                        override fun onPageStop(targetSession: GeckoSession, success: Boolean) {
                            if (!success) return
                            val parsed = try { Uri.parse(lastPopupUrl) } catch (_: Exception) { null }
                            val targetHost = parsed?.host?.lowercase().orEmpty()
                            val path = parsed?.path?.lowercase().orEmpty()
                            val query = parsed?.query?.lowercase().orEmpty()
                            val serviceHost = try { Uri.parse(service?.url.orEmpty()).host?.lowercase().orEmpty() } catch (_: Exception) { "" }

                            // If popup completed navigation to the authenticated service root/chat page
                            // (e.g. https://claude.ai/ or https://claude.ai/new) and is NOT an intermediate auth/callback step:
                            if (serviceHost.isNotBlank() && (targetHost == serviceHost || targetHost.endsWith(".$serviceHost"))) {
                                val isAuthPath = path.contains("login") || path.contains("auth") ||
                                        path.contains("signin") || path.contains("oauth") ||
                                        path.contains("callback")
                                val hasAuthParams = query.contains("code=") || query.contains("state=")

                                if (!isAuthPath && !hasAuthParams && (path == "/" || path.isBlank() || path.contains("chat") || path.contains("new"))) {
                                    Log.i(TAG, "OAuth popup completed login destination ($lastPopupUrl). Syncing to main view.")
                                    safeDismissPopup(false)
                                    service?.url?.let { activeGeckoSession?.loadUri(it) }
                                }
                            }
                        }
                    }

                    // Attach prompt delegate to popup so credentials & permissions work
                    try {
                        val delegate = GeckoPromptDelegateRegistry.createDelegate(getApplication())
                        popup.promptDelegate = delegate
                        popup.permissionDelegate = delegate
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not attach prompt delegate to popup", e)
                    }

                    oauthPopupSession = popup
                    _uiState.update { it.copy(popupGeckoSession = popup) }
                    return GeckoResult.fromValue(popup)
                }
            }

            // Non-auth external links: open in external browser
            if (uri.isNotBlank() && uri != "about:blank") {
                if (service != null && shouldOpenInExternalBrowser(service, uri)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        getApplication<Application>().startActivity(intent)
                    } catch (_: Exception) {}
                    return null
                }
                session.loadUri(uri)
            }
            return null
        }
    }

    // Content delegate with window.close() restoration to prevent white screens after Google/SSO login
    private val contentDelegate = object : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            _uiState.update { it.copy(currentTitle = title.orEmpty()) }
        }

        override fun onWebAppManifest(session: GeckoSession, manifest: JSONObject) {
            val themeColor = manifest.optString("theme_color", "")
            if (themeColor.isNotBlank()) {
                Log.d(TAG, "Web manifest theme color: $themeColor")
                updateThemeColorFromString(themeColor)
            }
        }

        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            Log.i(TAG, "GeckoSession onExternalResponse triggered: ${response.uri}")
            GeckoDownloadHandler.handleExternalResponse(getApplication(), response)
        }

        override fun onCloseRequest(session: GeckoSession) {
            Log.i(TAG, "GeckoSession onCloseRequest received (OAuth popup closed). Restoring main service UI.")
            val service = _uiState.value.currentService
            if (service != null) {
                session.loadUri(service.url)
            }
        }
    }

    // History delegate for GeckoSession state persistence across app process death
    private val historyDelegate = object : GeckoSession.HistoryDelegate {
        override fun onHistoryStateChange(
            session: GeckoSession,
            historyList: GeckoSession.HistoryDelegate.HistoryList
        ) {
            if (historyList is GeckoSession.SessionState) {
                val currentSessionId = _uiState.value.currentSession?.id ?: return
                val stateStr = historyList.toString()
                EncryptedStorage.getInstance(getApplication()).saveSessionState(currentSessionId, stateStr)
            }
        }
    }

    private var pendingSharedText: String? = null
    private var pendingSharedFiles: List<Uri>? = null
    private var pendingInitialAction: String? = null

    // Progress delegate
    private val progressDelegate = object : GeckoSession.ProgressDelegate {
        override fun onPageStart(session: GeckoSession, url: String) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    progress = 0.1f,
                    currentUrl = url
                )
            }
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    progress = 1.0f
                )
            }
            // Hide install app / get the app banners on page completion
            GeckoContentBlocker.injectAntiAppBanner(session)
            triggerPendingActions(session)
        }

        override fun onProgressChange(session: GeckoSession, progress: Int) {
            _uiState.update {
                it.copy(
                    progress = (progress / 100f).coerceIn(0f, 1f),
                    isLoading = progress < 100
                )
            }
        }

        override fun onSecurityChange(
            session: GeckoSession,
            securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
        ) {
            _uiState.update {
                it.copy(isSecure = securityInfo.isSecure)
            }
        }
    }

    private fun triggerPendingActions(session: GeckoSession) {
        pendingSharedText?.let { text ->
            injectSharedTextInternal(session, text)
            pendingSharedText = null
        }
        pendingSharedFiles?.let { uris ->
            injectSharedFilesInternal(session, uris)
            pendingSharedFiles = null
        }
        pendingInitialAction?.let { action ->
            executeInitialAction(session, action)
            pendingInitialAction = null
        }
    }

    init {
        loadServicesAndSessions()
        GeckoPromptDelegateRegistry.onFilesConsumedListener = {
            _uiState.update { it.copy(hasPendingSharedFiles = false, pendingSharedFileCount = 0) }
        }
    }

    /**
     * Dismisses active OAuth popup session and optionally reloads the parent session.
     */
    fun dismissOAuthPopup(reloadParent: Boolean = false) {
        viewModelScope.launch(Dispatchers.Main) {
            val popup = oauthPopupSession
            if (popup != null) {
                try {
                    popup.navigationDelegate = null
                    popup.contentDelegate = null
                    popup.progressDelegate = null
                    if (popup.isOpen) {
                        popup.close()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing OAuth popup session", e)
                }
                oauthPopupSession = null
                _uiState.update { it.copy(popupGeckoSession = null) }
            }
            if (reloadParent) {
                kotlinx.coroutines.delay(250)
                activeGeckoSession?.reload()
            }
        }
    }

    private fun resolveEffectiveContextId(session: Session?): String? {
        if (session == null) return null
        val storage = EncryptedStorage.getInstance(getApplication())
        return if (session.isDefault && storage.isSharedPersonalCookiesEnabled()) {
            "shared_personal_container"
        } else {
            session.id
        }
    }

    /**
     * Initializes services and selects either the provided or default service.
     */
    fun initialize(
        serviceId: String? = null,
        sessionId: String? = null,
        targetUrl: String? = null,
        initialAction: String? = null,
        sharedText: String? = null,
        sharedFiles: List<Uri>? = null
    ) {
        if (initialAction != null) pendingInitialAction = initialAction
        if (!sharedText.isNullOrBlank()) pendingSharedText = sharedText
        if (!sharedFiles.isNullOrEmpty()) {
            pendingSharedFiles = sharedFiles
            GeckoPromptDelegateRegistry.setPendingUploadFiles(sharedFiles)
            _uiState.update { it.copy(hasPendingSharedFiles = true, pendingSharedFileCount = sharedFiles.size) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            var services = serviceDao.getAllServicesList()
            if (services.isEmpty()) {
                DefaultServicesRepository.create(getApplication()).seedIfEmpty()
                services = serviceDao.getAllServicesList()
            }
            if (services.isEmpty()) return@launch

            val targetService = if (serviceId != null) {
                services.find { it.id == serviceId } ?: services.first()
            } else {
                services.first()
            }

            var sessions = sessionDao.getSessionsForServiceList(targetService.id)
            if (sessions.isEmpty()) {
                val newSession = Session(
                    id = java.util.UUID.randomUUID().toString(),
                    serviceId = targetService.id,
                    accountName = "Personal",
                    isDefault = true,
                    createdAt = System.currentTimeMillis()
                )
                sessionDao.insertSession(newSession)
                sessions = listOf(newSession)
            }

            val currentSession = _uiState.value.currentSession
            val targetSession = if (sessionId != null) {
                sessions.find { it.id == sessionId } ?: sessions.firstOrNull()
            } else if (targetService.widgetSessionId != null) {
                sessions.find { it.id == targetService.widgetSessionId } ?: sessions.find { it.isDefault } ?: sessions.firstOrNull()
            } else if (currentSession != null && currentSession.serviceId == targetService.id) {
                currentSession
            } else {
                sessions.find { it.isDefault } ?: sessions.firstOrNull()
            }

            _uiState.update {
                it.copy(
                    allServices = services,
                    currentService = targetService,
                    availableSessions = sessions,
                    currentSession = targetSession,
                    activeThemeColor = null
                )
            }

            if (targetSession != null) {
                activateSession(targetService, targetSession, overrideUrl = targetUrl)
            }
        }
    }

    private fun loadServicesAndSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            serviceDao.getAllServices().collect { services ->
                _uiState.update { it.copy(allServices = services) }
            }
        }
    }

    /**
     * Activates the given [session] in [GeckoSessionPool] and attaches delegates.
     */
    fun activateSession(service: AIService, session: Session, overrideUrl: String? = null) {
        viewModelScope.launch(Dispatchers.Main) {
            val storage = EncryptedStorage.getInstance(getApplication())

            // Detach delegates from prior session
            activeGeckoSession?.let {
                it.navigationDelegate = null
                it.contentDelegate = null
                it.progressDelegate = null
                it.historyDelegate = null
                contentBlocker.detach(it)
            }

            val effectiveContextId = resolveEffectiveContextId(session) ?: session.id
            val isExistingSession = sessionPool.hasSession(session.id)
            val initialUrl = overrideUrl ?: service.url
            val geckoSession = sessionPool.getOrCreateSession(
                sessionId = session.id,
                contextId = effectiveContextId,
                targetUrl = initialUrl
            )

            // Per-account content blocking (Item 6)
            // Keep tracking protection off to prevent breaking Claude telemetry/statsig and Google Login
            geckoSession.settings.useTrackingProtection = false

            // Attach delegates
            geckoSession.navigationDelegate = navigationDelegate
            geckoSession.contentDelegate = contentDelegate
            geckoSession.progressDelegate = progressDelegate
            geckoSession.historyDelegate = historyDelegate
            contentBlocker.attach(geckoSession)

            activeGeckoSession = geckoSession
            sessionPool.setActiveSession(session.id)

            val sessions = sessionDao.getSessionsForServiceList(service.id)

            _uiState.update {
                it.copy(
                    currentService = service,
                    currentSession = session,
                    availableSessions = sessions,
                    currentUrl = if (overrideUrl != null) overrideUrl else if (isExistingSession && it.currentUrl.isNotBlank() && it.currentUrl != "about:blank") it.currentUrl else initialUrl,
                    activeThemeColor = it.activeThemeColor,
                    geckoSession = geckoSession
                )
            }

            // Update last accessed timestamp
            sessionDao.updateLastAccessed(session.id, System.currentTimeMillis())

            // Restore saved session state if this was a fresh load and no deep link override was provided (Item 5)
            if (overrideUrl != null) {
                geckoSession.loadUri(overrideUrl)
            } else if (!isExistingSession) {
                val savedState = storage.getSessionState(session.id)
                if (savedState != null) {
                    try {
                        val state = GeckoSession.SessionState.fromString(savedState)
                        if (state != null) {
                            geckoSession.restoreState(state)
                            Log.i(TAG, "Restored GeckoSession state for container ${session.id}")
                        } else {
                            geckoSession.loadUri(initialUrl)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to restore session state, loading initial URL", e)
                        geckoSession.loadUri(initialUrl)
                    }
                } else {
                    geckoSession.loadUri(initialUrl)
                }
            } else {
                // Existing/preloaded session is already ready in GeckoSessionPool
                val curr = _uiState.value.currentUrl
                if (curr.isBlank() || curr == "about:blank") {
                    geckoSession.loadUri(initialUrl)
                }
                triggerPendingActions(geckoSession)
            }
        }
    }

    /**
     * Updates active theme color from a raw web color string.
     */
    fun updateThemeColorFromString(rawColor: String?) {
        val parsed = ThemeColorAdapter.parseWebColor(rawColor)
        _uiState.update {
            it.copy(
                rawThemeColor = rawColor,
                activeThemeColor = parsed ?: it.activeThemeColor
            )
        }
    }

    /**
     * Updates active theme color directly.
     */
    fun updateThemeColor(color: Color?) {
        _uiState.update {
            it.copy(activeThemeColor = color)
        }
    }

    /**
     * Switches to another session within the active service.
     */
    fun switchSession(session: Session) {
        val currentService = _uiState.value.currentService ?: return
        activateSession(currentService, session)
    }

    /**
     * Switches to a different AI service, activating its default session.
     */
    fun switchService(service: AIService) {
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = sessionDao.getSessionsForServiceList(service.id)
            val defaultSession = sessions.find { it.isDefault } ?: sessions.firstOrNull()
            if (defaultSession != null) {
                activateSession(service, defaultSession)
            }
        }
    }

    /**
     * Retrieves the currently active [GeckoSession] for the container view.
     */
    fun getActiveGeckoSession(): GeckoSession? = activeGeckoSession

    fun goBack() {
        activeGeckoSession?.goBack()
    }

    fun goForward() {
        activeGeckoSession?.goForward()
    }

    fun reload() {
        activeGeckoSession?.reload()
    }

    fun loadUrl(url: String) {
        activeGeckoSession?.loadUri(url)
        _uiState.update { it.copy(currentUrl = url) }
    }

    fun stop() {
        activeGeckoSession?.stop()
    }

    /**
     * Injects shared text from other apps straight into the chat prompt (Item 4).
     */
    fun injectSharedText(text: String?) {
        if (text.isNullOrBlank()) return
        pendingSharedText = text
        val session = activeGeckoSession
        if (session != null && !_uiState.value.isLoading) {
            injectSharedTextInternal(session, text)
            pendingSharedText = null
        }
    }

    private fun injectSharedTextInternal(session: GeckoSession, text: String) {
        try {
            val quoted = JSONObject.quote(text)
            val js = """
                (function() {
                    let attempts = 0;
                    const interval = setInterval(() => {
                        attempts++;
                        try {
                            const textToInsert = $quoted;
                            const input = document.querySelector(
                                '#prompt-textarea, ' +
                                'textarea[data-testid="prompt-textarea"], ' +
                                'div[contenteditable="true"].ProseMirror, ' +
                                'div[contenteditable="true"], ' +
                                '.ql-editor, ' +
                                '[role="textbox"], ' +
                                'textarea, ' +
                                'input[type="text"]'
                            );
                            if (input) {
                                clearInterval(interval);
                                input.focus();
                                if (input.tagName.toLowerCase() === 'textarea' || input.tagName.toLowerCase() === 'input') {
                                    input.value = (input.value ? input.value + '\n' : '') + textToInsert;
                                    input.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
                                    input.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
                                } else {
                                    input.textContent = (input.textContent ? input.textContent + '\n' : '') + textToInsert;
                                    input.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
                                }
                            } else if (attempts >= 35) {
                                clearInterval(interval);
                            }
                        } catch (e) {
                            console.error('ColAI: Failed to inject shared text', e);
                        }
                    }, 400);
                })();
            """.trimIndent()
            session.loadUri("javascript:$js")
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting shared text into GeckoSession", e)
        }
    }

    /**
     * Clears any pending shared files and dismisses the attachment banner.
     */
    fun clearPendingSharedFiles() {
        pendingSharedFiles = null
        GeckoPromptDelegateRegistry.consumePendingUploadFiles()
        _uiState.update { it.copy(hasPendingSharedFiles = false, pendingSharedFileCount = 0) }
    }

    /**
     * Injects shared images/files directly into the chat prompt or file input.
     */
    fun injectSharedFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        pendingSharedFiles = uris
        GeckoPromptDelegateRegistry.setPendingUploadFiles(uris)
        _uiState.update { it.copy(hasPendingSharedFiles = true, pendingSharedFileCount = uris.size) }
        val session = activeGeckoSession
        if (session != null && !_uiState.value.isLoading) {
            injectSharedFilesInternal(session, uris)
            pendingSharedFiles = null
        }
    }

    private fun injectSharedFilesInternal(session: GeckoSession, uris: List<Uri>) {
        if (uris.isEmpty()) return

        // Register files so that GeckoPromptDelegate auto-confirms when the file prompt opens
        GeckoPromptDelegateRegistry.setPendingUploadFiles(uris)
        _uiState.update { it.copy(hasPendingSharedFiles = true, pendingSharedFileCount = uris.size) }

        // Attempt triggering the provider's file picker button via DOM
        val js = """
            (function() {
                var attempts = 0;
                var interval = setInterval(function() {
                    attempts++;
                    try {
                        var btn = document.querySelector(
                            'button[data-testid="upload-button"], ' +
                            'button[data-testid="attach-button"], ' +
                            'button[data-testid*="attach" i], ' +
                            'button[data-testid*="file-upload" i], ' +
                            'button[aria-label*="attach" i], ' +
                            'button[aria-label*="upload" i], ' +
                            'button[aria-label*="add file" i], ' +
                            'button[aria-label*="add image" i], ' +
                            'button[aria-label*="image" i], ' +
                            'button[aria-label*="Attach" i], ' +
                            'label[for*="file" i], ' +
                            'button[class*="attach" i], ' +
                            'button[class*="upload" i]'
                        );
                        if (btn) {
                            clearInterval(interval);
                            btn.click();
                        } else if (attempts >= 25) {
                            clearInterval(interval);
                        }
                    } catch(e) {
                        clearInterval(interval);
                    }
                }, 400);
            })();
        """.trimIndent()

        try {
            if (session.isOpen) {
                session.loadUri("javascript:$js")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting attach click into GeckoSession", e)
        }
    }

    /**
     * Sets an initial quick action ("mic" or "input") to trigger as soon as the session mounts.
     */
    fun setInitialAction(action: String?) {
        pendingInitialAction = action
        val session = activeGeckoSession
        if (action != null && session != null && !_uiState.value.isLoading) {
            executeInitialAction(session, action)
            pendingInitialAction = null
        }
    }

    private fun executeInitialAction(session: GeckoSession, action: String) {
        val js = when (action) {
            "mic" -> """
                (function() {
                    let retries = 0;
                    const interval = setInterval(() => {
                        retries++;
                        const micBtn = document.querySelector(
                            'button[data-testid="composer-speech-button"], ' +
                            'button[data-testid="voice-mode-button"], ' +
                            'button[data-testid*="mic" i], ' +
                            'button[data-testid*="speech" i], ' +
                            'button[aria-label*="voice" i], ' +
                            'button[aria-label*="mic" i], ' +
                            'button[aria-label*="speech" i], ' +
                            'button[aria-label*="dictat" i], ' +
                            'button[aria-label*="record" i], ' +
                            'button[aria-label*="microphone" i], ' +
                            'button[title*="voice" i], ' +
                            'button[title*="mic" i], ' +
                            '[role="button"][aria-label*="mic" i], ' +
                            '[role="button"][aria-label*="voice" i], ' +
                            '.mic-button'
                        );
                        if (micBtn) {
                            clearInterval(interval);
                            try {
                                micBtn.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                                ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(evtType => {
                                    const evt = new MouseEvent(evtType, { bubbles: true, cancelable: true, view: window });
                                    micBtn.dispatchEvent(evt);
                                });
                                if (typeof micBtn.click === 'function') {
                                    micBtn.click();
                                }
                            } catch(e) {
                                console.error('ColAI: Mic click failed', e);
                            }
                        } else if (retries >= 35) {
                            clearInterval(interval);
                        }
                    }, 400);
                })();
            """.trimIndent()

            "input" -> """
                (function() {
                    let retries = 0;
                    const interval = setInterval(() => {
                        retries++;
                        const el = document.querySelector(
                            '#prompt-textarea, ' +
                            'textarea[data-testid="prompt-textarea"], ' +
                            'div[contenteditable="true"].ProseMirror, ' +
                            'div[contenteditable="true"], ' +
                            '.ql-editor, ' +
                            '[role="textbox"], ' +
                            'textarea, ' +
                            'input[type="text"]'
                        );
                        if (el) {
                            clearInterval(interval);
                            try {
                                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                el.focus();
                                ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(evtType => {
                                    const evt = new MouseEvent(evtType, { bubbles: true, cancelable: true, view: window });
                                    el.dispatchEvent(evt);
                                });
                                if (el.tagName === 'TEXTAREA' || el.tagName === 'INPUT') {
                                    const len = el.value.length;
                                    el.setSelectionRange(len, len);
                                } else if (el.isContentEditable) {
                                    const range = document.createRange();
                                    range.selectNodeContents(el);
                                    range.collapse(false);
                                    const sel = window.getSelection();
                                    sel.removeAllRanges();
                                    sel.addRange(range);
                                }
                            } catch(e) {
                                console.error('ColAI: Input focus failed', e);
                            }
                        } else if (retries >= 35) {
                            clearInterval(interval);
                        }
                    }, 400);
                })();
            """.trimIndent()

            else -> return
        }

        try {
            if (session.isOpen) {
                session.loadUri("javascript:$js")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing initial action $action", e)
        }
    }

    fun createSession(accountName: String) {
        val service = _uiState.value.currentService ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val newSession = Session(
                id = java.util.UUID.randomUUID().toString(),
                serviceId = service.id,
                accountName = accountName,
                isDefault = false,
                createdAt = System.currentTimeMillis()
            )
            sessionDao.insertSession(newSession)
            val sessions = sessionDao.getSessionsForServiceList(service.id)
            _uiState.update { it.copy(availableSessions = sessions) }
            activateSession(service, newSession)
        }
    }

    /**
     * Clears cookies, cache, and local storage for this container session without affecting other accounts (Item 2).
     */
    fun clearSessionData(session: Session) {
        val service = _uiState.value.currentService ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val storage = EncryptedStorage.getInstance(getApplication())
            val effectiveContextId = resolveEffectiveContextId(session) ?: session.id
            sessionPool.closeSession(session.id)
            sessionPool.clearContainerStorage(effectiveContextId)
            storage.clearSessionState(session.id)
            if (_uiState.value.currentSession?.id == session.id) {
                activateSession(service, session)
            }
        }
    }

    /**
     * Toggles tracking protection / content blocking for a specific container account (Item 6).
     */
    fun toggleContentBlocking(session: Session, enabled: Boolean) {
        val storage = EncryptedStorage.getInstance(getApplication())
        storage.setSessionContentBlockingEnabled(session.id, enabled)
        if (_uiState.value.currentSession?.id == session.id) {
            activeGeckoSession?.settings?.useTrackingProtection = enabled
        }
    }

    /**
     * Fully deletes a container account including all persistent contextId storage (Item 2).
     */
    fun deleteSession(session: Session) {
        val service = _uiState.value.currentService ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val storage = EncryptedStorage.getInstance(getApplication())
            val effectiveContextId = resolveEffectiveContextId(session) ?: session.id
            sessionPool.closeSession(session.id)
            sessionPool.clearContainerStorage(effectiveContextId)
            storage.clearSessionState(session.id)
            storage.setSessionPin(session.id, null)

            sessionDao.deleteSession(session)
            val remaining = sessionDao.getSessionsForServiceList(service.id)
            _uiState.update { it.copy(availableSessions = remaining) }
            if (_uiState.value.currentSession?.id == session.id) {
                remaining.firstOrNull()?.let { activateSession(service, it) }
            }
        }
    }

    private fun isAuthOrSsoUrl(uri: Uri, host: String): Boolean {
        // Broad Google ecosystem authentication support
        if (host == "google.com" || host.endsWith(".google.com") ||
            host == "googleapis.com" || host.endsWith(".googleapis.com") ||
            host == "gstatic.com" || host.endsWith(".gstatic.com") ||
            host == "googleusercontent.com" || host.endsWith(".googleusercontent.com") ||
            host == "youtube.com" || host.endsWith(".youtube.com")
        ) {
            val path = uri.path?.lowercase().orEmpty()
            val query = uri.query?.lowercase().orEmpty()
            if (host.startsWith("accounts.") || host.startsWith("oauth2.") || host.startsWith("myaccount.") ||
                host.startsWith("apis.") || host.startsWith("ssl.") ||
                path.contains("oauth") || path.contains("signin") || path.contains("servicelogin") ||
                path.contains("checkcookie") || path.contains("auth") || query.contains("oauth") ||
                query.contains("client_id") || query.contains("continue") || query.contains("redirect_uri")
            ) {
                return true
            }
        }

        // Generic OAuth / SSO parameters across all identity providers
        val query = uri.query?.lowercase().orEmpty()
        val path = uri.path?.lowercase().orEmpty()
        if (query.contains("response_type=") || query.contains("client_id=") ||
            query.contains("redirect_uri=") || query.contains("code=") ||
            query.contains("state=") || query.contains("session_state=") ||
            path.contains("/oauth") || path.contains("/signin") || path.contains("/callback") ||
            path.contains("/sso") || path.contains("/login") || path.contains("/auth")
        ) {
            return true
        }

        val authDomains = listOf(
            "accounts.google.com",
            "myaccount.google.com",
            "apis.google.com",
            "oauth2.googleapis.com",
            "google.com",
            "appleid.apple.com",
            "idmsa.apple.com",
            "login.microsoftonline.com",
            "login.live.com",
            "account.live.com",
            "msftauth.net",
            "auth.anthropic.com",
            "api.workos.com",
            "auth.workos.com",
            "auth0.com",
            "auth0.openai.com",
            "auth.openai.com",
            "okta.com",
            "clerk.accounts.dev",
            "challenges.cloudflare.com",
            "arkoselabs.com",
            "hcaptcha.com",
            "recaptcha.net",
            "x.com",
            "twitter.com",
            "api.x.com",
            "api.twitter.com"
        )
        return authDomains.any { host == it || host.endsWith(".$it") }
    }

    /**
     * Determines whether [urlString] belongs to the current AI service domain or should open externally (Item 7).
     * Prevents OAuth SSO and authentication redirects from escaping the GeckoView container.
     */
    private fun shouldOpenInExternalBrowser(
        service: AIService,
        urlString: String,
        request: GeckoSession.NavigationDelegate.LoadRequest? = null
    ): Boolean {
        val uri = try { Uri.parse(urlString) } catch (_: Exception) { return false }
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false

        val host = uri.host?.lowercase() ?: return false
        val serviceHost = try { Uri.parse(service.url).host?.lowercase() ?: "" } catch (_: Exception) { "" }

        if (serviceHost.isNotBlank() && (host == serviceHost || host.endsWith(".$serviceHost"))) {
            return false
        }

        val allowedHostsForService = when (service.id.lowercase()) {
            "chatgpt" -> listOf("chatgpt.com", "openai.com", "auth0.openai.com", "oaistatic.com", "oaiusercontent.com")
            "claude" -> listOf("claude.ai", "anthropic.com", "auth.anthropic.com", "claudeusercontent.com")
            "deepseek" -> listOf("deepseek.com")
            "grok" -> listOf("grok.com", "x.ai", "twitter.com", "x.com", "twimg.com")
            "gemini" -> listOf("gemini.google.com", "google.com", "googleusercontent.com", "gstatic.com")
            "perplexity" -> listOf("perplexity.ai", "pplx.ai")
            else -> emptyList()
        }

        if (allowedHostsForService.any { host == it || host.endsWith(".$it") }) {
            return false
        }

        // Keep all SSO / OAuth / Identity Provider transactions strictly inside the container
        if (isAuthOrSsoUrl(uri, host)) {
            return false
        }

        // For redirects: if it is not to the service or an auth flow, it must be opened externally
        // to prevent open-redirect / arbitrary web content loading inside container
        return true
    }

    override fun onCleared() {
        super.onCleared()
        activeGeckoSession?.let {
            it.navigationDelegate = null
            it.contentDelegate = null
            it.progressDelegate = null
            it.historyDelegate = null
            contentBlocker.detach(it)
        }
        oauthPopupSession?.let { popup ->
            try {
                popup.navigationDelegate = null
                popup.contentDelegate = null
                if (popup.isOpen) {
                    popup.close()
                }
            } catch (_: Exception) {}
            oauthPopupSession = null
        }
    }
}
