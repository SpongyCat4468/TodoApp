package com.example.todoapp.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.todoapp.TodoItem

object NotificationHelper {

    fun scheduleNotification(
        context: Context,
        todoItem: TodoItem
    ) {
        if (todoItem.notificationTime == null || todoItem.notificationTime <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check if we have permission to schedule exact alarms (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context, "請在設定中允許精確鬧鐘權限", Toast.LENGTH_LONG).show()
                return
            }
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("TITLE", todoItem.title)
            putExtra("DESCRIPTION", todoItem.description)
            putExtra("NOTIFICATION_ID", todoItem.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todoItem.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                todoItem.notificationTime,
                pendingIntent
            )
            Toast.makeText(context, "提醒已設定", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "無法設定提醒", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelNotification(context: Context, todoId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}