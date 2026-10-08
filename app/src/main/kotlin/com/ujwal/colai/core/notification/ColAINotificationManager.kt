package com.ujwal.colai.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ujwal.colai.MainActivity
import org.mozilla.geckoview.WebNotification

/**
 * Handles incoming web push and desktop notifications delivered by GeckoView
 * from AI providers (ChatGPT, Claude, Perplexity, etc.).
 */
object ColAINotificationManager {
    private const val TAG = "ColAINotification"
    const val CHANNEL_ID = "colai_ai_site_notifications"
    private const val CHANNEL_NAME = "AI Site Notifications"

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from AI providers (ChatGPT, Claude, Perplexity, etc.)"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, webNotification: WebNotification) {
        initChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val tag = webNotification.tag?.takeIf { it.isNotBlank() }
        val title = webNotification.title?.takeIf { it.isNotBlank() } ?: "ColAI Assistant"
        val text = webNotification.text ?: ""

        val notificationId = (tag ?: (title + text)).hashCode()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            webNotification.show()
            Log.i(TAG, "Successfully posted notification ID: $notificationId ($title)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted by user", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification", e)
        }
    }

    fun cancelNotification(context: Context, webNotification: WebNotification) {
        val notificationId = (webNotification.tag?.ifBlank { null } ?: (webNotification.title + webNotification.text)).hashCode()
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling notification", e)
        }
    }
}
