package com.example.todoapp.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.enums.TodoItem

@Composable
fun Card(
    todoItem: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    isDragging: Boolean = false
) {
    val title: String = todoItem.title
    val description: String = todoItem.description

    // Animate scale when checked/unchecked
    val scale by animateFloatAsState(
        targetValue = if (todoItem.isCompleted) 0.95f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "card_scale"
    )

    // Animate opacity when checked/unchecked
    val animatedAlpha by animateFloatAsState(
        targetValue = if (todoItem.isCompleted) 0.4f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "card_alpha"
    )

    // Animate background color
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isDragging -> Color(0xFF2E2E2E)
            todoItem.isCompleted -> Color(0xFF252525)
            else -> Color(0xFF1E1E1E)
        },
        animationSpec = tween(durationMillis = 300),
        label = "card_background"
    )

    // Animated checkbox scale
    val checkboxScale by animateFloatAsState(
        targetValue = if (todoItem.isCompleted) 1.1f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "checkbox_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .scale(scale)
            .alpha(animatedAlpha),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 4.dp
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = if (isDragging) ({}) else onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todoItem.isCompleted,
                onCheckedChange = if (isDragging) null else onCheckedChange,
                modifier = Modifier.scale(checkboxScale),
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF6200EE),
                    uncheckedColor = Color.Gray
                )
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    color = Color.White,
                    textDecoration = if (todoItem.isCompleted) TextDecoration.LineThrough else null
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color(0xFFB0B0B0),
                        textDecoration = if (todoItem.isCompleted) TextDecoration.LineThrough else null,
                        modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                    )
                }
                // Display notification times
                if (todoItem.notificationTimes.isNotEmpty()) {
                    val currentTime = System.currentTimeMillis()
                    val sortedTimes = todoItem.notificationTimes.sortedBy { it }
                    val totalCount = sortedTimes.size

                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        if (totalCount == 1) {
                            val time = sortedTimes[0]
                            val isExpired = time <= currentTime
                            Text(
                                text = "提醒：${formatNotificationTime(time)}${if (isExpired) " (過期)" else ""}",
                                fontSize = 14.sp,
                                color = if (isExpired) Color(0xFFFF6B6B) else Color(0xFF03DAC6)
                            )
                        } else {
                            Text(
                                text = "提醒 (${totalCount}):",
                                fontSize = 14.sp,
                                color = Color(0xFF03DAC6)
                            )
                            sortedTimes.take(2).forEach { time ->
                                val isExpired = time <= currentTime
                                Text(
                                    text = "• ${formatNotificationTime(time)}${if (isExpired) " (過期)" else ""}",
                                    fontSize = 12.sp,
                                    color = if (isExpired) Color(0xFFFF6B6B) else Color(0xFF03DAC6),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            if (totalCount > 2) {
                                Text(
                                    text = "+${totalCount - 2} 更多...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF03DAC6),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            if (isDragging) {
                Text(
                    text = "⋮⋮",
                    fontSize = 24.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}