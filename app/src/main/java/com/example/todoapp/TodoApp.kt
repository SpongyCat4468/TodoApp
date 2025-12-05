package com.example.todoapp

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.example.todoapp.enums.TodoItem
import com.example.todoapp.enums.loadTodoItems
import com.example.todoapp.enums.saveTodoItems
import com.example.todoapp.notification.NotificationHelper
import kotlinx.coroutines.launch

enum class FilterOption {
    ALL,
    COMPLETED,
    UNCOMPLETED
}

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
    val coroutineScope = rememberCoroutineScope()

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
    var showFilterDialog by remember { mutableStateOf(false) }
    var currentSortOption by remember { mutableStateOf(SortOption.DEFAULT) }
    var currentFilterOption by remember { mutableStateOf(FilterOption.ALL) }
    var recurringCount by remember { mutableStateOf(10) }
    var isFabExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    var lastMoveFromTop by remember { mutableStateOf(false) }
    var savedScrollIndex by remember { mutableStateOf(0) }
    var savedScrollOffset by remember { mutableStateOf(0) }

    LaunchedEffect(unfinishedItems, finishedItems, lastMoveFromTop) {
        if (lastMoveFromTop) {
            listState.scrollToItem(savedScrollIndex, savedScrollOffset)
            lastMoveFromTop = false
        }
    }

    val displayUnfinished = sortTodoList(unfinishedItems, currentSortOption)
    val displayFinished = sortTodoList(finishedItems, currentSortOption)

    val unfinishedDragDropState = rememberDragDropState(
        items = displayUnfinished,
        onMove = { fromIndex, toIndex ->
            if (currentSortOption == SortOption.DEFAULT) {
                val mutableList = unfinishedItems.toMutableList()
                val item = mutableList.removeAt(fromIndex)
                mutableList.add(toIndex, item)
                unfinishedItems = mutableList
                todoItems = unfinishedItems + finishedItems

                val lastIndex = displayUnfinished.size - 1
                val shouldScroll = (toIndex == 0 && fromIndex != 0) ||
                        fromIndex == lastIndex ||
                        toIndex == lastIndex

                if (shouldScroll) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(
                            index = toIndex.coerceAtMost(displayUnfinished.size - 1)
                        )
                    }
                } else if (fromIndex == 0) {
                    savedScrollIndex = listState.firstVisibleItemIndex
                    savedScrollOffset = listState.firstVisibleItemScrollOffset
                    lastMoveFromTop = true
                }
            }
        }
    )

    val finishedDragDropState = rememberDragDropState(
        items = displayFinished,
        onMove = { fromIndex, toIndex ->
            if (currentSortOption == SortOption.DEFAULT) {
                val mutableList = finishedItems.toMutableList()
                val item = mutableList.removeAt(fromIndex)
                mutableList.add(toIndex, item)
                finishedItems = mutableList
                todoItems = unfinishedItems + finishedItems

                val lastIndex = displayFinished.size - 1
                val shouldScroll = (toIndex == 0 && fromIndex != 0) ||
                        fromIndex == lastIndex ||
                        toIndex == lastIndex

                if (shouldScroll) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(
                            index = (displayUnfinished.size + 1 + toIndex).coerceAtMost(
                                displayUnfinished.size + displayFinished.size
                            )
                        )
                    }
                } else if (fromIndex == 0) {
                    savedScrollIndex = listState.firstVisibleItemIndex
                    savedScrollOffset = listState.firstVisibleItemScrollOffset
                    lastMoveFromTop = true
                }
            }
        }
    )

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
                            painter = painterResource(R.drawable.sort),
                            tint = Color.Unspecified,
                            contentDescription = "Sort",
                            modifier = Modifier.size(35.dp)
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.settings),
                            tint = Color.Unspecified,
                            contentDescription = "Settings",
                            modifier = Modifier.size(30.dp)
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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                showFilterDialog = true
                                isFabExpanded = false
                            },
                            containerColor = Color.Transparent
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.filter),
                                tint = Color.Unspecified,
                                contentDescription = "Filter",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        FloatingActionButton(
                            onClick = {
                                showDialog = true
                                isFabExpanded = false
                            },
                            containerColor = Color.Transparent
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.tool),
                                tint = Color.Unspecified,
                                contentDescription = "Add",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                val rotation by animateFloatAsState(
                    targetValue = if (isFabExpanded) 45f else 0f,
                    label = "fab_rotation"
                )

                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = Color.Transparent
                ) {
                    Icon(
                        painter = painterResource(R.drawable.toolbar),
                        contentDescription = if (isFabExpanded) "Close" else "Menu",
                        tint = Color.Unspecified,
                        modifier = Modifier.rotate(rotation).size(40.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(innerPadding)
        ) {
            if (currentFilterOption == FilterOption.ALL || currentFilterOption == FilterOption.UNCOMPLETED) {
                items(
                    count = displayUnfinished.size,
                    key = { index -> displayUnfinished[index].id }
                ) { index ->
                    DraggableItem(
                        index = index,
                        item = displayUnfinished[index],
                        dragDropState = unfinishedDragDropState
                    ) { item, isDragging ->
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
                            isDragging = isDragging
                        )
                    }
                }
            }

            // Show divider only when showing all items
            if (currentFilterOption == FilterOption.ALL) {
                item {
                    DividerWithText("已完成")
                }
            }

            // Show finished items based on filter
            if (currentFilterOption == FilterOption.ALL || currentFilterOption == FilterOption.COMPLETED) {
                if (displayFinished.isEmpty()) {
                    item {
                        EmptyText()
                    }
                } else {
                    items(
                        count = displayFinished.size,
                        key = { index -> displayFinished[index].id }
                    ) { index ->
                        DraggableItem(
                            index = index,
                            item = displayFinished[index],
                            dragDropState = finishedDragDropState
                        ) { item, isDragging ->
                            Card(
                                todoItem = item,
                                onCheckedChange = { isChecked ->
                                    todoItems = todoItems.map {
                                        if (it.id == item.id) {
                                            it.copy(isCompleted = isChecked)
                                        } else it
                                    }
                                },
                                onClick = {
                                    selectedTodoItem = item
                                    showEditDialog = true
                                },
                                isDragging = isDragging
                            )
                        }
                    }
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
                },
                recurringCount = recurringCount
            )
        }

        if (showFilterDialog) {
            var showDeleteConfirmation by remember { mutableStateOf(false) }
            var tempFilterOption by remember { mutableStateOf(currentFilterOption) }

            AlertDialog(
                onDismissRequest = {
                    currentFilterOption = FilterOption.ALL
                    showFilterDialog = false
                },
                title = {
                    Text(
                        text = "篩選",
                        color = Color.White
                    )
                },
                text = {
                    Column {
                        FilterOptionItem(
                            text = "全部",
                            isSelected = tempFilterOption == FilterOption.ALL,
                            onClick = {
                                tempFilterOption = FilterOption.ALL
                            }
                        )
                        FilterOptionItem(
                            text = "完成",
                            isSelected = tempFilterOption == FilterOption.COMPLETED,
                            onClick = {
                                tempFilterOption = FilterOption.COMPLETED
                            }
                        )
                        FilterOptionItem(
                            text = "未完成",
                            isSelected = tempFilterOption == FilterOption.UNCOMPLETED,
                            onClick = {
                                tempFilterOption = FilterOption.UNCOMPLETED
                            }
                        )
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                showDeleteConfirmation = true
                            }
                        ) {
                            Text(
                                text = "全選並刪除",
                                color = Color(0xFFCF6679)
                            )
                        }
                        TextButton(
                            onClick = {
                                currentFilterOption = FilterOption.ALL
                                showFilterDialog = false
                            }
                        ) {
                            Text(
                                text = "取消",
                                color = Color(0xFF03DAC6)
                            )
                        }
                        TextButton(
                            onClick = {
                                currentFilterOption = tempFilterOption
                                showFilterDialog = false
                            }
                        ) {
                            Text(
                                text = "套用",
                                color = Color(0xFF03DAC6)
                            )
                        }
                    }
                },
                containerColor = Color(0xFF2C2C2C)
            )

            if (showDeleteConfirmation) {
                val itemsToDelete = when (tempFilterOption) {
                    FilterOption.ALL -> todoItems
                    FilterOption.COMPLETED -> finishedItems
                    FilterOption.UNCOMPLETED -> unfinishedItems
                }

                val categoryText = when (tempFilterOption) {
                    FilterOption.ALL -> "全部"
                    FilterOption.COMPLETED -> "完成"
                    FilterOption.UNCOMPLETED -> "未完成"
                }

                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = {
                        Text(
                            "確認刪除",
                            color = Color.White
                        )
                    },
                    text = {
                        Text(
                            "確定要刪除所有「${categoryText}」的待辦事項嗎？（共 ${itemsToDelete.size} 項）",
                            color = Color.White
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = {
                                itemsToDelete.forEach { item ->
                                    NotificationHelper.cancelAllNotifications(context, item)
                                }

                                todoItems = when (tempFilterOption) {
                                    FilterOption.ALL -> emptyList()
                                    FilterOption.COMPLETED -> unfinishedItems
                                    FilterOption.UNCOMPLETED -> finishedItems
                                }

                                currentFilterOption = FilterOption.ALL
                                showDeleteConfirmation = false
                                showFilterDialog = false
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFCF6679),
                                contentColor = Color.White
                            )
                        ) {
                            Text("刪除")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteConfirmation = false }
                        ) {
                            Text(
                                text = "取消",
                                color = Color(0xFFB0B0B0)
                            )
                        }
                    },
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    textContentColor = Color.White
                )
            }
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
                recurringCount = recurringCount,
                onDismiss = { showSettingsDialog = false },
                onMuteChanged = { newMuteState ->
                    isMuted = newMuteState
                },
                onRecurringCountChanged = { newCount ->
                    recurringCount = newCount
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
                },
                recurringCount = recurringCount
            )
        }
    }
}

@Composable
fun FilterOptionItem(
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