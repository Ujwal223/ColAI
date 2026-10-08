package com.ujwal.colai.core.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.engine.GeckoSessionPool
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val TAG = "BackupManager"
private const val BACKUP_FORMAT = "colai-backup-v1"
private const val PBKDF2_ITERATIONS = 100_000
private const val KEY_LENGTH_BITS = 256
private const val GCM_TAG_LENGTH_BITS = 128
private const val SALT_LENGTH_BYTES = 16
private const val IV_LENGTH_BYTES = 12

/**
 * Granular options selecting which application data categories to export.
 */
data class BackupOptions(
    val includeLogins: Boolean = true,
    val includeServices: Boolean = true,
    val includeSecurity: Boolean = true,
    val includePreferences: Boolean = true
)

/**
 * Granular options selecting which application data categories to restore.
 */
data class RestoreOptions(
    val restoreLogins: Boolean = true,
    val restoreServices: Boolean = true,
    val restoreSecurity: Boolean = true,
    val restorePreferences: Boolean = true
)

/**
 * Decrypted backup manifest summary presented to the user before restoration.
 */
data class BackupInspection(
    val timestamp: Long,
    val hasLogins: Boolean,
    val hasServices: Boolean,
    val hasSecurity: Boolean,
    val hasPreferences: Boolean,
    val serviceCount: Int,
    val sessionCount: Int
)

/**
 * Handles military-grade AES-256-GCM encrypted backup, inspection, and selective restore
 * of application preferences, session PIN locks, AI services, and persistent Gecko profile logins.
 */
object BackupManager {

    /**
     * Creates an encrypted backup file written directly to [outputStream].
     *
     * @param context Application context
     * @param password User-supplied encryption password
     * @param outputStream Target output stream (e.g. from Storage Access Framework)
     * @param options Category options defining what data to include
     */
    suspend fun createBackup(
        context: Context,
        password: String,
        outputStream: OutputStream,
        options: BackupOptions = BackupOptions()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(password.isNotBlank()) { "Backup password cannot be empty" }

            val db = AppDatabase.getDatabase(context)
            val storage = EncryptedStorage.getInstance(context)

            val services = if (options.includeServices) db.serviceDao().getAllServicesList() else emptyList()
            val sessions = if (options.includeServices) db.sessionDao().getAllSessionsList() else emptyList()

            // Construct payload JSON
            val payload = JSONObject().apply {
                put("version", 2)
                put("timestamp", System.currentTimeMillis())

                // 1. Preferences & App Settings
                if (options.includePreferences) {
                    val prefsObj = JSONObject().apply {
                        put("theme_mode", storage.getThemeMode())
                        put("contrast_level", storage.getContrastLevel())
                        put("desktop_mode_default", storage.isDesktopModeDefault())
                        put("do_not_track", storage.isDoNotTrackEnabled())
                        put("content_blocking_enabled", storage.isContentBlockingEnabled())
                        put("enable_sso", storage.isSsoEnabled())
                        put("shared_personal_cookies", storage.isSharedPersonalCookiesEnabled())

                        val cbObj = JSONObject()
                        db.sessionDao().getAllSessionsList().forEach { s ->
                            cbObj.put(s.id, storage.isSessionContentBlockingEnabled(s.id))
                        }
                        put("session_content_blocking", cbObj)
                    }
                    put("preferences", prefsObj)
                }

                // 2. Security & Session Locks
                if (options.includeSecurity) {
                    val securityObj = JSONObject().apply {
                        storage.getMasterRecoveryKey()?.let { put("master_recovery_key", it) }
                        val pinsObj = JSONObject()
                        db.sessionDao().getAllSessionsList().forEach { s ->
                            storage.getSessionPin(s.id)?.let { pinsObj.put(s.id, it) }
                        }
                        put("session_pins", pinsObj)
                    }
                    put("security", securityObj)
                }

                // 3. AI Services & Container Sessions
                if (options.includeServices) {
                    val servicesArr = JSONArray()
                    services.forEach { s ->
                        val sObj = JSONObject().apply {
                            put("id", s.id)
                            put("name", s.name)
                            put("url", s.url)
                            put("faviconUrl", s.faviconUrl)
                            s.iconPath?.let { put("iconPath", it) }
                            s.customUserAgent?.let { put("customUserAgent", it) }
                            s.widgetSessionId?.let { put("widgetSessionId", it) }
                            put("notificationsEnabled", s.notificationsEnabled)
                            put("sortOrder", s.sortOrder)
                            put("createdAt", s.createdAt)
                        }
                        servicesArr.put(sObj)
                    }
                    put("services", servicesArr)

                    val sessionsArr = JSONArray()
                    sessions.forEach { s ->
                        val sObj = JSONObject().apply {
                            put("id", s.id)
                            put("serviceId", s.serviceId)
                            put("accountName", s.accountName)
                            put("isDefault", s.isDefault)
                            put("lastAccessed", s.lastAccessed)
                            s.cookieStorePath?.let { put("cookieStorePath", it) }
                            put("notificationsEnabled", s.notificationsEnabled)
                            s.themeMode?.let { put("themeMode", it) }
                            s.customColors?.let { put("customColors", it) }
                            put("createdAt", s.createdAt)
                            storage.getSessionState(s.id)?.let { put("geckoSessionState", it) }
                        }
                        sessionsArr.put(sObj)
                    }
                    put("sessions", sessionsArr)
                }

                // 4. Web Logins, Cookies, and Gecko Profiles
                if (options.includeLogins) {
                    val mozillaDir = File(context.filesDir, "mozilla")
                    if (mozillaDir.exists()) {
                        val tempZip = File(context.cacheDir, "mozilla_temp_${System.currentTimeMillis()}.zip")
                        try {
                            tempZip.outputStream().use { fos ->
                                ZipOutputStream(fos.buffered()).use { zos ->
                                    zipDirectory(mozillaDir, zos, "")
                                }
                            }
                            if (tempZip.length() > 0) {
                                val zipBytes = tempZip.readBytes()
                                put("mozilla_profile_zip", Base64.getEncoder().encodeToString(zipBytes))
                                Log.i(TAG, "Packed Gecko profile into backup (${zipBytes.size} bytes compressed)")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to compress mozilla profile directory", e)
                        } finally {
                            tempZip.delete()
                        }
                    }
                }

                // 5. Pack Logos & Custom Service Images (optimized and deduplicated)
                try {
                    val logosDir = File(context.filesDir, "logos")
                    val referencedCustomNames = services.mapNotNull { it.iconPath?.let { p -> File(p).name } }.toSet()
                    val customFiles = context.filesDir.listFiles { _, name ->
                        name.startsWith("custom_") && name.endsWith(".png") && referencedCustomNames.contains(name)
                    }
                    val hasLogos = (logosDir.exists() && (logosDir.listFiles()?.isNotEmpty() == true)) || (!customFiles.isNullOrEmpty())
                    if (hasLogos) {
                        val baos = ByteArrayOutputStream()
                        ZipOutputStream(baos).use { zos ->
                            if (logosDir.exists()) {
                                zipDirectory(logosDir, zos, "logos")
                            }
                            customFiles?.forEach { customFile ->
                                writeOptimizedIcon(customFile, zos, customFile.name)
                            }
                        }
                        val zipBytes = baos.toByteArray()
                        if (zipBytes.isNotEmpty()) {
                            put("logos_zip", Base64.getEncoder().encodeToString(zipBytes))
                            Log.i(TAG, "Packed logos and custom icons into backup (${zipBytes.size} bytes compressed)")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to compress logos directory", e)
                }
            }

            val envelopeJson = encryptString(payload.toString(), password)

            outputStream.use { out ->
                out.write(envelopeJson.toByteArray(Charsets.UTF_8))
                out.flush()
            }

            Log.i(TAG, "Successfully exported encrypted backup with options: $options")
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create encrypted backup", e)
            Result.failure(e)
        }
    }

    /**
     * Inspects a backup file with [password] to determine its available data categories.
     * Allows presenting selective restore options to the user.
     */
    suspend fun inspectBackup(
        context: Context,
        password: String,
        inputStream: InputStream
    ): Result<BackupInspection> = withContext(Dispatchers.IO) {
        try {
            require(password.isNotBlank()) { "Password cannot be empty" }
            val rawJson = inputStream.use { it.bufferedReader(Charsets.UTF_8).readText() }
            val decryptedJson = decryptString(rawJson, password)
            val payload = JSONObject(decryptedJson)

            val timestamp = payload.optLong("timestamp", System.currentTimeMillis())
            val hasLogins = payload.has("mozilla_profile_zip")
            val hasServices = payload.has("services") || payload.has("sessions")
            val hasPreferences = payload.has("preferences")

            val hasSecurity = payload.has("security") ||
                (payload.has("preferences") && payload.getJSONObject("preferences").has("session_pins")) ||
                (payload.has("preferences") && payload.getJSONObject("preferences").has("master_recovery_key"))

            val serviceCount = payload.optJSONArray("services")?.length() ?: 0
            val sessionCount = payload.optJSONArray("sessions")?.length() ?: 0

            Result.success(
                BackupInspection(
                    timestamp = timestamp,
                    hasLogins = hasLogins,
                    hasServices = hasServices,
                    hasSecurity = hasSecurity,
                    hasPreferences = hasPreferences,
                    serviceCount = serviceCount,
                    sessionCount = sessionCount
                )
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed inspecting backup file", e)
            Result.failure(e)
        }
    }

    /**
     * Decrypts and restores selected components from [inputStream] into the database and encrypted storage.
     *
     * @param context Application context
     * @param password User-supplied decryption password
     * @param inputStream Source input stream (e.g. from Storage Access Framework)
     * @param options Granular restore preferences
     */
    suspend fun restoreBackup(
        context: Context,
        password: String,
        inputStream: InputStream,
        options: RestoreOptions = RestoreOptions()
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            require(password.isNotBlank()) { "Password cannot be empty" }

            val rawJson = inputStream.use { it.bufferedReader(Charsets.UTF_8).readText() }
            val decryptedJson = decryptString(rawJson, password)
            val payload = JSONObject(decryptedJson)

            val db = AppDatabase.getDatabase(context)
            val storage = EncryptedStorage.getInstance(context)

            // 1. Restore Preferences
            if (options.restorePreferences && payload.has("preferences")) {
                val prefsObj = payload.getJSONObject("preferences")
                if (prefsObj.has("theme_mode")) storage.setThemeMode(prefsObj.getString("theme_mode"))
                if (prefsObj.has("contrast_level")) storage.setContrastLevel(prefsObj.getString("contrast_level"))
                if (prefsObj.has("desktop_mode_default")) storage.setDesktopModeDefault(prefsObj.getBoolean("desktop_mode_default"))
                if (prefsObj.has("do_not_track")) storage.setDoNotTrackEnabled(prefsObj.getBoolean("do_not_track"))
                if (prefsObj.has("content_blocking_enabled")) storage.setContentBlockingEnabled(prefsObj.getBoolean("content_blocking_enabled"))
                if (prefsObj.has("enable_sso")) storage.setSsoEnabled(prefsObj.getBoolean("enable_sso"))
                if (prefsObj.has("shared_personal_cookies")) storage.setSharedPersonalCookiesEnabled(prefsObj.getBoolean("shared_personal_cookies"))

                if (prefsObj.has("session_content_blocking")) {
                    val cbObj = prefsObj.getJSONObject("session_content_blocking")
                    cbObj.keys().forEach { sid ->
                        storage.setSessionContentBlockingEnabled(sid, cbObj.getBoolean(sid))
                    }
                }
            }

            // 2. Restore Security & Locks
            if (options.restoreSecurity) {
                // Check v2 security object
                if (payload.has("security")) {
                    val secObj = payload.getJSONObject("security")
                    if (secObj.has("master_recovery_key")) {
                        storage.setMasterRecoveryKey(secObj.getString("master_recovery_key"))
                    }
                    if (secObj.has("session_pins")) {
                        val pinsObj = secObj.getJSONObject("session_pins")
                        pinsObj.keys().forEach { sid ->
                            storage.setSessionPin(sid, pinsObj.getString(sid))
                        }
                    }
                }

                // Also check v1 legacy location inside preferences
                if (payload.has("preferences")) {
                    val prefsObj = payload.getJSONObject("preferences")
                    if (prefsObj.has("master_recovery_key")) {
                        storage.setMasterRecoveryKey(prefsObj.getString("master_recovery_key"))
                    }
                    if (prefsObj.has("session_pins")) {
                        val pinsObj = prefsObj.getJSONObject("session_pins")
                        pinsObj.keys().forEach { sid ->
                            storage.setSessionPin(sid, pinsObj.getString(sid))
                        }
                    }
                }
            }

            // 3. Restore Logos & Custom Icons FIRST so images exist on disk for services
            if (payload.has("logos_zip")) {
                val logosZipBase64 = payload.getString("logos_zip")
                if (logosZipBase64.isNotBlank()) {
                    try {
                        val zipBytes = Base64.getDecoder().decode(logosZipBase64)
                        unzipDirectory(zipBytes, context.filesDir)
                        Log.i(TAG, "Successfully restored logos and custom icons from backup")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed unpacking logos from backup", e)
                    }
                }
            }

            // 4. Restore Services with normalized icon paths across devices/user profiles
            var restoredServiceCount = 0
            if (options.restoreServices && payload.has("services")) {
                val servicesArr = payload.getJSONArray("services")
                for (i in 0 until servicesArr.length()) {
                    val sObj = servicesArr.getJSONObject(i)
                    val rawIconPath = if (sObj.has("iconPath")) sObj.getString("iconPath") else null
                    val normalizedIconPath = if (!rawIconPath.isNullOrBlank()) {
                        if (rawIconPath.startsWith("http://", ignoreCase = true) || rawIconPath.startsWith("https://", ignoreCase = true)) {
                            rawIconPath
                        } else {
                            val fileName = File(rawIconPath).name
                            if (rawIconPath.contains("/logos/")) {
                                File(File(context.filesDir, "logos"), fileName).absolutePath
                            } else {
                                File(context.filesDir, fileName).absolutePath
                            }
                        }
                    } else {
                        val cachedFile = File(File(context.filesDir, "logos"), "${sObj.getString("id").lowercase()}.png")
                        if (cachedFile.exists()) cachedFile.absolutePath else null
                    }

                    val service = AIService(
                        id = sObj.getString("id"),
                        name = sObj.getString("name"),
                        url = sObj.getString("url"),
                        faviconUrl = sObj.getString("faviconUrl"),
                        iconPath = normalizedIconPath,
                        customUserAgent = if (sObj.has("customUserAgent")) sObj.getString("customUserAgent") else null,
                        widgetSessionId = if (sObj.has("widgetSessionId")) sObj.getString("widgetSessionId") else null,
                        notificationsEnabled = sObj.optBoolean("notificationsEnabled", true),
                        sortOrder = sObj.optInt("sortOrder", 0),
                        createdAt = sObj.optLong("createdAt", System.currentTimeMillis())
                    )
                    db.serviceDao().insertService(service)
                    restoredServiceCount++
                }
            }

            // 5. Restore Sessions
            var restoredSessionCount = 0
            if (options.restoreServices && payload.has("sessions")) {
                val sessionsArr = payload.getJSONArray("sessions")
                for (i in 0 until sessionsArr.length()) {
                    val sObj = sessionsArr.getJSONObject(i)
                    val session = Session(
                        id = sObj.getString("id"),
                        serviceId = sObj.getString("serviceId"),
                        accountName = sObj.getString("accountName"),
                        isDefault = sObj.optBoolean("isDefault", false),
                        lastAccessed = sObj.optLong("lastAccessed", System.currentTimeMillis()),
                        cookieStorePath = if (sObj.has("cookieStorePath")) sObj.getString("cookieStorePath") else null,
                        notificationsEnabled = sObj.optBoolean("notificationsEnabled", true),
                        themeMode = if (sObj.has("themeMode")) sObj.getString("themeMode") else null,
                        customColors = if (sObj.has("customColors")) sObj.getString("customColors") else null,
                        createdAt = sObj.optLong("createdAt", System.currentTimeMillis())
                    )
                    db.sessionDao().insertSession(session)
                    if (sObj.has("geckoSessionState")) {
                        storage.saveSessionState(session.id, sObj.getString("geckoSessionState"))
                    }
                    restoredSessionCount++
                }
            }

            // 6. Restore Web Logins & Cookies (Gecko Profile)
            if (options.restoreLogins && payload.has("mozilla_profile_zip")) {
                val zipBase64 = payload.getString("mozilla_profile_zip")
                if (zipBase64.isNotBlank()) {
                    try {
                        val zipBytes = Base64.getDecoder().decode(zipBase64)
                        val mozillaDir = File(context.filesDir, "mozilla")
                        if (!mozillaDir.exists()) mozillaDir.mkdirs()

                        // Determine existing active default profile folder name on this device if any
                        val existingProfilesIni = File(mozillaDir, "profiles.ini")
                        val activeProfileName = if (existingProfilesIni.exists()) {
                            parseDefaultProfilePath(existingProfilesIni)
                        } else null

                        val activeProfileDir = if (activeProfileName != null && File(mozillaDir, activeProfileName).exists()) {
                            File(mozillaDir, activeProfileName)
                        } else {
                            mozillaDir.listFiles()?.firstOrNull { it.isDirectory && it.name.endsWith(".default") }
                        }

                        // Stage extraction in temporary cache directory to avoid any locked files or collisions
                        val stagingDir = File(context.cacheDir, "mozilla_stage_${System.currentTimeMillis()}")
                        stagingDir.mkdirs()
                        try {
                            unzipDirectory(zipBytes, stagingDir)

                            // Find the backed up default profile folder in staging
                            val stagedProfilesIni = File(stagingDir, "profiles.ini")
                            val stagedProfileName = if (stagedProfilesIni.exists()) {
                                parseDefaultProfilePath(stagedProfilesIni)
                            } else null

                            val sourceProfileDir = if (stagedProfileName != null && File(stagingDir, stagedProfileName).exists()) {
                                File(stagingDir, stagedProfileName)
                            } else {
                                stagingDir.listFiles()?.firstOrNull { it.isDirectory && it.name.endsWith(".default") }
                            }

                            if (sourceProfileDir != null && sourceProfileDir.exists()) {
                                // 1. Target the active profile directory on this device, or create target directory
                                val targetDir = activeProfileDir ?: File(mozillaDir, sourceProfileDir.name).also { it.mkdirs() }

                                // Delete WAL / SHM and lock files in target directory before copying to prevent SQLite journal replay
                                listOf(
                                    "cookies.sqlite-wal", "cookies.sqlite-shm",
                                    "storage.sqlite-wal", "storage.sqlite-shm",
                                    "webappsstore.sqlite-wal", "webappsstore.sqlite-shm",
                                    "formhistory.sqlite-wal", "formhistory.sqlite-shm",
                                    "permissions.sqlite-wal", "permissions.sqlite-shm",
                                    "lock", ".parentlock", "parent.lock"
                                ).forEach { fileName ->
                                    val f = File(targetDir, fileName)
                                    if (f.exists()) f.delete()
                                }

                                sourceProfileDir.copyRecursively(targetDir, overwrite = true)
                                cleanupLockFiles(targetDir)

                                // Also ensure profiles.ini exists and points to the target profile
                                val profilesIni = File(mozillaDir, "profiles.ini")
                                val iniContent = "[General]\nStartWithLastProfile=1\nVersion=2\n\n[Profile0]\nName=default\nIsRelative=1\nPath=${targetDir.name}\nDefault=1\n"
                                profilesIni.writeText(iniContent)

                                cleanupLockFiles(mozillaDir)
                                Log.i(TAG, "Successfully restored Gecko profile files into ${targetDir.name}")
                            } else {
                                Log.w(TAG, "No valid .default profile found in backup zip staging")
                            }
                        } finally {
                            stagingDir.deleteRecursively()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed unpacking mozilla profile from backup", e)
                    }
                }
            }

            Log.i(TAG, "Successfully restored backup with options: $options")
            Result.success(restoredSessionCount)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to restore backup", e)
            Result.failure(e)
        }
    }

    private val EXCLUDED_PROFILE_NAMES = setOf(
        "lock", "parent.lock", ".parentlock",
        "cache2", "cache", "thumbnails", "startupCache", "shader-cache",
        "safebrowsing", "security_state",
        "minidumps", "crashes", "Crash Reports", "Pending Pings",
        "saved-telemetry-pings", "jumpListCache",
        "datareporting", "gmp-gmpopenh264", "weave", "features",
        "sessionstore-backups", "AlternateDataStorage", "serviceworker.txt"
    )

    private fun zipDirectory(sourceDir: File, out: ZipOutputStream, basePath: String) {
        val files = sourceDir.listFiles() ?: return
        for (file in files) {
            val name = file.name
            try {
                if (java.nio.file.Files.isSymbolicLink(file.toPath())) {
                    continue
                }
            } catch (_: Exception) {
                continue
            }
            if (EXCLUDED_PROFILE_NAMES.contains(name) || name.endsWith(".lock") || name.endsWith(".tmp") || name.endsWith(".bak")) {
                continue
            }
            // Skip large media/blob files (> 10MB) - auth databases are tiny (< 5MB)
            if (file.isFile && file.length() > 10 * 1024 * 1024) {
                Log.d(TAG, "Skipping oversized file in backup: $name (${file.length()} bytes)")
                continue
            }
            val entryPath = if (basePath.isEmpty()) name else "$basePath/$name"
            if (entryPath.contains("storage/permanent/chrome") ||
                entryPath.contains("storage/temporary") ||
                entryPath.contains("/cache/") ||
                entryPath.endsWith("/cache") ||
                entryPath.contains("CacheStorage")
            ) {
                continue
            }
            if (file.isDirectory) {
                try {
                    out.putNextEntry(ZipEntry("$entryPath/"))
                    out.closeEntry()
                    zipDirectory(file, out, entryPath)
                } catch (e: Exception) {
                    Log.w(TAG, "Error writing zip directory entry: $entryPath", e)
                }
            } else {
                try {
                    out.putNextEntry(ZipEntry(entryPath))
                    file.inputStream().buffered().use { input -> input.copyTo(out) }
                    out.closeEntry()
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping unreadable file during zip: $entryPath", e)
                    try { out.closeEntry() } catch (_: Exception) {}
                }
            }
        }
    }

    private fun unzipDirectory(zipBytes: ByteArray, targetDir: File) {
        if (!targetDir.exists()) targetDir.mkdirs()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                // Prevent Zip Slip vulnerability (strictly check against target directory with separator)
                val canonicalTarget = targetDir.canonicalPath.let { if (it.endsWith(File.separator)) it else it + File.separator }
                if (!outFile.canonicalPath.startsWith(canonicalTarget)) {
                    Log.w(TAG, "Blocked Zip Slip attempt: ${entry.name}")
                    entry = zis.nextEntry
                    continue
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun writeOptimizedIcon(file: File, zos: ZipOutputStream, entryName: String) {
        try {
            if (file.length() > 256 * 1024) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val maxDim = maxOf(bitmap.width, bitmap.height)
                    val scaledBitmap = if (maxDim > 512) {
                        val scale = 512f / maxDim
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt().coerceAtLeast(1),
                            (bitmap.height * scale).toInt().coerceAtLeast(1),
                            true
                        )
                    } else bitmap

                    zos.putNextEntry(ZipEntry(entryName))
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, zos)
                    zos.closeEntry()
                    if (scaledBitmap != bitmap) scaledBitmap.recycle()
                    bitmap.recycle()
                    return
                }
            }
            zos.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { input -> input.copyTo(zos) }
            zos.closeEntry()
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing icon entry $entryName", e)
            try { zos.closeEntry() } catch (_: Exception) {}
        }
    }

    private fun parseDefaultProfilePath(profilesIni: File): String? {
        return try {
            val lines = profilesIni.readLines()
            var currentPath: String? = null
            var isDefault = false
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("[Profile")) {
                    if (isDefault && currentPath != null) return currentPath
                    currentPath = null
                    isDefault = false
                } else if (trimmed.startsWith("Path=")) {
                    currentPath = trimmed.substringAfter("Path=").trim()
                } else if (trimmed == "Default=1") {
                    isDefault = true
                }
            }
            if (isDefault && currentPath != null) currentPath else currentPath
        } catch (_: Exception) {
            null
        }
    }

    private fun updateDefaultProfilePath(profilesIni: File, newPath: String) {
        try {
            val lines = profilesIni.readLines()
            val newLines = lines.map { line ->
                if (line.trim().startsWith("Path=")) {
                    "Path=$newPath"
                } else line
            }
            profilesIni.writeText(newLines.joinToString("\n"))
        } catch (e: Exception) {
            Log.w(TAG, "Failed updating profiles.ini with new path: $newPath", e)
        }
    }

    private fun cleanupLockFiles(dir: File) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) {
                cleanupLockFiles(f)
            } else if (f.name == "parent.lock" || f.name == ".parentlock" || f.name == "lock" || f.name.endsWith(".lock")) {
                try {
                    // Delete lock files and broken/stale lock symlinks
                    f.delete()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Encrypts plaintext string with user password using PBKDF2 (100k iterations) and AES-256-GCM.
     */
    fun encryptString(plaintext: String, password: String): String {
        val cleanPassword = password.trim()
        require(cleanPassword.isNotBlank()) { "Backup password cannot be empty" }
        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { random.nextBytes(it) }
        val secretKey = deriveKey(cleanPassword, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintextBytes)

        val envelope = JSONObject().apply {
            put("format", BACKUP_FORMAT)
            put("salt", Base64.getEncoder().encodeToString(salt))
            put("iv", Base64.getEncoder().encodeToString(iv))
            put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
        }
        return envelope.toString()
    }

    /**
     * Decrypts JSON envelope string using user password and validates HMAC / GCM authentication tag.
     * Evaluates whitespace variations to safeguard against accidental mobile keyboard spacing.
     */
    fun decryptString(envelopeJson: String, password: String): String {
        val cleanPassword = password.trim()
        require(cleanPassword.isNotBlank()) { "Password cannot be empty" }
        val envelope = JSONObject(envelopeJson)
        val format = envelope.optString("format", "")
        if (format != BACKUP_FORMAT) {
            throw IllegalArgumentException("Unsupported or corrupt backup file format: $format")
        }

        val salt = Base64.getDecoder().decode(envelope.getString("salt"))
        val iv = Base64.getDecoder().decode(envelope.getString("iv"))
        val ciphertext = Base64.getDecoder().decode(envelope.getString("ciphertext"))

        // Candidate passwords to tolerate accidental trailing/leading whitespace from mobile keyboards
        val passwordCandidates = linkedSetOf(
            password,
            cleanPassword,
            "$cleanPassword ",
            " $cleanPassword"
        )

        var lastException: Exception? = null
        for (candidate in passwordCandidates) {
            try {
                val secretKey = deriveKey(candidate, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

                val plaintextBytes = cipher.doFinal(ciphertext)
                return String(plaintextBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw lastException ?: GeneralSecurityException("Failed to decrypt backup with provided password")
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
