package com.ujwal.colai.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure key-value vault utilizing Android Jetpack Security with Android KeyStore MasterKey.
 *
 * Uses:
 * - AES-256-SIV for deterministic key encryption
 * - AES-256-GCM for authenticated payload encryption
 *
 * Persists sensitive session credentials, theme settings, SSO toggles, and device state.
 */
class EncryptedStorage internal constructor(
    private val prefs: SharedPreferences
) {

    companion object {
        private const val TAG = "EncryptedStorage"
        private const val PREFS_FILE_NAME = "colai_secure_vault"

        // Common Keys
        const val KEY_THEME_MODE = "theme_mode" // "light", "dark", "system"
        const val KEY_CONTRAST_LEVEL = "contrast_level" // "standard", "high"
        const val KEY_ENABLE_SSO = "enable_sso"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_ACTIVE_SERVICE_ID = "active_service_id"
        const val KEY_ACTIVE_SESSION_ID = "active_session_id"
        const val KEY_USER_AGENT_OVERRIDE = "user_agent_override"
        const val KEY_DESKTOP_MODE_DEFAULT = "desktop_mode_default"
        const val KEY_DO_NOT_TRACK = "do_not_track"
        const val KEY_CONTENT_BLOCKING = "content_blocking_enabled"
        const val KEY_MASTER_RECOVERY_KEY = "master_recovery_key"
        const val KEY_WIDGET_SERVICE_ID = "widget_service_id"
        const val KEY_WIDGET_SESSION_ID = "widget_session_id"
        const val KEY_SHARED_PERSONAL_COOKIES = "shared_personal_cookies"

        @Volatile
        private var instance: EncryptedStorage? = null

        /**
         * Returns singleton instance of [EncryptedStorage].
         */
        fun getInstance(context: Context): EncryptedStorage {
            return instance ?: synchronized(this) {
                instance ?: create(context).also { instance = it }
            }
        }

        /**
         * Factory method creating hardware-backed EncryptedSharedPreferences.
         */
        private fun create(context: Context): EncryptedStorage {
            val appContext = context.applicationContext
            return try {
                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val encryptedPrefs = EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                EncryptedStorage(encryptedPrefs)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize EncryptedSharedPreferences with KeyStore, recreating...", e)
                try {
                    // In case of corrupted KeyStore entries (e.g. app reinstall or backup restore)
                    appContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).edit().clear().apply()
                    val masterKey = MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    val encryptedPrefs = EncryptedSharedPreferences.create(
                        appContext,
                        PREFS_FILE_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                    EncryptedStorage(encryptedPrefs)
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "Fatal fallback error creating EncryptedSharedPreferences", fallbackException)
                    throw fallbackException
                }
            }
        }

        /**
         * Test factory allowing injection of standard or mock SharedPreferences for JVM tests.
         */
        fun forTesting(sharedPreferences: SharedPreferences): EncryptedStorage {
            return EncryptedStorage(sharedPreferences)
        }
    }

    fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return prefs.getString(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    // High-level App Preference Accessors

    fun getThemeMode(): String {
        return getString(KEY_THEME_MODE, "system") ?: "system"
    }

    fun setThemeMode(mode: String) {
        putString(KEY_THEME_MODE, mode)
    }

    fun getContrastLevel(): String {
        return getString(KEY_CONTRAST_LEVEL, "standard") ?: "standard"
    }

    fun setContrastLevel(level: String) {
        putString(KEY_CONTRAST_LEVEL, level)
    }

    fun isSsoEnabled(): Boolean {
        return getBoolean(KEY_ENABLE_SSO, true)
    }

    fun setSsoEnabled(enabled: Boolean) {
        putBoolean(KEY_ENABLE_SSO, enabled)
    }

    fun isOnboardingCompleted(): Boolean {
        return getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        putBoolean(KEY_ONBOARDING_COMPLETED, completed)
    }

    fun getActiveServiceId(): String? {
        return getString(KEY_ACTIVE_SERVICE_ID)
    }

    fun setActiveServiceId(serviceId: String?) {
        putString(KEY_ACTIVE_SERVICE_ID, serviceId)
    }

    fun getActiveSessionId(): String? {
        return getString(KEY_ACTIVE_SESSION_ID)
    }

    fun setActiveSessionId(sessionId: String?) {
        putString(KEY_ACTIVE_SESSION_ID, sessionId)
    }

    fun getWidgetServiceId(): String? {
        return getString(KEY_WIDGET_SERVICE_ID)
    }

    fun setWidgetServiceId(serviceId: String?) {
        putString(KEY_WIDGET_SERVICE_ID, serviceId)
    }

    fun getWidgetSessionId(): String? {
        return getString(KEY_WIDGET_SESSION_ID)
    }

    fun setWidgetSessionId(sessionId: String?) {
        putString(KEY_WIDGET_SESSION_ID, sessionId)
    }

    fun setWidgetTarget(serviceId: String?, sessionId: String?) {
        setWidgetServiceId(serviceId)
        setWidgetSessionId(sessionId)
    }

    fun isDoNotTrackEnabled(): Boolean {
        return getBoolean(KEY_DO_NOT_TRACK, true)
    }

    fun setDoNotTrackEnabled(enabled: Boolean) {
        putBoolean(KEY_DO_NOT_TRACK, enabled)
    }

    fun isContentBlockingEnabled(): Boolean {
        return getBoolean(KEY_CONTENT_BLOCKING, true)
    }

    fun setContentBlockingEnabled(enabled: Boolean) {
        putBoolean(KEY_CONTENT_BLOCKING, enabled)
    }

    fun isDesktopModeDefault(): Boolean {
        return getBoolean(KEY_DESKTOP_MODE_DEFAULT, false)
    }

    fun setDesktopModeDefault(enabled: Boolean) {
        putBoolean(KEY_DESKTOP_MODE_DEFAULT, enabled)
    }

    fun isSharedPersonalCookiesEnabled(): Boolean {
        return getBoolean(KEY_SHARED_PERSONAL_COOKIES, true)
    }

    fun setSharedPersonalCookiesEnabled(enabled: Boolean) {
        putBoolean(KEY_SHARED_PERSONAL_COOKIES, enabled)
    }

    /**
     * Associates an optional 4-digit PIN lock with an isolated [sessionId].
     * Passing null or blank removes the PIN lock.
     */
    fun setSessionPin(sessionId: String, pin: String?) {
        if (pin.isNullOrBlank()) {
            prefs.edit().remove("session_pin_$sessionId").commit()
        } else {
            prefs.edit().putString("session_pin_$sessionId", pin.trim()).commit()
        }
    }

    fun removeSessionPin(sessionId: String) {
        setSessionPin(sessionId, null)
    }

    fun getSessionPin(sessionId: String): String? {
        return prefs.getString("session_pin_$sessionId", null)
    }

    fun isSessionPinLocked(sessionId: String): Boolean {
        val pin = getSessionPin(sessionId)
        return !pin.isNullOrBlank()
    }

    fun verifySessionPin(sessionId: String, inputPin: String): Boolean {
        val pin = getSessionPin(sessionId) ?: return true
        return java.security.MessageDigest.isEqual(
            pin.toByteArray(Charsets.UTF_8),
            inputPin.trim().toByteArray(Charsets.UTF_8)
        )
    }

    // --- Master Recovery Key (Universal Reset for Forgotten Session Locks) ---

    fun getMasterRecoveryKey(): String? {
        return prefs.getString(KEY_MASTER_RECOVERY_KEY, null)
    }

    fun setMasterRecoveryKey(key: String?) {
        if (key.isNullOrBlank()) {
            prefs.edit().remove(KEY_MASTER_RECOVERY_KEY).commit()
        } else {
            prefs.edit().putString(KEY_MASTER_RECOVERY_KEY, key.trim()).commit()
        }
    }

    fun hasMasterRecoveryKey(): Boolean {
        return !getMasterRecoveryKey().isNullOrBlank()
    }

    fun verifyMasterRecoveryKey(key: String): Boolean {
        val stored = getMasterRecoveryKey() ?: return false
        return java.security.MessageDigest.isEqual(
            stored.toByteArray(Charsets.UTF_8),
            key.trim().toByteArray(Charsets.UTF_8)
        )
    }

    // --- Per-Account Content Blocking & Tracking Protection ---

    fun isSessionContentBlockingEnabled(sessionId: String): Boolean {
        return prefs.getBoolean("content_blocking_$sessionId", false)
    }

    fun setSessionContentBlockingEnabled(sessionId: String, enabled: Boolean) {
        prefs.edit().putBoolean("content_blocking_$sessionId", enabled).apply()
    }

    // --- GeckoSession State Persistence (Restoration across Process Death) ---

    fun saveSessionState(sessionId: String, stateJson: String) {
        prefs.edit().putString("gecko_session_state_$sessionId", stateJson).apply()
    }

    fun getSessionState(sessionId: String): String? {
        return prefs.getString("gecko_session_state_$sessionId", null)
    }

    fun clearSessionState(sessionId: String) {
        prefs.edit().remove("gecko_session_state_$sessionId").apply()
    }
}

