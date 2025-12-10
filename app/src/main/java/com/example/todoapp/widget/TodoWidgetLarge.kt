package com.example.todoapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.todoapp.MainActivity
import com.example.todoapp.R
import com.example.todoapp.enumsAndItems.loadTodoItems

class TodoWidgetLarge : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.todo_widget_large)

            val todoItems = loadTodoItems(context)
            val unfinishedItems = todoItems.filter { !it.isCompleted }

            val titleText = "待辦事項 (${unfinishedItems.size})"
            views.setTextViewText(R.id.widget_title, titleText)

            val todoText = if (unfinishedItems.isEmpty()) {
                "暫無待辦事項 ✓"
            } else {
                unfinishedItems.take(8).joinToString("\n") { "• ${it.title}" }
            }
            views.setTextViewText(R.id.widget_todo_list, todoText)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            val refreshIntent = Intent(context, TodoWidgetLarge::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}