package com.example.todoapp.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DividerWithText(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFF424242)
        )
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp),
            color = Color.Gray
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFF424242)
        )
    }
}

@Composable
fun EmptyText() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "(Empty)",
                color = Color.Gray
            )
        }
    }
}

fun formatNotificationTime(timestamp: Long): String {
    val notificationCal = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    val todayCal = Calendar.getInstance()

    val isToday = notificationCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            notificationCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

    return if (isToday) {
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        "Today ${timeFormatter.format(Date(timestamp))}"
    } else {
        val formatter = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        formatter.format(Date(timestamp))
    }
}
