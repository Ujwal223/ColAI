package com.ujwal.colai.feature.webview

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.WebResponse
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder

private const val TAG = "GeckoDownloadHandler"

/**
 * Handles seamless downloads for AI generated files, images, PDFs, code exports, and charts.
 *
 * Streams [WebResponse.body] directly to the user's public Downloads directory
 * under a clean "ColAI" subfolder using Android's modern [MediaStore] APIs.
 */
object GeckoDownloadHandler {

    fun handleExternalResponse(context: Context, response: WebResponse) {
        val appContext = context.applicationContext
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        coroutineScope.launch {
            try {
                val uri = response.uri
                val headers = response.headers
                val inputStream = response.body ?: return@launch

                val contentDisposition = headers["Content-Disposition"] ?: headers["content-disposition"]
                val contentType = headers["Content-Type"] ?: headers["content-type"] ?: "application/octet-stream"

                val filename = extractFilename(contentDisposition, uri, contentType)
                Log.i(TAG, "Starting download: $filename from $uri (type: $contentType)")

                val savedUri = saveToDownloads(appContext, inputStream, filename, contentType)

                withContext(Dispatchers.Main) {
                    if (savedUri != null) {
                        Toast.makeText(
                            appContext,
                            "Saved \"$filename\" to Downloads",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(appContext, "Failed to download \"$filename\"", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling external download response", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Download error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sanitizeFilename(name: String): String {
        val baseName = File(name).name
        val sanitized = baseName.replace("[/\\\\?%*:|\"<>]".toRegex(), "_").trim('.')
        return sanitized.ifBlank { "download_${System.currentTimeMillis()}" }
    }

    private fun extractFilename(contentDisposition: String?, uri: String, contentType: String): String {
        if (!contentDisposition.isNullOrBlank()) {
            // Check filename*=UTF-8''...
            val utf8Match = Regex("filename\\*=(?:UTF-8|utf-8)''([^;\\r\\n]+)").find(contentDisposition)
            if (utf8Match != null) {
                try {
                    val decoded = URLDecoder.decode(utf8Match.groupValues[1].trim('"', '\''), Charsets.UTF_8.name())
                    val sanitized = sanitizeFilename(decoded)
                    if (sanitized.isNotBlank()) return sanitized
                } catch (_: Exception) {}
            }

            // Check standard filename="..."
            val standardMatch = Regex("filename=[\"']?([^\"';\\r\\n]+)[\"']?").find(contentDisposition)
            if (standardMatch != null) {
                val candidate = standardMatch.groupValues[1].trim()
                val sanitized = sanitizeFilename(candidate)
                if (sanitized.isNotBlank()) return sanitized
            }
        }

        // Fallback: extract from URI
        val uriPath = try { Uri.parse(uri).lastPathSegment } catch (_: Exception) { null }
        if (!uriPath.isNullOrBlank() && uriPath.contains('.')) {
            val sanitized = sanitizeFilename(uriPath)
            if (sanitized.isNotBlank()) return sanitized
        }

        // Fallback: generate name with extension from MIME type
        val extension = when {
            contentType.contains("pdf") -> ".pdf"
            contentType.contains("png") -> ".png"
            contentType.contains("jpeg") || contentType.contains("jpg") -> ".jpg"
            contentType.contains("webp") -> ".webp"
            contentType.contains("json") -> ".json"
            contentType.contains("csv") -> ".csv"
            contentType.contains("markdown") || contentType.contains("text/plain") -> ".txt"
            contentType.contains("zip") -> ".zip"
            else -> ".bin"
        }

        return "colai_file_${System.currentTimeMillis()}$extension"
    }

    private fun saveToDownloads(
        context: Context,
        input: InputStream,
        filename: String,
        mimeType: String
    ): Uri? {
        val safeFilename = sanitizeFilename(filename)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeFilename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ColAI")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val itemUri = context.contentResolver.insert(collection, values) ?: return null

                context.contentResolver.openOutputStream(itemUri)?.use { out ->
                    input.copyTo(out)
                }

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(itemUri, values, null, null)

                itemUri
            } else {
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ColAI"
                )
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                val targetFile = File(downloadsDir, safeFilename)
                if (!targetFile.canonicalPath.startsWith(downloadsDir.canonicalPath + File.separator)) {
                    Log.e(TAG, "Blocked path traversal download target: $filename")
                    return null
                }
                FileOutputStream(targetFile).use { out ->
                    input.copyTo(out)
                }
                Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file to Downloads", e)
            null
        }
    }
}
