package com.example.todoapp

import android.content.Context
import androidx.core.content.edit

data class TodoItem (
    val title: String,
    val description: String,
    var isCompleted: Boolean = false,
    val notificationTime: Long? = null,
    val id: Int = 0
)

fun saveTodoItems(context: Context, items: List<TodoItem>) {
    val sharedPrefs = context.getSharedPreferences("TodoApp", Context.MODE_PRIVATE)
    val json = items.joinToString(separator = "|||") { item ->
        "${item.id}::${item.title}::${item.description}::${item.isCompleted}::${item.notificationTime ?: ""}"
    }
    sharedPrefs.edit { putString("todos", json) }
}

fun loadTodoItems(context: Context): List<TodoItem> {
    val sharedPrefs = context.getSharedPreferences("TodoApp", Context.MODE_PRIVATE)
    val json = sharedPrefs.getString("todos", "") ?: ""
    if (json.isEmpty()) return emptyList()

    return json.split("|||").mapNotNull { item ->
        val parts = item.split("::")
        if (parts.size >= 4) {
            TodoItem(
                title = parts[1],
                description = parts[2],
                isCompleted = parts[3].toBoolean(),
                id = parts[0].toInt(),
                notificationTime = parts.getOrNull(4)?.toLongOrNull()
            )
        } else null
    }
}