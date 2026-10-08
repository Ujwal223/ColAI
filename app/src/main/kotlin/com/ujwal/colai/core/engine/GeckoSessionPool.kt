package com.ujwal.colai.core.engine

import android.util.Log
import com.ujwal.colai.ColAIApp
import com.ujwal.colai.feature.webview.GeckoPromptDelegateRegistry
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

/**
 * Manages an LRU pool of containerized [GeckoSession] instances.
 *
 * Each container session is strictly isolated using Mozilla Gecko's native `contextId`.
 * This isolates:
 * - Cookies & Session Tokens
 * - LocalStorage & IndexedDB
 * - Cache Storage & HTTP Auth
 *
 * Features:
 * - LRU eviction to prevent high memory pressure with multiple open AI tabs.
 * - Prioritization hints for the active foreground tab vs background tabs.
 * - Storage wiping per container when accounts are removed.
 */
class GeckoSessionPool(
    private val runtime: GeckoRuntime = GeckoRuntimeManager.getRuntime(),
    private val maxCapacity: Int = MAX_POOL_CAPACITY
) {
    companion object {
        private const val TAG = "GeckoSessionPool"
        const val MAX_POOL_CAPACITY = 5

        @Volatile
        private var instance: GeckoSessionPool? = null

        /**
         * Singleton accessor for global session pool.
         */
        fun getInstance(): GeckoSessionPool {
            return instance ?: synchronized(this) {
                instance ?: GeckoSessionPool().also { instance = it }
            }
        }
    }

    /**
     * Managed wrapper holding session reference and metadata.
     */
    data class PooledSession(
        val sessionId: String,
        val contextId: String,
        val session: GeckoSession,
        var isDesktopMode: Boolean = false,
        var lastAccessedAt: Long = System.currentTimeMillis()
    )

    private val pool = LinkedHashMap<String, PooledSession>(maxCapacity, 0.75f, true)
    private var activeSessionId: String? = null

    /**
     * Retrieves an existing session for [sessionId], or creates and initializes
     * a new isolated container session using [contextId].
     *
     * @param sessionId Unique session identifier (e.g. per-provider session ID).
     * @param contextId Unique container identifier for cookies/storage (e.g. shared_personal_container or session ID).
     * @param targetUrl Optional URL to pre-configure user-agent headers.
     * @param isDesktopMode Whether to request desktop user agent and viewport.
     * @return Fully initialized and opened [GeckoSession].
     */
    @Synchronized
    fun getOrCreateSession(
        sessionId: String,
        contextId: String,
        targetUrl: String? = null,
        isDesktopMode: Boolean = false
    ): GeckoSession {
        require(sessionId.isNotBlank()) { "sessionId must not be blank" }
        require(contextId.isNotBlank()) { "contextId must not be blank" }

        val existing = pool[sessionId]
        if (existing != null && existing.session.isOpen) {
            existing.lastAccessedAt = System.currentTimeMillis()
            Log.d(TAG, "Reusing existing container session for sessionId: $sessionId (contextId: $contextId)")
            return existing.session
        }

        // Evict LRU session if pool reached capacity
        if (pool.size >= maxCapacity) {
            evictOldestInactiveSession()
        }

        Log.i(TAG, "Creating new GeckoSession for sessionId: $sessionId with container contextId: $contextId")

        val userAgent = UserAgentGenerator.getOptimizedUserAgent(targetUrl, isDesktopMode)
        val viewportMode = if (isDesktopMode) {
            GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        }
        val userAgentMode = if (isDesktopMode) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }

        val settings = GeckoSessionSettings.Builder()
            .contextId(contextId)
            .usePrivateMode(false)
            .useTrackingProtection(false)
            .viewportMode(viewportMode)
            .userAgentMode(userAgentMode)
            .userAgentOverride(userAgent)
            .suspendMediaWhenInactive(true)
            .allowJavascript(true)
            .build()

        val session = GeckoSession(settings)
        try {
            val delegate = GeckoPromptDelegateRegistry.createDelegate(ColAIApp.instance)
            session.promptDelegate = delegate
            session.permissionDelegate = delegate
        } catch (e: Exception) {
            Log.w(TAG, "Could not attach GeckoPromptDelegate, falling back to default", e)
        }
        session.open(runtime)

        pool[sessionId] = PooledSession(
            sessionId = sessionId,
            contextId = contextId,
            session = session,
            isDesktopMode = isDesktopMode
        )

        return session
    }

    /**
     * Preloads and warms up a container session in the background with the given [targetUrl].
     * Marks the session as inactive and PRIORITY_DEFAULT so it does not compete with foreground UI.
     */
    @Synchronized
    fun preloadSession(
        sessionId: String,
        contextId: String,
        targetUrl: String,
        isDesktopMode: Boolean = false
    ): GeckoSession {
        val existing = pool[sessionId]
        if (existing != null && existing.session.isOpen) {
            return existing.session
        }
        val session = getOrCreateSession(sessionId, contextId, targetUrl, isDesktopMode)
        session.setActive(false)
        session.setPriorityHint(GeckoSession.PRIORITY_DEFAULT)
        session.loadUri(targetUrl)
        Log.i(TAG, "Preloaded background container session sessionId: $sessionId, contextId: $contextId, url: $targetUrl")
        return session
    }

    /**
     * Marks the session with [sessionId] as active (foreground), setting its priority
     * to [GeckoSession.PRIORITY_HIGH] and marking all other sessions inactive.
     */
    @Synchronized
    fun setActiveSession(sessionId: String) {
        activeSessionId = sessionId
        for ((id, pooled) in pool) {
            if (id == sessionId) {
                pooled.session.setActive(true)
                pooled.session.setPriorityHint(GeckoSession.PRIORITY_HIGH)
                pooled.lastAccessedAt = System.currentTimeMillis()
            } else {
                pooled.session.setActive(false)
                pooled.session.setPriorityHint(GeckoSession.PRIORITY_DEFAULT)
            }
        }
    }

    /**
     * Checks if a session for [sessionId] exists and is open.
     */
    @Synchronized
    fun hasSession(sessionId: String): Boolean {
        return pool[sessionId]?.session?.isOpen == true
    }

    /**
     * Returns the currently active sessionId, if any.
     */
    @Synchronized
    fun getActiveSessionId(): String? = activeSessionId

    /**
     * Returns the number of currently opened sessions in the pool.
     */
    @Synchronized
    fun getSessionCount(): Int = pool.size

    /**
     * Closes and removes a specific session from the pool by [sessionId].
     */
    @Synchronized
    fun closeSession(sessionId: String) {
        val removed = pool.remove(sessionId)
        if (removed != null) {
            Log.d(TAG, "Closing container session for sessionId: $sessionId")
            try {
                if (removed.session.isOpen) {
                    removed.session.close()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error closing GeckoSession for sessionId: $sessionId", e)
            }
        }
        if (activeSessionId == sessionId) {
            activeSessionId = null
        }
    }

    /**
     * Completely wipes all persistent data (cookies, cache, IndexedDB, localStorage)
     * belonging to this isolated container [contextId].
     */
    @Synchronized
    fun clearContainerStorage(contextId: String) {
        // Close all pooled sessions belonging to this contextId
        val sessionsToRemove = pool.filterValues { it.contextId == contextId }.keys.toList()
        for (sid in sessionsToRemove) {
            closeSession(sid)
        }
        Log.i(TAG, "Purging persistent container storage for contextId: $contextId")
        try {
            runtime.storageController.clearDataForSessionContext(contextId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear session context storage for contextId: $contextId", e)
        }
    }

    /**
     * Closes all active sessions in the pool.
     */
    @Synchronized
    fun closeAll() {
        Log.i(TAG, "Closing all sessions in GeckoSessionPool (${pool.size} active)")
        for ((_, pooled) in pool) {
            try {
                if (pooled.session.isOpen) {
                    pooled.session.close()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error closing session", e)
            }
        }
        pool.clear()
        activeSessionId = null
    }

    /**
     * Evicts the oldest session that is NOT the currently active session.
     */
    private fun evictOldestInactiveSession() {
        val candidate = pool.entries.firstOrNull { it.key != activeSessionId }
        if (candidate != null) {
            Log.i(TAG, "Evicting LRU background session for sessionId: ${candidate.key}")
            try {
                if (candidate.value.session.isOpen) {
                    candidate.value.session.close()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error closing evicted session", e)
            }
            pool.remove(candidate.key)
        }
    }
}
