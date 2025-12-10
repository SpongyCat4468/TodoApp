package com.example.todoapp.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object WidgetUpdater {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Update all small widgets
        val smallWidgetComponent = ComponentName(context, TodoWidgetSmall::class.java)
        val smallWidgetIds = appWidgetManager.getAppWidgetIds(smallWidgetComponent)
        for (widgetId in smallWidgetIds) {
            TodoWidgetSmall.updateAppWidget(context, appWidgetManager, widgetId)
        }

        // Update all medium widgets
        val mediumWidgetComponent = ComponentName(context, TodoWidgetMedium::class.java)
        val mediumWidgetIds = appWidgetManager.getAppWidgetIds(mediumWidgetComponent)
        for (widgetId in mediumWidgetIds) {
            TodoWidgetMedium.updateAppWidget(context, appWidgetManager, widgetId)
        }

        // Update all large widgets
        val largeWidgetComponent = ComponentName(context, TodoWidgetLarge::class.java)
        val largeWidgetIds = appWidgetManager.getAppWidgetIds(largeWidgetComponent)
        for (widgetId in largeWidgetIds) {
            TodoWidgetLarge.updateAppWidget(context, appWidgetManager, widgetId)
        }
    }
}