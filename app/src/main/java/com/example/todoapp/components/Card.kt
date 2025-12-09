package com.example.todoapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.enumsAndItems.TodoItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun Card(
    todoItem: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    isDragging: Boolean = false,
    syncTag: String? = null
) {
    val backgroundColor = if (isDragging) Color(0xFF3A3A4A) else Color(0xFF2C2C2C)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todoItem.isCompleted,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF03DAC6),
                    uncheckedColor = Color.Gray,
                    checkmarkColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = todoItem.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (todoItem.isCompleted) Color.Gray else Color.White,
                        textDecoration = if (todoItem.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (syncTag != null) {
                        Text(
                            text = syncTag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (syncTag == "線上") Color(0xFF4CAF50) else Color(0xFFFFA726),
                            modifier = Modifier
                                .background(
                                    color = if (syncTag == "線上")
                                        Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    else
                                        Color(0xFFFFA726).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (todoItem.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = todoItem.description,
                        fontSize = 14.sp,
                        color = if (todoItem.isCompleted) Color.Gray else Color.LightGray,
                        textDecoration = if (todoItem.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (todoItem.notificationTimes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    val timeText = if (todoItem.notificationTimes.size == 1) {
                        dateFormat.format(Date(todoItem.notificationTimes[0]))
                    } else {
                        "${dateFormat.format(Date(todoItem.notificationTimes.minOrNull() ?: 0))} +${todoItem.notificationTimes.size - 1}"
                    }

                    Text(
                        text = "🔔 $timeText",
                        fontSize = 12.sp,
                        color = Color(0xFF03DAC6),
                        modifier = Modifier
                            .background(
                                Color(0xFF03DAC6).copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}