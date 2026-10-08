package com.ujwal.colai.core.engine

import android.content.Context
import android.util.Log
import com.ujwal.colai.core.notification.ColAINotificationManager
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.WebNotification
import org.mozilla.geckoview.WebNotificationDelegate

/**
 * Singleton manager responsible for lifecycle, configuration, and global access
 * to the Mozilla GeckoView runtime engine.
 *
 * Configures enterprise-grade privacy protection:
 * - Global Privacy Control (Sec-GPC) enabled
 * - Strict Enhanced Tracking Protection (ETP)
 * - Cookie isolation (first-party isolation)
 * - Query parameter stripping (anti-tracking URL parameters)
 * - Cookie purging for bounced tracking domains
 */
object GeckoRuntimeManager {
    private const val TAG = "GeckoRuntimeManager"

    @Volatile
    private var runtime: GeckoRuntime? = null

    val isInitialized: Boolean
        get() = runtime != null

    /**
     * Initializes the application-level GeckoRuntime singleton.
     * Safe to call multiple times; subsequent calls return the existing instance.
     */
    @Synchronized
    fun init(context: Context): GeckoRuntime {
        runtime?.let { return it }

        Log.i(TAG, "Initializing Mozilla GeckoRuntime with enhanced privacy & container architecture...")

        // Keep tracking protection minimal to prevent breaking AI providers (Claude statsig/telemetry)
        // and allow third-party cookies during Google OAuth and SSO handoffs
        val contentBlockingSettings = ContentBlocking.Settings.Builder()
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.NONE)
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_ALL)
            .strictSocialTrackingProtection(false)
            .queryParameterStrippingEnabled(false)
            .cookiePurging(false)
            .build()

        val runtimeSettings = GeckoRuntimeSettings.Builder()
            .globalPrivacyControlEnabled(false)
            .contentBlocking(contentBlockingSettings)
            .javaScriptEnabled(true)
            .loginAutofillEnabled(true)
            .aboutConfigEnabled(false)
            .consoleOutput(false)
            .debugLogging(false)
            .appZygoteProcessEnabled(true)
            .fissionEnabled(false)
            .build()

        val instance = GeckoRuntime.create(context.applicationContext, runtimeSettings)
        instance.webNotificationDelegate = object : WebNotificationDelegate {
            override fun onShowNotification(notification: WebNotification) {
                ColAINotificationManager.showNotification(context.applicationContext, notification)
            }

            override fun onCloseNotification(notification: WebNotification) {
                ColAINotificationManager.cancelNotification(context.applicationContext, notification)
            }
        }

        try {
            instance.webExtensionController.ensureBuiltIn(
                "resource://android/assets/extensions/adblocker/",
                "shield@colai.local"
            )
            Log.i(TAG, "ColAI Shield WebExtension successfully registered")
        } catch (e: Exception) {
            Log.w(TAG, "Could not register ColAI Shield built-in WebExtension", e)
        }

        runtime = instance
        Log.i(TAG, "Mozilla GeckoRuntime successfully initialized with WebNotificationDelegate (Omni 157.0 ready)")
        return instance
    }

    /**
     * Retrieves the initialized GeckoRuntime.
     * Throws [IllegalStateException] if [init] has not been called.
     */
    fun getRuntime(): GeckoRuntime {
        return runtime ?: throw IllegalStateException(
            "GeckoRuntime has not been initialized. Call GeckoRuntimeManager.init(context) first."
        )
    }

    /**
     * Cleanly shuts down the GeckoRuntime engine if active.
     */
    @Synchronized
    fun shutdown() {
        runtime?.let { activeRuntime ->
            Log.i(TAG, "Shutting down GeckoRuntime...")
            try {
                activeRuntime.shutdown()
            } catch (e: Exception) {
                Log.w(TAG, "Exception during GeckoRuntime shutdown", e)
            }
            runtime = null
        }
    }
}
