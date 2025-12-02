package com.example.todoapp.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.TodoItem

@Composable
fun Card(todoItem: TodoItem, onCheckedChange: (Boolean) -> Unit, onClick: () -> Unit) {
    val title: String = todoItem.title
    val description: String = todoItem.description

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { onClick() },  // Add clickable
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todoItem.isCompleted,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF6200EE),
                    uncheckedColor = Color.Gray
                )
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    color = Color.White
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color(0xFFB0B0B0),
                        modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                    )
                }
                if (todoItem.notificationTime != null && todoItem.notificationTime > 0) {
                    Text(
                        text = "提醒：" + formatNotificationTime(todoItem.notificationTime),
                        fontSize = 14.sp,
                        color = Color(0xFF03DAC6),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}