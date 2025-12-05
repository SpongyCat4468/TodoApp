package com.example.todoapp

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.components.Card
import com.example.todoapp.components.DividerWithText
import com.example.todoapp.components.EmptyText
import com.example.todoapp.dialogs.EditTodoDialog
import com.example.todoapp.dialogs.SettingsDialog
import com.example.todoapp.dialogs.TodoDialog
import com.example.todoapp.enumsAndItems.FilterOptionItem
import com.example.todoapp.enumsAndItems.SortOptionItem
import com.example.todoapp.enumsAndItems.TodoItem
import com.example.todoapp.enumsAndItems.loadTodoItems
import com.example.todoapp.enumsAndItems.saveTodoItems
import com.example.todoapp.notification.NotificationHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

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

// API call -> TODO
class TodoApiService {
    private val client = OkHttpClient()
    private val baseUrl = "https://your-api-domain.com/api"

    suspend fun fetchUserTodos(userEmail: String): List<TodoItem> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/todos?email=$userEmail")
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "[]"
                parseTodoItems(responseBody)
            } else {
                Log.e("TodoApiService", "Failed to fetch todos: ${response.code}")
                emptyList()
            }
        } catch (e: IOException) {
            Log.e("TodoApiService", "Network error: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error fetching todos: ${e.message}")
            emptyList()
        }
    }

    suspend fun syncTodoItems(userEmail: String, todos: List<TodoItem>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            todos.forEach { todo ->
                val jsonObject = JSONObject().apply {
                    put("id", todo.id)
                    put("title", todo.title)
                    put("description", todo.description)
                    put("isCompleted", todo.isCompleted)
                    put("notificationTimes", JSONArray(todo.notificationTimes))
                }
                jsonArray.put(jsonObject)
            }

            val requestBody = JSONObject().apply {
                put("email", userEmail)
                put("todos", jsonArray)
            }

            val request = Request.Builder()
                .url("$baseUrl/todos/sync")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error syncing todos: ${e.message}")
            false
        }
    }

    private fun parseTodoItems(jsonString: String): List<TodoItem> {
        try {
            val jsonArray = JSONArray(jsonString)
            val items = mutableListOf<TodoItem>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val notificationTimesArray = jsonObject.getJSONArray("notificationTimes")
                val notificationTimes = mutableListOf<Long>()

                for (j in 0 until notificationTimesArray.length()) {
                    notificationTimes.add(notificationTimesArray.getLong(j))
                }

                items.add(
                    TodoItem(
                        id = jsonObject.getInt("id"),
                        title = jsonObject.getString("title"),
                        description = jsonObject.getString("description"),
                        isCompleted = jsonObject.getBoolean("isCompleted"),
                        notificationTimes = notificationTimes
                    )
                )
            }

            return items
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error parsing todos: ${e.message}")
            return emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(context: Context) {
    val coroutineScope = rememberCoroutineScope()
    val localContext = LocalContext.current
    val apiService = remember { TodoApiService() }

    var todoItems by remember {
        mutableStateOf(loadTodoItems(context))
    }

    var unfinishedItems by remember { mutableStateOf(todoItems.filter { !it.isCompleted }) }
    var finishedItems by remember { mutableStateOf(todoItems.filter { it.isCompleted }) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedTodoItem by remember { mutableStateOf<TodoItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var currentSortOption by remember { mutableStateOf(SortOption.DEFAULT) }
    var currentFilterOption by remember { mutableStateOf(FilterOption.ALL) }
    var recurringCount by remember { mutableStateOf(10) }
    var isFabExpanded by remember { mutableStateOf(false) }
    var userEmail by remember { mutableStateOf<String?>(null) }
    var userPhotoUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    var lastMoveFromTop by remember { mutableStateOf(false) }
    var savedScrollIndex by remember { mutableStateOf(0) }
    var savedScrollOffset by remember { mutableStateOf(0) }

    LaunchedEffect(todoItems) {
        saveTodoItems(context, todoItems)
    }

    LaunchedEffect(todoItems) {
        unfinishedItems = todoItems.filter { !it.isCompleted }
        finishedItems = todoItems.filter { it.isCompleted }
    }

    LaunchedEffect(userEmail) {
        if (userEmail != null) {
            isLoading = true
            try {
                val fetchedTodos = apiService.fetchUserTodos(userEmail!!)
                if (fetchedTodos.isNotEmpty()) {
                    todoItems = fetchedTodos
                    Toast.makeText(localContext, "已載入雲端待辦事項", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("TodoApp", "Error loading todos: ${e.message}")
                Toast.makeText(localContext, "載入待辦事項失敗", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun syncTodos() {
        if (userEmail != null && !isSyncing) {
            coroutineScope.launch {
                isSyncing = true
                try {
                    val success = apiService.syncTodoItems(userEmail!!, todoItems)
                    if (success) {
                        Log.d("TodoApp", "Todos synced successfully")
                        Toast.makeText(localContext, "同步成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("TodoApp", "Failed to sync todos")
                        Toast.makeText(localContext, "同步失敗", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("TodoApp", "Sync error: ${e.message}")
                    Toast.makeText(localContext, "同步錯誤: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isSyncing = false
                }
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                userEmail = account.email
                userPhotoUrl = account.photoUrl?.toString()
                Log.d("TodoApp", "Signed in as: $userEmail")
                Toast.makeText(
                    localContext,
                    "登入成功: ${account.email}",
                    Toast.LENGTH_LONG
                ).show()

                val idToken = account.idToken
                Log.d("TodoApp", "ID Token: $idToken")
            } else {
                Log.e("TodoApp", "Account is null")
                Toast.makeText(localContext, "登入失敗: 無法取得帳戶資訊", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            Log.e("TodoApp", "Sign-in failed with status code: ${e.statusCode}", e)
            val errorMessage = when (e.statusCode) {
                7 -> "網路連線錯誤"
                8 -> "內部錯誤，請稍後再試"
                10 -> "開發者錯誤，請檢查配置"
                12501 -> "登入已取消"
                12502 -> "登入進行中"
                else -> "登入失敗 (錯誤代碼: ${e.statusCode})"
            }
            Toast.makeText(
                localContext,
                errorMessage,
                Toast.LENGTH_LONG
            ).show()
            userEmail = null
        } catch (e: Exception) {
            Log.e("TodoApp", "Unexpected error during sign-in", e)
            Toast.makeText(
                localContext,
                "登入時發生未預期的錯誤",
                Toast.LENGTH_LONG
            ).show()
            userEmail = null
        }
    }

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
                    Column {
                        Text(
                            text = "待辦",
                            fontSize = 20.sp
                        )
                        if (userEmail != null) {
                            Text(
                                text = userEmail!!,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    if (userEmail != null) {
                        IconButton(
                            onClick = { syncTodos() },
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    tint = Color.Unspecified,
                                    contentDescription = "Sync",
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (userEmail == null) {
                                try {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken("944913276122-25tq9gsuijcman0dmq8bo9ui9uno35tf.apps.googleusercontent.com")
                                        .requestEmail()
                                        .build()

                                    val googleSignInClient = GoogleSignIn.getClient(localContext, gso)
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                } catch (e: Exception) {
                                    Log.e("TodoApp", "Error initiating sign-in", e)
                                    Toast.makeText(
                                        localContext,
                                        "無法啟動登入流程",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                showLogoutDialog = true
                            }
                        }
                    ) {
                        if (userEmail == null) {
                            Icon(
                                painter = painterResource(R.drawable.google_signin),
                                tint = Color.Unspecified,
                                contentDescription = "Sign In",
                                modifier = Modifier.size(30.dp)
                            )
                        } else {
                            if (userPhotoUrl != null && userPhotoUrl!!.isNotEmpty()) {
                                AsyncImage(
                                    model = userPhotoUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                        .background(Color.Gray, CircleShape),
                                    contentScale = ContentScale.Crop,
                                    onError = {
                                        Log.e("TodoApp", "Error loading profile image: ${it.result.throwable}")
                                    }
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.google_signout),
                                    tint = Color.Unspecified,
                                    contentDescription = "Sign Out",
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { showSortDialog = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sort),
                            tint = Color.Unspecified,
                            contentDescription = "Sort",
                            modifier = Modifier.size(30.dp)
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
                    targetValue = if (isFabExpanded) 270f else 180f,
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
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White)
                Text(
                    text = "載入中...",
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
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
                                    if (userEmail != null) {
                                        syncTodos()
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

                if (currentFilterOption == FilterOption.ALL) {
                    item {
                        DividerWithText("已完成")
                    }
                }

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
                                        if (userEmail != null) {
                                            syncTodos()
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
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = "確認登出",
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "確定要登出嗎？本地資料將會保留。",
                        color = Color.White
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            try {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .build()
                                val googleSignInClient = GoogleSignIn.getClient(localContext, gso)
                                googleSignInClient.signOut().addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        userEmail = null
                                        userPhotoUrl = null
                                        todoItems = loadTodoItems(context)
                                        Toast.makeText(
                                            localContext,
                                            "已成功登出",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        Log.d("TodoApp", "Successfully signed out")
                                    } else {
                                        Toast.makeText(
                                            localContext,
                                            "登出失敗",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        Log.e("TodoApp", "Sign out failed")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("TodoApp", "Error during sign-out", e)
                                Toast.makeText(
                                    localContext,
                                    "登出時發生錯誤",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            showLogoutDialog = false
                        }
                    ) {
                        Text(
                            text = "登出",
                            color = Color(0xFFCF6679)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLogoutDialog = false }
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

                    if (userEmail != null) {
                        syncTodos()
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

                                if (userEmail != null) {
                                    syncTodos()
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

                    if (userEmail != null) {
                        syncTodos()
                    }

                    showEditDialog = false
                    selectedTodoItem = null
                },
                onDelete = { itemToDelete ->
                    NotificationHelper.cancelAllNotifications(context, itemToDelete)

                    todoItems = todoItems.filter { it.id != itemToDelete.id }

                    if (userEmail != null) {
                        syncTodos()
                    }

                    showEditDialog = false
                    selectedTodoItem = null
                },
                recurringCount = recurringCount
            )
        }
    }
}