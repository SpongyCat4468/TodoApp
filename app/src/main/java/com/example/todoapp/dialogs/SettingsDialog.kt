package com.example.todoapp.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsDialog(
    isMuted: Boolean,
    recurringCount: Int = 10,
    onDismiss: () -> Unit,
    onMuteChanged: (Boolean) -> Unit,
    onRecurringCountChanged: ((Int) -> Unit)? = null
) {
    var muteState by remember { mutableStateOf(isMuted) }
    var countState by remember { mutableFloatStateOf(recurringCount.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "設定",
                color = Color.White
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "靜音",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Switch(
                        checked = muteState,
                        onCheckedChange = {
                            muteState = it
                            onMuteChanged(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6200EE),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF424242)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "重複提醒次數",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "(適用於每日，每週，每月，每年)",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B0),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${countState.toInt()} 次",
                            fontSize = 16.sp,
                            color = Color(0xFFBB86FC)
                        )
                    }
                    Slider(
                        value = countState,
                        onValueChange = { countState = it },
                        valueRange = 5f..100f,
                        steps = 94,
                        onValueChangeFinished = {
                            onRecurringCountChanged?.invoke(countState.toInt())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF6200EE),
                            activeTrackColor = Color(0xFF6200EE),
                            inactiveTrackColor = Color(0xFF424242)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "5",
                            fontSize = 12.sp,
                            color = Color(0xFFB0B0B0)
                        )
                        Text(
                            text = "100",
                            fontSize = 12.sp,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "確定",
                    color = Color(0xFF03DAC6)
                )
            }
        },
        containerColor = Color(0xFF2C2C2C),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}