package com.ujwal.colai

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetLaunchIntent

class ServiceWidgetMediumProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val isDarkMode = (context.resources.configuration.uiMode and 
                          android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                          android.content.res.Configuration.UI_MODE_NIGHT_YES

        val layoutId = if (isDarkMode) R.layout.service_widget_medium_layout 
                       else R.layout.service_widget_medium_layout_light

        val views = RemoteViews(context.packageName, layoutId)

        val intent = HomeWidgetLaunchIntent.getActivity(
            context,
            MainActivity::class.java,
            Uri.parse("colai://widget/click")
        )
        
        views.setOnClickPendingIntent(R.id.widget_container, intent)
        views.setOnClickPendingIntent(R.id.widget_icon, intent)
        try {
            views.setOnClickPendingIntent(R.id.widget_root, intent)
        } catch (e: Exception) {}

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
