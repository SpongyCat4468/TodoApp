package com.example.todoapp

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.components.Card
import com.example.todoapp.components.DividerWithText
import com.example.todoapp.components.EmptyText
import com.example.todoapp.dialogs.EditTodoDialog
import com.example.todoapp.dialogs.SettingsDialog
import com.example.todoapp.dialogs.TodoDialog
import com.example.todoapp.notification.NotificationHelper
import kotlin.collections.plus

fun sortTodoList(list: List<TodoItem>, sortOption: SortOption): List<TodoItem> {
    return when (sortOption) {
        SortOption.DEFAULT -> list
        SortOption.DATE_ASCENDING -> list.sortedBy {
            it.notificationTimes.minOrNull() ?: Long.MAX_VALUE
        }
        SortOption.DATE_DESCENDING -> list.sortedByDescending {
            it.notificationTimes.minOrNull() ?: Long.MIN_VALUE
        }
        SortOption.TITLE_AZ -> list.sortedBy { it.title.lowercase() }
        SortOption.TITLE_ZA -> list.sortedByDescending { it.title.lowercase() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(context: Context) {
    var todoItems by remember {
        mutableStateOf(loadTodoItems(context))
    }

    LaunchedEffect(todoItems) {
        saveTodoItems(context, todoItems)
    }

    var unfinishedItems by remember { mutableStateOf(todoItems.filter { !it.isCompleted }) }
    var finishedItems by remember { mutableStateOf(todoItems.filter { it.isCompleted }) }

    LaunchedEffect(todoItems) {
        unfinishedItems = todoItems.filter { !it.isCompleted }
        finishedItems = todoItems.filter { it.isCompleted }
    }

    var showDialog by remember { mutableStateOf(false) }
    var selectedTodoItem by remember { mutableStateOf<TodoItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var currentSortOption by remember { mutableStateOf(SortOption.DEFAULT) }

    // Apply sorting to the lists
    val displayUnfinished = sortTodoList(unfinishedItems, currentSortOption)
    val displayFinished = sortTodoList(finishedItems, currentSortOption)

    // Move item functions
    fun moveUnfinishedItem(fromIndex: Int, direction: Int) {
        if (currentSortOption != SortOption.DEFAULT) return
        val toIndex = fromIndex + direction
        if (toIndex < 0 || toIndex >= unfinishedItems.size) return

        val mutableList = unfinishedItems.toMutableList()
        val item = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, item)
        unfinishedItems = mutableList
        todoItems = unfinishedItems + finishedItems
    }

    fun moveFinishedItem(fromIndex: Int, direction: Int) {
        if (currentSortOption != SortOption.DEFAULT) return
        val toIndex = fromIndex + direction
        if (toIndex < 0 || toIndex >= finishedItems.size) return

        val mutableList = finishedItems.toMutableList()
        val item = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, item)
        finishedItems = mutableList
        todoItems = unfinishedItems + finishedItems
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "待辦",
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showSortDialog = true }
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                            contentDescription = "Sort"
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true }
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_preferences),
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF41414E),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_add),
                    contentDescription = "Add"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            itemsIndexed(
                items = displayUnfinished,
                key = { _, item -> item.id }
            ) { index, item ->
                Card(
                    todoItem = item,
                    onCheckedChange = { isChecked ->
                        todoItems = todoItems.map {
                            if (it.id == item.id) {
                                if (isChecked) {
                                    NotificationHelper.cancelAllNotifications(context, it)
                                }
                                it.copy(isCompleted = isChecked)
                            } else it
                        }
                    },
                    onClick = {
                        selectedTodoItem = item
                        showEditDialog = true
                    },
                    showDragHandle = currentSortOption == SortOption.DEFAULT,
                    onMoveUp = if (index > 0) {
                        { moveUnfinishedItem(index, -1) }
                    } else null,
                    onMoveDown = if (index < displayUnfinished.size - 1) {
                        { moveUnfinishedItem(index, 1) }
                    } else null
                )
            }

            item {
                DividerWithText("已完成")
            }

            if (displayFinished.isEmpty()) {
                item {
                    EmptyText()
                }
            } else {
                itemsIndexed(
                    items = displayFinished,
                    key = { _, item -> item.id }
                ) { index, item ->
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
                        },
                        showDragHandle = currentSortOption == SortOption.DEFAULT,
                        onMoveUp = if (index > 0) {
                            { moveFinishedItem(index, -1) }
                        } else null,
                        onMoveDown = if (index < displayFinished.size - 1) {
                            { moveFinishedItem(index, 1) }
                        } else null
                    )
                }
            }
        }

        if (showDialog) {
            TodoDialog(
                onDismiss = { showDialog = false },
                onConfirm = { title: String, description: String, notificationTimes: List<Long> ->
                    val newId = (todoItems.maxOfOrNull { it.id } ?: 0) + 1
                    val newItem = TodoItem(
                        title = title,
                        description = description,
                        id = newId,
                        notificationTimes = notificationTimes
                    )
                    todoItems = todoItems + newItem

                    if (notificationTimes.isNotEmpty()) {
                        NotificationHelper.scheduleNotifications(context, newItem, isMuted)
                    }

                    showDialog = false
                }
            )
        }

        if (showSortDialog) {
            AlertDialog(
                onDismissRequest = { showSortDialog = false },
                title = {
                    Text(
                        text = "排序方式",
                        color = Color.White
                    )
                },
                text = {
                    Column {
                        SortOptionItem(
                            text = "預設順序",
                            isSelected = currentSortOption == SortOption.DEFAULT,
                            onClick = {
                                currentSortOption = SortOption.DEFAULT
                                showSortDialog = false
                            }
                        )
                        SortOptionItem(
                            text = "日期 (最晚到最早)",
                            isSelected = currentSortOption == SortOption.DATE_DESCENDING,
                            onClick = {
                                currentSortOption = SortOption.DATE_DESCENDING
                                showSortDialog = false
                            }
                        )
                        SortOptionItem(
                            text = "日期 (最早到最晚)",
                            isSelected = currentSortOption == SortOption.DATE_ASCENDING,
                            onClick = {
                                currentSortOption = SortOption.DATE_ASCENDING
                                showSortDialog = false
                            }
                        )
                        SortOptionItem(
                            text = "標題 (A-Z)",
                            isSelected = currentSortOption == SortOption.TITLE_AZ,
                            onClick = {
                                currentSortOption = SortOption.TITLE_AZ
                                showSortDialog = false
                            }
                        )
                        SortOptionItem(
                            text = "標題 (Z-A)",
                            isSelected = currentSortOption == SortOption.TITLE_ZA,
                            onClick = {
                                currentSortOption = SortOption.TITLE_ZA
                                showSortDialog = false
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showSortDialog = false }
                    ) {
                        Text(
                            text = "取消",
                            color = Color(0xFF03DAC6)
                        )
                    }
                },
                containerColor = Color(0xFF2C2C2C)
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                isMuted = isMuted,
                onDismiss = { showSettingsDialog = false },
                onMuteChanged = { newMuteState ->
                    isMuted = newMuteState
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
                    NotificationHelper.cancelAllNotifications(context, updatedItem)

                    todoItems = todoItems.map {
                        if (it.id == updatedItem.id) updatedItem else it
                    }

                    if (updatedItem.notificationTimes.isNotEmpty()) {
                        NotificationHelper.scheduleNotifications(context, updatedItem, isMuted)
                    }

                    showEditDialog = false
                    selectedTodoItem = null
                },
                onDelete = { itemToDelete ->
                    NotificationHelper.cancelAllNotifications(context, itemToDelete)

                    todoItems = todoItems.filter { it.id != itemToDelete.id }

                    showEditDialog = false
                    selectedTodoItem = null
                }
            )
        }
    }
}

@Composable
fun SortOptionItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF6200EE),
                unselectedColor = Color.Gray
            )
        )
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 16.sp
        )
    }
}