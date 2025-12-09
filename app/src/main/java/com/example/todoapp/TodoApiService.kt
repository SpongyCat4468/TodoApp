package com.example.todoapp

import android.util.Log
import com.example.todoapp.enumsAndItems.TodoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class TodoApiService {
    private val client = OkHttpClient()
    private val baseUrl = "https://todoapi-production-8020.up.railway.app"

    // Fetch all todos for a user
    suspend fun fetchUserTodos(userEmail: String): List<TodoItem> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/todos/$userEmail"
            Log.d("TodoApiService", "=== FETCH TODOS ===")
            Log.d("TodoApiService", "URL: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            Log.d("TodoApiService", "Fetch Response Code: ${response.code}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "[]"
                Log.d("TodoApiService", "Fetch Response Body: $responseBody")
                val items = parseTodoItems(responseBody)
                Log.d("TodoApiService", "Parsed ${items.size} items")
                items
            } else {
                val errorBody = response.body?.string()
                Log.e("TodoApiService", "Failed to fetch todos: ${response.code}")
                Log.e("TodoApiService", "Error body: $errorBody")
                emptyList()
            }
        } catch (e: IOException) {
            Log.e("TodoApiService", "Network error: ${e.message}")
            Log.e("TodoApiService", "Network error details", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error fetching todos: ${e.message}")
            Log.e("TodoApiService", "Error details", e)
            emptyList()
        }
    }

    // Create a new todo
    suspend fun createTodo(userEmail: String, todo: TodoItem): TodoItem? = withContext(Dispatchers.IO) {
        try {
            val jsonObject = JSONObject().apply {
                put("title", todo.title)
                put("description", todo.description)
                put("isCompleted", todo.isCompleted)
                put("notificationTimes", JSONArray(todo.notificationTimes))
            }

            val url = "$baseUrl/todos/?gmail=$userEmail"
            val requestBody = jsonObject.toString()

            Log.d("TodoApiService", "=== CREATE TODO API CALL ===")
            Log.d("TodoApiService", "URL: $url")
            Log.d("TodoApiService", "Request Body: $requestBody")

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d("TodoApiService", "Executing request...")
            val response = client.newCall(request).execute()

            Log.d("TodoApiService", "Response Code: ${response.code}")
            Log.d("TodoApiService", "Response Message: ${response.message}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("TodoApiService", "Response Body: $responseBody")
                Log.d("TodoApiService", "Response Body Length: ${responseBody?.length}")

                if (responseBody == null || responseBody.isEmpty()) {
                    Log.e("TodoApiService", "Response body is null or empty")
                    return@withContext null
                }

                try {
                    val result = parseSingleTodoItem(responseBody)
                    Log.d("TodoApiService", "Parsed result: $result")
                    result
                } catch (e: Exception) {
                    Log.e("TodoApiService", "Error parsing response: ${e.message}", e)
                    Log.e("TodoApiService", "Failed response body was: $responseBody")
                    null
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("TodoApiService", "Failed to create todo: ${response.code}")
                Log.e("TodoApiService", "Error body: $errorBody")
                null
            }
        } catch (e: IOException) {
            Log.e("TodoApiService", "Network error creating todo", e)
            Log.e("TodoApiService", "Error details: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error creating todo", e)
            Log.e("TodoApiService", "Error details: ${e.message}")
            Log.e("TodoApiService", "Stack trace: ", e)
            null
        }
    }

    // Update an existing todo
    suspend fun updateTodo(userEmail: String, todo: TodoItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonObject = JSONObject().apply {
                put("title", todo.title)
                put("description", todo.description)
                put("isCompleted", todo.isCompleted)
                put("notificationTimes", JSONArray(todo.notificationTimes))
            }

            val url = "$baseUrl/todos/$userEmail/${todo.id}"
            Log.d("TodoApiService", "=== UPDATE TODO ===")
            Log.d("TodoApiService", "URL: $url")
            Log.d("TodoApiService", "Request Body: ${jsonObject.toString()}")

            val request = Request.Builder()
                .url(url)
                .put(jsonObject.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            Log.d("TodoApiService", "Update Response Code: ${response.code}")

            if (!response.isSuccessful) {
                Log.e("TodoApiService", "Update failed: ${response.body?.string()}")
            }

            response.isSuccessful
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error updating todo: ${e.message}", e)
            false
        }
    }

    // Toggle todo completion status
    suspend fun toggleTodo(userEmail: String, todoId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/todos/$userEmail/$todoId/toggle")
                .patch("".toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error toggling todo: ${e.message}")
            false
        }
    }

    // Delete a specific todo
    suspend fun deleteTodo(userEmail: String, todoId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/todos/$userEmail/$todoId")
                .delete()
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error deleting todo: ${e.message}")
            false
        }
    }

    // Delete all todos for a user
    suspend fun deleteAllTodos(userEmail: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/todos/$userEmail")
                .delete()
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error deleting all todos: ${e.message}")
            false
        }
    }

    // Search todos by title
    suspend fun searchTodos(userEmail: String, query: String): List<TodoItem> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/todos/$userEmail/search?q=$query")
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "[]"
                parseTodoItems(responseBody)
            } else {
                Log.e("TodoApiService", "Failed to search todos: ${response.code}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error searching todos: ${e.message}")
            emptyList()
        }
    }

    // Get todo statistics
    suspend fun getTodoStats(userEmail: String): TodoStats? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/todos/$userEmail/stats")
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext null
                parseStats(responseBody)
            } else {
                Log.e("TodoApiService", "Failed to fetch stats: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error fetching stats: ${e.message}")
            null
        }
    }

    // Bulk create todos
    suspend fun bulkCreateTodos(userEmail: String, todos: List<TodoItem>): List<TodoItem> = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            todos.forEach { todo ->
                val jsonObject = JSONObject().apply {
                    put("title", todo.title)
                    put("description", todo.description)
                    put("isCompleted", todo.isCompleted)
                    put("notificationTimes", JSONArray(todo.notificationTimes))
                }
                jsonArray.put(jsonObject)
            }

            val url = "$baseUrl/todos/$userEmail/bulk-create"
            val requestBody = jsonArray.toString()

            Log.d("TodoApiService", "=== BULK CREATE TODOS ===")
            Log.d("TodoApiService", "URL: $url")
            Log.d("TodoApiService", "Request Body: $requestBody")
            Log.d("TodoApiService", "Todo count: ${todos.size}")

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            Log.d("TodoApiService", "Bulk create Response Code: ${response.code}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "[]"
                Log.d("TodoApiService", "Bulk create Response Body: $responseBody")
                val items = parseTodoItems(responseBody)
                Log.d("TodoApiService", "Bulk create parsed ${items.size} items")
                items
            } else {
                val errorBody = response.body?.string()
                Log.e("TodoApiService", "Failed to bulk create todos: ${response.code}")
                Log.e("TodoApiService", "Error body: $errorBody")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error bulk creating todos: ${e.message}", e)
            emptyList()
        }
    }

    // Parse multiple todo items from JSON
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

    // Parse a single todo item from JSON
    private fun parseSingleTodoItem(jsonString: String): TodoItem? {
        try {
            Log.d("TodoApiService", "=== PARSING SINGLE TODO ===")
            Log.d("TodoApiService", "JSON String: $jsonString")

            val jsonObject = JSONObject(jsonString)
            Log.d("TodoApiService", "JSON Keys: ${jsonObject.keys().asSequence().toList()}")

            val id = jsonObject.getInt("id")
            val title = jsonObject.getString("title")
            val description = jsonObject.getString("description")
            val isCompleted = jsonObject.getBoolean("isCompleted")

            Log.d("TodoApiService", "Parsed basic fields - id: $id, title: $title, isCompleted: $isCompleted")

            val notificationTimesArray = jsonObject.getJSONArray("notificationTimes")
            val notificationTimes = mutableListOf<Long>()

            for (i in 0 until notificationTimesArray.length()) {
                notificationTimes.add(notificationTimesArray.getLong(i))
            }

            Log.d("TodoApiService", "Notification times count: ${notificationTimes.size}")

            val item = TodoItem(
                id = id,
                title = title,
                description = description,
                isCompleted = isCompleted,
                notificationTimes = notificationTimes
            )

            Log.d("TodoApiService", "Successfully created TodoItem: $item")
            return item
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error parsing single todo: ${e.message}", e)
            Log.e("TodoApiService", "Stack trace:", e)
            return null
        }
    }

    private fun parseStats(jsonString: String): TodoStats? {
        try {
            val jsonObject = JSONObject(jsonString)
            return TodoStats(
                total = jsonObject.getInt("total"),
                completed = jsonObject.getInt("completed"),
                pending = jsonObject.getInt("pending"),
                completionRate = jsonObject.getDouble("completion_rate")
            )
        } catch (e: Exception) {
            Log.e("TodoApiService", "Error parsing stats: ${e.message}")
            return null
        }
    }
}

data class TodoStats(
    val total: Int,
    val completed: Int,
    val pending: Int,
    val completionRate: Double
)