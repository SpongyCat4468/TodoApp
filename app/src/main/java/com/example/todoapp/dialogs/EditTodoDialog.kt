package com.example.todoapp.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.TodoItem
import com.example.todoapp.components.formatNotificationTime

@Composable
fun EditTodoDialog(
    todoItem: TodoItem,
    onDismiss: () -> Unit,
    onSave: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit
) {
    var title by remember { mutableStateOf(todoItem.title) }
    var description by remember { mutableStateOf(todoItem.description) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf(todoItem.notificationTime) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "編輯待辦事項",
                color = Color.White
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("標題", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6200EE),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("說明 (非必須)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6200EE),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTime != null) {
                            "提醒時間: ${formatNotificationTime(selectedTime!!)}"
                        } else {
                            "沒有提醒時間"
                        },
                        fontSize = 14.sp,
                        color = Color(0xFFB0B0B0),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { showTimePicker = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EE),
                            contentColor = Color.White
                        )
                    ) {
                        Text("設定時間")
                    }
                }

                if (selectedTime != null) {
                    TextButton(
                        onClick = { selectedTime = null },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF03DAC6)
                        )
                    ) {
                        Text("清除提醒")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delete button
                Button(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCF6679),
                        contentColor = Color.White
                    )
                ) {
                    Text("刪除此待辦事項")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val updatedItem = todoItem.copy(
                            title = title,
                            description = description,
                            notificationTime = selectedTime
                        )
                        onSave(updatedItem)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF424242),
                    disabledContentColor = Color.Gray
                )
            ) {
                Text("儲存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFB0B0B0)
                )
            ) {
                Text("取消")
            }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onTimeSelected = { timeInMillis ->
                selectedTime = timeInMillis
                showTimePicker = false
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
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
                    "確定要刪除「${todoItem.title}」嗎？",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(todoItem)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCF6679),
                        contentColor = Color.White
                    )
                ) {
                    Text("刪除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFB0B0B0)
                    )
                ) {
                    Text("取消")
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}