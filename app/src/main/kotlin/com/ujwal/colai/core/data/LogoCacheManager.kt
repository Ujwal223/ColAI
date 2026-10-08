package com.ujwal.colai.core.data

import android.content.Context
import android.util.Log
import com.ujwal.colai.core.database.ServiceDao
import com.ujwal.colai.core.model.AIService
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * High-performance logo caching engine that downloads and persists AI provider
 * icons into app-private storage (`filesDir/logos/`).
 *
 * Ensures offline display and instant bitmap decoding without network dependency.
 */
class LogoCacheManager(
    private val context: Context,
    private val serviceDao: ServiceDao? = null,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private const val TAG = "LogoCacheManager"
        private const val LOGOS_DIR_NAME = "logos"
        private const val CONNECT_TIMEOUT_MS = 6000
        private const val READ_TIMEOUT_MS = 8000
        private const val MAX_LOGO_BYTES = 2 * 1024 * 1024 // 2MB safety limit
    }

    private val logosDir: File by lazy {
        File(context.filesDir, LOGOS_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Returns the cached local logo file for a given service ID if it exists and is readable.
     */
    fun getCachedLogoFile(serviceId: String): File? {
        val file = File(logosDir, "${serviceId.lowercase()}.png")
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Downloads and caches the logo for a single [AIService].
     * Updates the database entity if a [ServiceDao] is provided.
     *
     * @return Absolute file path to the local cached logo, or null on failure.
     */
    suspend fun cacheLogoForService(service: AIService): String? = withContext(ioDispatcher) {
        val targetFile = File(logosDir, "${service.id.lowercase()}.png")
        if (targetFile.exists() && targetFile.length() > 0) {
            Log.d(TAG, "Logo for ${service.name} already cached at ${targetFile.absolutePath}")
            return@withContext targetFile.absolutePath
        }

        // Determine remote download URL safely
        val downloadUrl = getDownloadUrlForService(service)

        var downloaded = false
        if (!downloadUrl.isNullOrBlank()) {
            try {
                downloaded = downloadToFile(downloadUrl, targetFile)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download logo for ${service.name} from $downloadUrl: ${e.message}")
            }
        }

        // Fallback 1: Google S2 Favicon service
        if (!downloaded || !targetFile.exists() || targetFile.length() == 0L) {
            val googleUrl = getGoogleFaviconUrl(service.url)
            if (!googleUrl.isNullOrBlank()) {
                try {
                    downloaded = downloadToFile(googleUrl, targetFile)
                } catch (_: Exception) {}
            }
        }

        // Fallback 2: Bundled resource drawable for default AI providers
        if (!downloaded || !targetFile.exists() || targetFile.length() == 0L) {
            val defaultRes = DefaultServicesRepository.getDefaultIconResource(service.id)
            if (defaultRes != null) {
                try {
                    val bitmap = BitmapFactory.decodeResource(context.resources, defaultRes)
                    if (bitmap != null) {
                        FileOutputStream(targetFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        downloaded = true
                        Log.i(TAG, "Written bundled drawable to cache for ${service.name}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed writing bundled drawable for ${service.name}", e)
                }
            }
        }

        if (downloaded && targetFile.exists() && targetFile.length() > 0) {
            val absolutePath = targetFile.absolutePath
            serviceDao?.let { dao ->
                val updatedService = service.copy(iconPath = absolutePath)
                dao.updateService(updatedService)
            }
            Log.i(TAG, "Successfully cached logo for ${service.name} -> $absolutePath")
            return@withContext absolutePath
        }

        if (targetFile.exists() && targetFile.length() == 0L) targetFile.delete()
        return@withContext null
    }

    /**
     * Forces a refresh of the logo without destroying existing assets if the network fails.
     * Downloads to a temporary file first before atomically replacing the active logo cache.
     */
    suspend fun forceRefreshLogo(service: AIService): String? = withContext(ioDispatcher) {
        val targetFile = File(logosDir, "${service.id.lowercase()}.png")
        val tempFile = File(logosDir, "${service.id.lowercase()}_tmp.png")

        val downloadUrl = getDownloadUrlForService(service)
        var downloaded = false

        if (!downloadUrl.isNullOrBlank()) {
            try {
                downloaded = downloadToFile(downloadUrl, tempFile)
            } catch (e: Exception) {
                Log.w(TAG, "Direct download failed during refresh for ${service.name}: ${e.message}")
            }
        }

        // Fallback to Google S2 favicon service if primary blocked or failed
        if (!downloaded || !tempFile.exists() || tempFile.length() == 0L) {
            val googleUrl = getGoogleFaviconUrl(service.url)
            if (!googleUrl.isNullOrBlank()) {
                try {
                    downloaded = downloadToFile(googleUrl, tempFile)
                } catch (_: Exception) {}
            }
        }

        if (downloaded && tempFile.exists() && tempFile.length() > 0) {
            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)
            val absolutePath = targetFile.absolutePath
            serviceDao?.let { dao ->
                dao.updateService(service.copy(iconPath = absolutePath))
            }
            Log.i(TAG, "Successfully refreshed logo for ${service.name} -> $absolutePath")
            return@withContext absolutePath
        } else {
            if (tempFile.exists()) tempFile.delete()

            // If network failed and targetFile doesn't exist, try bundled asset
            if (!targetFile.exists() || targetFile.length() == 0L) {
                val defaultRes = DefaultServicesRepository.getDefaultIconResource(service.id)
                if (defaultRes != null) {
                    try {
                        val bitmap = BitmapFactory.decodeResource(context.resources, defaultRes)
                        if (bitmap != null) {
                            FileOutputStream(targetFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            val absolutePath = targetFile.absolutePath
                            serviceDao?.let { dao ->
                                dao.updateService(service.copy(iconPath = absolutePath))
                            }
                            Log.i(TAG, "Restored bundled asset during refresh for ${service.name}")
                            return@withContext absolutePath
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Return existing target file if it was already valid
        return@withContext if (targetFile.exists() && targetFile.length() > 0) targetFile.absolutePath else null
    }

    private fun getDownloadUrlForService(service: AIService): String? {
        return when {
            service.faviconUrl.startsWith("http://", ignoreCase = true) || service.faviconUrl.startsWith("https://", ignoreCase = true) -> {
                service.faviconUrl
            }
            service.iconPath != null && (service.iconPath.startsWith("http://", ignoreCase = true) || service.iconPath.startsWith("https://", ignoreCase = true)) -> {
                service.iconPath
            }
            else -> {
                DefaultServicesRepository.getDefaultIconUrl(service.id)
            }
        }
    }

    private fun getGoogleFaviconUrl(webUrl: String): String? {
        return try {
            val uri = Uri.parse(webUrl)
            val host = uri.host
            if (!host.isNullOrBlank()) {
                "https://www.google.com/s2/favicons?domain=$host&sz=128"
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Downloads and caches logos for all supplied services sequentially,
     * delivering real-time progress callbacks.
     */
    suspend fun cacheAllServices(
        services: List<AIService>,
        onProgress: (current: Int, total: Int, serviceName: String) -> Unit
    ): Map<String, String> = withContext(ioDispatcher) {
        val resultMap = mutableMapOf<String, String>()
        val total = services.size

        services.forEachIndexed { index, service ->
            onProgress(index + 1, total, service.name)
            val path = cacheLogoForService(service)
            if (path != null) {
                resultMap[service.id] = path
            }
        }
        return@withContext resultMap
    }

    /**
     * Clears all cached logo assets from private storage.
     */
    suspend fun clearCache(): Boolean = withContext(ioDispatcher) {
        try {
            logosDir.deleteRecursively().also {
                logosDir.mkdirs()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing logo cache", e)
            false
        }
    }

    private fun downloadToFile(urlString: String, outputFile: File): Boolean {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Android; Mobile; rv:157.0) Gecko/157.0 Firefox/157.0 ColAI/2.0"
                )
                setRequestProperty("Accept", "image/png,image/webp,image/jpeg,image/*;q=0.8")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.w(TAG, "HTTP $responseCode while downloading $urlString")
                return false
            }

            val contentLength = connection.contentLength
            if (contentLength > MAX_LOGO_BYTES) {
                Log.w(TAG, "Logo at $urlString exceeded maximum allowed size ($contentLength bytes)")
                return false
            }

            inputStream = connection.inputStream
            outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalRead += bytesRead
                if (totalRead > MAX_LOGO_BYTES) {
                    Log.w(TAG, "Logo stream exceeded limit during download")
                    outputFile.delete()
                    return false
                }
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Exception downloading from $urlString: ${e.message}")
            if (outputFile.exists()) outputFile.delete()
            false
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }
}
