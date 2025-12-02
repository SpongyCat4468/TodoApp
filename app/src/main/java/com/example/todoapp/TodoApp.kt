package com.example.todoapp

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.todoapp.components.Card
import com.example.todoapp.components.DividerWithText
import com.example.todoapp.components.EmptyText
import com.example.todoapp.dialogs.EditTodoDialog
import com.example.todoapp.dialogs.TodoDialog
import com.example.todoapp.notification.NotificationHelper
import kotlin.collections.plus

@Composable
fun TodoApp(context: Context) {
    var todoItems by remember {
        mutableStateOf(loadTodoItems(context))
    }

    LaunchedEffect(todoItems) {
        saveTodoItems(context, todoItems)
    }

    val unfinished = todoItems.filter { !it.isCompleted }
    val finished = todoItems.filter { it.isCompleted }
    var showDialog by remember { mutableStateOf(false) }
    var selectedTodoItem by remember { mutableStateOf<TodoItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF121212),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Text(
                    text = "+",
                    fontSize = 24.sp
                )
            }
        }
    ) { innerPadding ->

        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(unfinished) { item ->
                Card(
                    todoItem = item,
                    onCheckedChange = { isChecked ->
                        todoItems = todoItems.map {
                            if (it.id == item.id) {
                                if (isChecked) {
                                    NotificationHelper.cancelNotification(context, it.id)
                                }
                                it.copy(isCompleted = isChecked)
                            } else it
                        }
                    },
                    onClick = {
                        selectedTodoItem = item
                        showEditDialog = true
                    }
                )
            }
            item {
                DividerWithText("已完成")
            }
            if (finished.isEmpty()) {
                item {
                    EmptyText()
                }
            } else {
                items(finished) { item ->
                    Card(
                        todoItem = item,
                        onCheckedChange = { isChecked ->
                            todoItems = todoItems.map {
                                if (it.id == item.id) it.copy(isCompleted = isChecked)
                                else it
                            }
                        },
                        onClick = {
                            selectedTodoItem = item
                            showEditDialog = true
                        }
                    )
                }
            }
        }

        if (showDialog) {
            TodoDialog(
                onDismiss = { showDialog = false },
                onConfirm = { title: String, description: String, notificationTime: Long? ->
                    val newId = (todoItems.maxOfOrNull { it.id } ?: 0) + 1
                    val newItem = TodoItem(
                        title = title,
                        description = description,
                        id = newId,
                        notificationTime = notificationTime
                    )
                    todoItems = todoItems + newItem

                    if (notificationTime != null) {
                        NotificationHelper.scheduleNotification(context, newItem)
                    }

                    showDialog = false
                }
            )
        }

        if (showEditDialog && selectedTodoItem != null) {
            EditTodoDialog(
                todoItem = selectedTodoItem!!,
                onDismiss = {
                    showEditDialog = false
                    selectedTodoItem = null
                },
                onSave = { updatedItem ->
                    NotificationHelper.cancelNotification(context, updatedItem.id)

                    todoItems = todoItems.map {
                        if (it.id == updatedItem.id) updatedItem else it
                    }

                    if (updatedItem.notificationTime != null) {
                        NotificationHelper.scheduleNotification(context, updatedItem)
                    }

                    showEditDialog = false
                    selectedTodoItem = null
                },
                onDelete = { itemToDelete ->
                    NotificationHelper.cancelNotification(context, itemToDelete.id)

                    todoItems = todoItems.filter { it.id != itemToDelete.id }

                    showEditDialog = false
                    selectedTodoItem = null
                }
            )
        }
    }
}