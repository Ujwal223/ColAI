package com.ujwal.colai

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetLaunchIntent

class ServiceWidgetProvider : AppWidgetProvider() {
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
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        val isMedium = minHeight >= 100 // Roughly 2 cells high
        val isDarkMode = (context.resources.configuration.uiMode and 
                          android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                          android.content.res.Configuration.UI_MODE_NIGHT_YES

        val layoutId = when {
            isMedium && isDarkMode -> R.layout.service_widget_medium_layout
            isMedium && !isDarkMode -> R.layout.service_widget_medium_layout_light
            !isMedium && isDarkMode -> R.layout.service_widget_layout
            else -> R.layout.service_widget_layout_light
        }

        val views = RemoteViews(context.packageName, layoutId)

        // Setup click intent to launch the app (Main Container)
        val mainIntent = HomeWidgetLaunchIntent.getActivity(
            context,
            MainActivity::class.java,
            Uri.parse("colai://widget/click")
        )
        views.setOnClickPendingIntent(R.id.widget_container, mainIntent)
        views.setOnClickPendingIntent(R.id.widget_icon, mainIntent)
        
        // Setup Mic Intent
        val micIntent = HomeWidgetLaunchIntent.getActivity(
            context,
            MainActivity::class.java,
            Uri.parse("colai://widget/mic")
        )
        views.setOnClickPendingIntent(R.id.widget_action_mic, micIntent)
 
        // Setup Search Intent
        val searchIntent = HomeWidgetLaunchIntent.getActivity(
            context,
            MainActivity::class.java,
            Uri.parse("colai://widget/search")
        )
        views.setOnClickPendingIntent(R.id.widget_action_search, searchIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
