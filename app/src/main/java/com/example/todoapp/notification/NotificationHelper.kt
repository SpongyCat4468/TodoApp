package com.example.todoapp.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.todoapp.enums.TodoItem

object NotificationHelper {

    fun scheduleNotifications(
        context: Context,
        todoItem: TodoItem,
        isMuted: Boolean = false
    ) {
        // Don't schedule if muted
        if (isMuted) {
            return
        }

        if (todoItem.notificationTimes.isEmpty()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context, "請在設定中允許精確鬧鐘權限", Toast.LENGTH_LONG).show()
                return
            }
        }

        var scheduledCount = 0
        todoItem.notificationTimes.forEachIndexed { index, notificationTime ->
            if (notificationTime > System.currentTimeMillis()) {
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("TITLE", todoItem.title)
                    putExtra("DESCRIPTION", todoItem.description)
                    putExtra("NOTIFICATION_ID", generateNotificationId(todoItem.id, index))
                }

                // Use unique request code for each notification
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    generateNotificationId(todoItem.id, index),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        notificationTime,
                        pendingIntent
                    )
                    scheduledCount++
                } catch (e: SecurityException) {
                    Toast.makeText(context, "無法設定提醒", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (scheduledCount > 0) {
            val message = if (scheduledCount == 1) "提醒已設定" else "$scheduledCount 個提醒已設定"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelAllNotifications(context: Context, todoItem: TodoItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (index in 0 until 20) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                generateNotificationId(todoItem.id, index),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun generateNotificationId(todoId: Int, timeIndex: Int): Int {
        return todoId * 1000 + timeIndex
    }
}