package com.ujwal.colai.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.ujwal.colai.MainActivity
import com.ujwal.colai.R
import com.ujwal.colai.core.data.LogoCacheManager
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.security.EncryptedStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Native Android AppWidgetProvider for the Medium Bento (4x2) Home Screen Widget.
 *
 * Provides a rich AI dashboard experience featuring:
 * - Active AI service branding with high-res logo from [LogoCacheManager]
 * - Instant deep link container launch
 * - Quick action buttons for Voice Prompt (`colai://widget/mic`) and Search (`colai://widget/search`)
 * - Bento-grid styling with specular glass borders
 * - Dynamic Dark/Light Theme RemoteViews switching (API 31+)
 */
open class ServiceWidgetMediumProvider : AppWidgetProvider() {

    companion object {
        const val TAG = "ServiceWidgetMediumProvider"

        const val ACTION_WIDGET_CLICK = "com.ujwal.colai.action.WIDGET_CLICK"
        const val ACTION_WIDGET_MIC = "com.ujwal.colai.action.WIDGET_MIC"
        const val ACTION_WIDGET_SEARCH = "com.ujwal.colai.action.WIDGET_SEARCH"
        const val ACTION_REFRESH_WIDGET = "com.ujwal.colai.action.REFRESH_WIDGET"

        const val EXTRA_SERVICE_ID = "extra_service_id"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_ACTION = "extra_action"

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Broadcasts update to all installed Medium (4x2) widgets.
         */
        fun updateAll(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, ServiceWidgetMediumProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    widgetScope.launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val storage = EncryptedStorage.getInstance(context)
                            val widgetServiceId = storage.getWidgetServiceId() ?: storage.getActiveServiceId()
                            val services = db.serviceDao().getAllServicesList()
                            val targetService = if (!widgetServiceId.isNullOrBlank()) {
                                services.find { it.id == widgetServiceId }
                            } else null ?: services.find { it.widgetSessionId != null } ?: services.firstOrNull()

                            for (widgetId in ids) {
                                updateAppWidget(context, appWidgetManager, widgetId, targetService)
                            }
                        } catch (t: Throwable) {
                            Log.e(TAG, "Error updating medium widgets in background", t)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to broadcast medium widget update", e)
            }
        }

        /**
         * Builds [RemoteViews] for the 4x2 medium widget based on theme mode and active [AIService].
         */
        fun buildRemoteViews(context: Context, service: AIService?, isDark: Boolean): RemoteViews {
            val layoutId = if (isDark) {
                R.layout.service_widget_medium_layout
            } else {
                R.layout.service_widget_medium_layout_light
            }
            val views = RemoteViews(context.packageName, layoutId)

            val serviceName = service?.name ?: "ColAI"
            views.setTextViewText(R.id.widget_title, serviceName)
            views.setTextViewText(R.id.widget_text, "Ask $serviceName anything or tap mic to dictate...")

            // Apply cached service logo if present
            var logoApplied = false
            if (service != null) {
                val logoFile = LogoCacheManager(context).getCachedLogoFile(service.id)
                if (logoFile != null && logoFile.exists()) {
                    try {
                        val bitmap = decodeScaledWidgetIcon(logoFile.absolutePath, 128)
                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.widget_icon, bitmap)
                            logoApplied = true
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error decoding medium widget logo for ${service.id}", e)
                    }
                }
            }

            if (!logoApplied) {
                views.setImageViewResource(R.id.widget_icon, R.mipmap.launcher_icon)
            }

            // Resolve effective session ID prioritizing user-selected widget target
            val storage = EncryptedStorage.getInstance(context)
            val effectiveSessionId = if (service != null && service.id == storage.getWidgetServiceId()) {
                storage.getWidgetSessionId() ?: service.widgetSessionId
            } else {
                service?.widgetSessionId
            }

            // PendingIntent for main container click -> opens THAT session of THAT AI provider
            val targetUri = if (!effectiveSessionId.isNullOrBlank()) {
                Uri.parse("colai://widget/service/${service?.id ?: "chatgpt"}/$effectiveSessionId")
            } else {
                Uri.parse("colai://widget/service/${service?.id ?: "chatgpt"}")
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_WIDGET_CLICK
                data = targetUri
                putExtra(EXTRA_SERVICE_ID, service?.id)
                putExtra(EXTRA_SESSION_ID, effectiveSessionId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val containerPendingIntent = PendingIntent.getActivity(
                context,
                2001,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, containerPendingIntent)

            // PendingIntent for textbox click -> opens session with typing status focused
            val textUri = if (!effectiveSessionId.isNullOrBlank()) {
                Uri.parse("colai://widget/input/${service?.id ?: "chatgpt"}/$effectiveSessionId")
            } else {
                Uri.parse("colai://widget/input/${service?.id ?: "chatgpt"}")
            }
            val textIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_WIDGET_CLICK
                data = textUri
                putExtra(EXTRA_ACTION, "input")
                putExtra(EXTRA_SERVICE_ID, service?.id)
                putExtra(EXTRA_SESSION_ID, effectiveSessionId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val textPendingIntent = PendingIntent.getActivity(
                context,
                2004,
                textIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_text, textPendingIntent)

            // PendingIntent for voice/mic action -> opens session and triggers mic
            val micUri = if (!effectiveSessionId.isNullOrBlank()) {
                Uri.parse("colai://widget/mic/${service?.id ?: "chatgpt"}/$effectiveSessionId")
            } else {
                Uri.parse("colai://widget/mic/${service?.id ?: "chatgpt"}")
            }
            val micIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_WIDGET_MIC
                data = micUri
                putExtra(EXTRA_ACTION, "mic")
                putExtra(EXTRA_SERVICE_ID, service?.id)
                putExtra(EXTRA_SESSION_ID, effectiveSessionId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val micPendingIntent = PendingIntent.getActivity(
                context,
                2002,
                micIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_action_mic, micPendingIntent)

            // PendingIntent for search action
            val searchIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_WIDGET_SEARCH
                data = Uri.parse("colai://widget/search")
                putExtra(EXTRA_ACTION, "search")
                putExtra(EXTRA_SERVICE_ID, service?.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val searchPendingIntent = PendingIntent.getActivity(
                context,
                2003,
                searchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_action_search, searchPendingIntent)

            return views
        }

        /**
         * Updates a single widget instance with appropriate light/dark RemoteViews.
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            service: AIService?
        ) {
            try {
                val storage = EncryptedStorage.getInstance(context)
                val isDark = when (storage.getThemeMode()) {
                    "dark" -> true
                    "light" -> false
                    else -> {
                        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                        uiMode == Configuration.UI_MODE_NIGHT_YES
                    }
                }
                val remoteViews = buildRemoteViews(context, service, isDark = isDark)
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            } catch (t: Throwable) {
                Log.e(TAG, "Error updating medium appWidgetId $appWidgetId", t)
            }
        }

        private fun decodeScaledWidgetIcon(filePath: String, maxSizePx: Int = 128): Bitmap? {
            return try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(filePath, options)
                if (options.outWidth <= 0 || options.outHeight <= 0) return null

                var inSampleSize = 1
                while (options.outWidth / (inSampleSize * 2) >= maxSizePx &&
                    options.outHeight / (inSampleSize * 2) >= maxSizePx
                ) {
                    inSampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null
                if (decoded.width > maxSizePx || decoded.height > maxSizePx) {
                    val scaled = Bitmap.createScaledBitmap(decoded, maxSizePx, maxSizePx, true)
                    if (scaled != decoded) decoded.recycle()
                    scaled
                } else {
                    decoded
                }
            } catch (t: Throwable) {
                null
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        if (appWidgetIds.isEmpty()) return

        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val storage = EncryptedStorage.getInstance(context)
                val widgetServiceId = storage.getWidgetServiceId() ?: storage.getActiveServiceId()
                val services = db.serviceDao().getAllServicesList()

                val targetService = if (!widgetServiceId.isNullOrBlank()) {
                    services.find { it.id == widgetServiceId }
                } else null ?: services.find { it.widgetSessionId != null } ?: services.firstOrNull()

                for (widgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, widgetId, targetService)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing medium widget update in coroutine", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            updateAll(context)
        }
    }
}
