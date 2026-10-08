package com.ujwal.colai.feature.webview

import android.content.Context
import android.net.Uri
import android.util.Log
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import org.mozilla.geckoview.GeckoSession.PermissionDelegate

private const val TAG = "GeckoPromptDelegate"

/**
 * Handles file uploads, image attachments, camera triggers, and Web permissions
 * for containerized [GeckoSession] instances.
 *
 * Essential for modern AI web interactions:
 * - ChatGPT & Claude photo / PDF / document file attachments.
 * - DeepSeek & Grok image uploads.
 * - Voice chat microphone access.
 */
class GeckoPromptDelegate(
    private val context: Context,
    private val onFilePickerRequest: (
        mimeTypes: Array<String>,
        isMultiple: Boolean,
        onResult: (List<Uri>) -> Unit
    ) -> Unit,
    private val onPermissionRequest: (
        permissions: Array<String>,
        onResult: (Boolean) -> Unit
    ) -> Unit
) : PromptDelegate, PermissionDelegate {

    // --- PROMPT DELEGATE (File Pickers, Alerts, Dialogs) ---

    private fun prepareSafeUploadUri(uri: Uri): Uri {
        return try {
            val contentResolver = context.contentResolver
            var displayName = "upload_${System.currentTimeMillis()}.jpg"
            if (uri.scheme == "file") {
                val file = java.io.File(uri.path ?: "")
                if (file.exists() && !file.name.isNullOrBlank()) {
                    displayName = file.name
                }
            } else {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val colIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (colIndex != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(colIndex)
                        if (!name.isNullOrBlank()) {
                            displayName = name
                        }
                    }
                }
            }
            val uploadDir = java.io.File(context.cacheDir, "uploads").apply { mkdirs() }
            val sanitizedName = displayName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val destFile = java.io.File(uploadDir, "${System.currentTimeMillis()}_$sanitizedName")
            val inputStream = if (uri.scheme == "file") {
                val srcFile = java.io.File(uri.path ?: "")
                if (srcFile.exists()) srcFile.inputStream() else null
            } else {
                contentResolver.openInputStream(uri)
            }
            inputStream?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.setReadable(true, false)
            // CRITICAL: Return Uri.fromFile(destFile) with scheme "file".
            // GeckoView's internal FilePrompt.getFile directly resolves "file" scheme
            // to destFile.absolutePath, guaranteeing the sandboxed Gecko process has read access.
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            Log.w(TAG, "Could not prepare cached upload URI for $uri, using original", e)
            uri
        }
    }

    override fun onFilePrompt(
        session: GeckoSession,
        prompt: PromptDelegate.FilePrompt
    ): GeckoResult<PromptDelegate.PromptResponse> {
        val mimeTypes = prompt.mimeTypes ?: arrayOf("*/*")
        val isMultiple = prompt.type == PromptDelegate.FilePrompt.Type.MULTIPLE

        Log.d(TAG, "File prompt received: multiple=$isMultiple, mimeTypes=${mimeTypes.joinToString()}")

        // 1. Check if there are pending shared files waiting to be fulfilled
        val pendingShared = GeckoPromptDelegateRegistry.consumePendingUploadFiles()
        if (!pendingShared.isNullOrEmpty()) {
            Log.d(TAG, "Auto-confirming FilePrompt with ${pendingShared.size} pending shared file(s)")
            try {
                val safeUris = pendingShared.map { prepareSafeUploadUri(it) }
                val response = if (isMultiple) {
                    prompt.confirm(context, safeUris.toTypedArray())
                } else {
                    prompt.confirm(context, safeUris.first())
                }
                return GeckoResult.fromValue(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-confirming pending shared file prompt", e)
            }
        }

        // 2. Otherwise dispatch to user-facing activity file picker launcher
        val result = GeckoResult<PromptDelegate.PromptResponse>()

        try {
            onFilePickerRequest(mimeTypes, isMultiple) { selectedUris ->
                try {
                    val safeUris = selectedUris.map { prepareSafeUploadUri(it) }
                    val response = if (safeUris.isEmpty()) {
                        Log.d(TAG, "File prompt cancelled by user")
                        prompt.dismiss()
                    } else if (isMultiple) {
                        Log.d(TAG, "Confirming multiple files: ${safeUris.size}")
                        prompt.confirm(context, safeUris.toTypedArray())
                    } else {
                        Log.d(TAG, "Confirming single file: ${safeUris.first()}")
                        prompt.confirm(context, safeUris.first())
                    }
                    result.complete(response)
                } catch (e: Exception) {
                    Log.e(TAG, "Error confirming file prompt", e)
                    result.complete(prompt.dismiss())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching file picker request", e)
            result.complete(prompt.dismiss())
        }

        return result
    }

    override fun onAlertPrompt(
        session: GeckoSession,
        prompt: PromptDelegate.AlertPrompt
    ): GeckoResult<PromptDelegate.PromptResponse> {
        Log.d(TAG, "Web Alert: ${prompt.title}")
        return GeckoResult.fromValue(prompt.dismiss())
    }

    override fun onButtonPrompt(
        session: GeckoSession,
        prompt: PromptDelegate.ButtonPrompt
    ): GeckoResult<PromptDelegate.PromptResponse> {
        // Auto-confirm standard confirmations (OK/Confirm)
        return GeckoResult.fromValue(prompt.confirm(PromptDelegate.ButtonPrompt.Type.POSITIVE))
    }

    override fun onChoicePrompt(
        session: GeckoSession,
        prompt: PromptDelegate.ChoicePrompt
    ): GeckoResult<PromptDelegate.PromptResponse> {
        // Fallback for native select dropdowns
        val firstChoice = prompt.choices?.firstOrNull()
        return if (firstChoice != null) {
            GeckoResult.fromValue(prompt.confirm(firstChoice))
        } else {
            GeckoResult.fromValue(prompt.dismiss())
        }
    }

    // --- PERMISSION DELEGATE (Camera, Microphone, Notifications) ---

    override fun onAndroidPermissionsRequest(
        session: GeckoSession,
        permissions: Array<out String>?,
        callback: PermissionDelegate.Callback
    ) {
        if (permissions == null || permissions.isEmpty()) {
            callback.reject()
            return
        }

        Log.d(TAG, "Android permission request: ${permissions.joinToString()}")

        onPermissionRequest(permissions.filterNotNull().toTypedArray()) { granted ->
            if (granted) {
                Log.d(TAG, "Permissions granted by user")
                callback.grant()
            } else {
                Log.d(TAG, "Permissions rejected by user")
                callback.reject()
            }
        }
    }

    override fun onMediaPermissionRequest(
        session: GeckoSession,
        uri: String,
        video: Array<out PermissionDelegate.MediaSource>?,
        audio: Array<out PermissionDelegate.MediaSource>?,
        callback: PermissionDelegate.MediaCallback
    ) {
        Log.d(TAG, "Media permission request for uri: $uri")
        val audioSource = audio?.firstOrNull()
        val videoSource = video?.firstOrNull()

        if (audioSource != null || videoSource != null) {
            callback.grant(videoSource, audioSource)
        } else {
            callback.reject()
        }
    }

    override fun onContentPermissionRequest(
        session: GeckoSession,
        perm: PermissionDelegate.ContentPermission
    ): GeckoResult<Int> {
        Log.d(TAG, "Content permission request type: ${perm.permission} for uri: ${perm.uri}")
        // Enforce principle of least privilege: Never silently grant Geolocation or unknown sensitive permissions.
        return when (perm.permission) {
            PermissionDelegate.PERMISSION_GEOLOCATION -> {
                Log.w(TAG, "Silently denied web geolocation permission for privacy and security: ${perm.uri}")
                GeckoResult.fromValue(PermissionDelegate.ContentPermission.VALUE_DENY)
            }
            PermissionDelegate.PERMISSION_PERSISTENT_STORAGE,
            PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> {
                GeckoResult.fromValue(PermissionDelegate.ContentPermission.VALUE_ALLOW)
            }
            else -> {
                GeckoResult.fromValue(PermissionDelegate.ContentPermission.VALUE_DENY)
            }
        }
    }
}
