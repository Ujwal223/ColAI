package com.ujwal.colai.core.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.database.ServiceDao
import com.ujwal.colai.core.database.SessionDao
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Migration engine for upgrading existing installations to the modern Native Room database and EncryptedStorage.
 *
 * Reads legacy SharedPreferences stored in prior installations:
 * - `ai_services` -> Room `ai_services` table
 * - `sessions` -> Room `sessions` table
 * - `theme_mode`, `enable_sso`, etc. -> [EncryptedStorage]
 *
 * Ensures zero data loss for existing users, preserving their custom AI services,
 * account labels, container IDs, and preferences.
 */
class LegacyDataMigrator(
    private val serviceDao: ServiceDao,
    private val sessionDao: SessionDao,
    private val encryptedStorage: EncryptedStorage,
    private val legacyPrefs: SharedPreferences
) {

    constructor(
        database: AppDatabase,
        encryptedStorage: EncryptedStorage,
        legacyPrefs: SharedPreferences
    ) : this(
        serviceDao = database.serviceDao(),
        sessionDao = database.sessionDao(),
        encryptedStorage = encryptedStorage,
        legacyPrefs = legacyPrefs
    )

    data class MigrationResult(
        val isMigrated: Boolean,
        val servicesMigrated: Int,
        val sessionsMigrated: Int,
        val preferencesMigrated: Boolean
    )

    companion object {
        private const val TAG = "LegacyDataMigrator"
        const val LEGACY_PREFS_NAME = "FlutterSharedPreferences"
        const val KEY_MIGRATION_COMPLETED = "legacy_data_migration_completed"

        // Legacy keys (prefixed and unprefixed)
        private const val LEGACY_PREFIX = "flutter."
        private const val KEY_AI_SERVICES = "ai_services"
        private const val KEY_SESSIONS = "sessions"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CONTRAST_LEVEL = "contrast_level"
        private const val KEY_ENABLE_SSO = "enable_sso"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

        /**
         * Factory method creating migrator with real app context.
         */
        fun create(context: Context): LegacyDataMigrator {
            val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            val database = AppDatabase.getDatabase(context)
            val encryptedStorage = EncryptedStorage.getInstance(context)
            return LegacyDataMigrator(database, encryptedStorage, legacyPrefs)
        }
    }

    /**
     * Executes migration if not already completed and legacy data is detected.
     */
    suspend fun migrateIfNeeded(): MigrationResult {
        if (encryptedStorage.getBoolean(KEY_MIGRATION_COMPLETED, false)) {
            Log.d(TAG, "Legacy data migration has already been executed.")
            return MigrationResult(isMigrated = false, servicesMigrated = 0, sessionsMigrated = 0, preferencesMigrated = false)
        }

        val hasServices = hasLegacyKey(KEY_AI_SERVICES)
        val hasSessions = hasLegacyKey(KEY_SESSIONS)
        val hasSettings = hasLegacyKey(KEY_THEME_MODE) || hasLegacyKey(KEY_ONBOARDING_COMPLETE)

        if (!hasServices && !hasSessions && !hasSettings) {
            Log.d(TAG, "No legacy data detected. Fresh installation.")
            encryptedStorage.putBoolean(KEY_MIGRATION_COMPLETED, true)
            return MigrationResult(isMigrated = false, servicesMigrated = 0, sessionsMigrated = 0, preferencesMigrated = false)
        }

        Log.i(TAG, "Migrating legacy data into Native Room and EncryptedStorage...")

        var servicesCount = 0
        var sessionsCount = 0
        var prefsMigrated = false

        try {
            // 1. Migrate AI Services
            val rawServicesJson = getLegacyString(KEY_AI_SERVICES)
            if (!rawServicesJson.isNullOrBlank()) {
                val services = parseServices(rawServicesJson)
                if (services.isNotEmpty()) {
                    serviceDao.insertServices(services)
                    servicesCount = services.size
                    Log.i(TAG, "Migrated $servicesCount AI services from legacy storage.")
                }
            }

            // 2. Migrate Sessions
            val rawSessionsJson = getLegacyString(KEY_SESSIONS)
            if (!rawSessionsJson.isNullOrBlank()) {
                val sessions = parseSessions(rawSessionsJson)
                if (sessions.isNotEmpty()) {
                    sessionDao.insertSessions(sessions)
                    sessionsCount = sessions.size
                    Log.i(TAG, "Migrated $sessionsCount sessions from legacy storage.")
                }
            }

            // 3. Migrate App Preferences
            migratePreferences()
            prefsMigrated = true

            // Mark completed
            encryptedStorage.putBoolean(KEY_MIGRATION_COMPLETED, true)
            Log.i(TAG, "Legacy data migration successfully completed!")

            return MigrationResult(
                isMigrated = true,
                servicesMigrated = servicesCount,
                sessionsMigrated = sessionsCount,
                preferencesMigrated = prefsMigrated
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing legacy data migration", e)
            return MigrationResult(
                isMigrated = false,
                servicesMigrated = servicesCount,
                sessionsMigrated = sessionsCount,
                preferencesMigrated = prefsMigrated
            )
        }
    }

    private fun hasLegacyKey(key: String): Boolean {
        return legacyPrefs.contains(LEGACY_PREFIX + key) || legacyPrefs.contains(key)
    }

    private fun getLegacyString(key: String): String? {
        return legacyPrefs.getString(LEGACY_PREFIX + key, null)
            ?: legacyPrefs.getString(key, null)
    }

    private fun getLegacyBoolean(key: String, defaultValue: Boolean): Boolean {
        val prefKey = if (legacyPrefs.contains(LEGACY_PREFIX + key)) LEGACY_PREFIX + key else key
        return try {
            legacyPrefs.getBoolean(prefKey, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun migratePreferences() {
        val themeMode = getLegacyString(KEY_THEME_MODE)
        if (!themeMode.isNullOrBlank()) {
            encryptedStorage.setThemeMode(themeMode)
        }

        val contrastLevel = getLegacyString(KEY_CONTRAST_LEVEL)
        if (!contrastLevel.isNullOrBlank()) {
            encryptedStorage.setContrastLevel(contrastLevel)
        }

        if (hasLegacyKey(KEY_ENABLE_SSO)) {
            val enableSSO = getLegacyBoolean(KEY_ENABLE_SSO, true)
            encryptedStorage.setSsoEnabled(enableSSO)
        }

        if (hasLegacyKey(KEY_ONBOARDING_COMPLETE)) {
            val onboardingComplete = getLegacyBoolean(KEY_ONBOARDING_COMPLETE, false)
            encryptedStorage.setOnboardingCompleted(onboardingComplete)
        }
    }

    internal fun parseServices(jsonString: String): List<AIService> {
        val result = mutableListOf<AIService>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val url = obj.getString("url")
                val faviconUrl = obj.optString("faviconUrl", "")
                val iconPath = if (obj.has("iconPath") && !obj.isNull("iconPath")) obj.getString("iconPath") else null
                val customUserAgent = if (obj.has("customUserAgent") && !obj.isNull("customUserAgent")) obj.getString("customUserAgent") else null

                var customHeaders: Map<String, String>? = null
                if (obj.has("customHeaders") && !obj.isNull("customHeaders")) {
                    val headersObj = obj.getJSONObject("customHeaders")
                    val map = mutableMapOf<String, String>()
                    val keys = headersObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        map[k] = headersObj.optString(k)
                    }
                    customHeaders = map
                }

                val widgetSessionId = if (obj.has("widgetSessionId") && !obj.isNull("widgetSessionId")) obj.getString("widgetSessionId") else null
                val notificationsEnabled = obj.optBoolean("notificationsEnabled", true)
                val createdAtStr = if (obj.has("createdAt") && !obj.isNull("createdAt")) obj.getString("createdAt") else null
                val createdAt = parseTimestamp(createdAtStr)

                result.add(
                    AIService(
                        id = id,
                        name = name,
                        url = url,
                        faviconUrl = faviconUrl,
                        iconPath = iconPath,
                        customUserAgent = customUserAgent,
                        customHeaders = customHeaders,
                        widgetSessionId = widgetSessionId,
                        notificationsEnabled = notificationsEnabled,
                        sortOrder = i,
                        createdAt = createdAt
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing legacy AI services JSON", e)
        }
        return result
    }

    internal fun parseSessions(jsonString: String): List<Session> {
        val result = mutableListOf<Session>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val serviceId = obj.getString("serviceId")
                val accountName = obj.optString("accountName", "Account ${i + 1}")
                val isDefault = obj.optBoolean("isDefault", false)
                val lastAccessedStr = if (obj.has("lastAccessed") && !obj.isNull("lastAccessed")) obj.getString("lastAccessed") else null
                val lastAccessed = parseTimestamp(lastAccessedStr)
                val cookieStorePath = if (obj.has("cookieStorePath") && !obj.isNull("cookieStorePath")) obj.getString("cookieStorePath") else null
                val notificationsEnabled = obj.optBoolean("notificationsEnabled", true)
                val themeMode = if (obj.has("themeMode") && !obj.isNull("themeMode")) obj.getString("themeMode") else null
                val customColors = if (obj.has("customColors") && !obj.isNull("customColors")) obj.getString("customColors") else null

                result.add(
                    Session(
                        id = id,
                        serviceId = serviceId,
                        accountName = accountName,
                        isDefault = isDefault,
                        lastAccessed = lastAccessed,
                        cookieStorePath = cookieStorePath,
                        notificationsEnabled = notificationsEnabled,
                        themeMode = themeMode,
                        customColors = customColors,
                        createdAt = lastAccessed
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing legacy sessions JSON", e)
        }
        return result
    }

    private fun parseTimestamp(rawDate: String?): Long {
        if (rawDate.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            Instant.parse(rawDate).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
