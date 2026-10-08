package com.ujwal.colai.feature.webview

import android.content.Context
import android.net.Uri

/**
 * Global registry connecting GeckoView's prompt and permission delegates
 * to Android Activity Result Launchers in MainActivity.
 */
object GeckoPromptDelegateRegistry {

    private var filePickerHandler: ((mimeTypes: Array<String>, isMultiple: Boolean, onResult: (List<Uri>) -> Unit) -> Unit)? = null
    private var permissionHandler: ((permissions: Array<String>, onResult: (Boolean) -> Unit) -> Unit)? = null

    @Volatile
    private var pendingUploadFiles: List<Uri>? = null

    var onFilesConsumedListener: (() -> Unit)? = null

    fun register(
        filePicker: (mimeTypes: Array<String>, isMultiple: Boolean, onResult: (List<Uri>) -> Unit) -> Unit,
        permissionRequester: (permissions: Array<String>, onResult: (Boolean) -> Unit) -> Unit
    ) {
        filePickerHandler = filePicker
        permissionHandler = permissionRequester
    }

    fun unregister() {
        filePickerHandler = null
        permissionHandler = null
        pendingUploadFiles = null
        onFilesConsumedListener = null
    }

    fun setPendingUploadFiles(uris: List<Uri>?) {
        pendingUploadFiles = uris
    }

    fun consumePendingUploadFiles(): List<Uri>? {
        val files = pendingUploadFiles
        pendingUploadFiles = null
        if (!files.isNullOrEmpty()) {
            onFilesConsumedListener?.invoke()
        }
        return files
    }

    fun peekPendingUploadFiles(): List<Uri>? = pendingUploadFiles

    fun hasPendingUploadFiles(): Boolean = !pendingUploadFiles.isNullOrEmpty()

    fun createDelegate(context: Context): GeckoPromptDelegate {
        return GeckoPromptDelegate(
            context = context.applicationContext,
            onFilePickerRequest = { mimeTypes, isMultiple, onResult ->
                filePickerHandler?.invoke(mimeTypes, isMultiple, onResult) ?: onResult(emptyList())
            },
            onPermissionRequest = { permissions, onResult ->
                permissionHandler?.invoke(permissions, onResult) ?: onResult(false)
            }
        )
    }
}
