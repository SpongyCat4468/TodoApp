package com.example.todoapp.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

data class NotificationTime(
    val time: Long,
    val recurringType: RecurringType = RecurringType.NONE
)

enum class RecurringType {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}

fun RecurringType.toDisplayString(): String {
    return when (this) {
        RecurringType.NONE -> ""
        RecurringType.DAILY -> "每日"
        RecurringType.WEEKLY -> "每週"
        RecurringType.MONTHLY -> "每月"
        RecurringType.YEARLY -> "每年"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (NotificationTime) -> Unit,
    onRecurringTimeSelected: ((List<NotificationTime>) -> Unit)? = null,
    recurringCount: Int = 10
) {
    val calendar = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var selectedRecurring by remember { mutableStateOf(RecurringType.NONE) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRecurringPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "設定提醒時間",
                color = Color.White
            )
        },
        text = {
            Column {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text(
                        text = String.format("%04d/%02d/%02d", selectedYear, selectedMonth + 1, selectedDay),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text(
                        text = String.format("%02d:%02d", selectedHour, selectedMinute),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showRecurringPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text(
                        text = when (selectedRecurring) {
                            RecurringType.NONE -> "不重複"
                            RecurringType.DAILY -> "每日"
                            RecurringType.WEEKLY -> "每週"
                            RecurringType.MONTHLY -> "每月"
                            RecurringType.YEARLY -> "每年"
                        },
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, selectedYear)
                        set(Calendar.MONTH, selectedMonth)
                        set(Calendar.DAY_OF_MONTH, selectedDay)
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (selectedRecurring != RecurringType.NONE && onRecurringTimeSelected != null) {
                        val times = generateRecurringNotifications(cal.timeInMillis, selectedRecurring, recurringCount)
                        onRecurringTimeSelected(times)
                    } else {
                        onTimeSelected(NotificationTime(cal.timeInMillis, RecurringType.NONE))
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE),
                    contentColor = Color.White
                )
            ) {
                Text("OK")
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }.timeInMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = millis
                            }
                            selectedYear = cal.get(Calendar.YEAR)
                            selectedMonth = cal.get(Calendar.MONTH)
                            selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF6200EE)
                    )
                ) {
                    Text("確定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFB0B0B0)
                    )
                ) {
                    Text("取消")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF1E1E1E),
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                weekdayContentColor = Color(0xFFB0B0B0),
                subheadContentColor = Color.White,
                yearContentColor = Color.White,
                currentYearContentColor = Color(0xFF6200EE),
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = Color(0xFF6200EE),
                dayContentColor = Color.White,
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = Color(0xFF6200EE),
                todayContentColor = Color(0xFF6200EE),
                todayDateBorderColor = Color(0xFF6200EE)
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF6200EE)
                    )
                ) {
                    Text("確定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFB0B0B0)
                    )
                ) {
                    Text("取消")
                }
            },
            text = {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "選擇時間",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WheelPicker(
                            range = 0..23,
                            selectedValue = selectedHour,
                            onValueSelected = { selectedHour = it }
                        )

                        WheelPicker(
                            range = 0..59,
                            selectedValue = selectedMinute,
                            onValueSelected = { selectedMinute = it }
                        )
                    }
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showRecurringPicker) {
        AlertDialog(
            onDismissRequest = { showRecurringPicker = false },
            title = {
                Text("重複頻率", color = Color.White)
            },
            text = {
                Column {
                    RecurringOption(
                        text = "不重複",
                        isSelected = selectedRecurring == RecurringType.NONE,
                        onClick = {
                            selectedRecurring = RecurringType.NONE
                            showRecurringPicker = false
                        }
                    )
                    RecurringOption(
                        text = "每日",
                        isSelected = selectedRecurring == RecurringType.DAILY,
                        onClick = {
                            selectedRecurring = RecurringType.DAILY
                            showRecurringPicker = false
                        }
                    )
                    RecurringOption(
                        text = "每週",
                        isSelected = selectedRecurring == RecurringType.WEEKLY,
                        onClick = {
                            selectedRecurring = RecurringType.WEEKLY
                            showRecurringPicker = false
                        }
                    )
                    RecurringOption(
                        text = "每月",
                        isSelected = selectedRecurring == RecurringType.MONTHLY,
                        onClick = {
                            selectedRecurring = RecurringType.MONTHLY
                            showRecurringPicker = false
                        }
                    )
                    RecurringOption(
                        text = "每年",
                        isSelected = selectedRecurring == RecurringType.YEARLY,
                        onClick = {
                            selectedRecurring = RecurringType.YEARLY
                            showRecurringPicker = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showRecurringPicker = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF6200EE)
                    )
                ) {
                    Text("確定")
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

@Composable
fun RecurringOption(
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
fun WheelPicker(
    range: IntRange,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit
) {
    val baseList = range.toList()
    val repeatedList = remember { List(1000) { baseList }.flatten() }

    val centerStartIndex = remember { (repeatedList.size / 2) - (repeatedList.size / 2 % baseList.size) }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = centerStartIndex + (selectedValue - range.first)
    )

    val centerItemOffset = 1

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val index = lazyListState.firstVisibleItemIndex + centerItemOffset
            val realValue = repeatedList[index]

            onValueSelected(realValue)

            lazyListState.scrollToItem(
                centerStartIndex + (realValue - range.first)
            )
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .height(150.dp)
            .width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(repeatedList.size) { i ->
            val value = repeatedList[i]

            Text(
                text = "%02d".format(value),
                fontSize = 28.sp,
                color = if (value == selectedValue) Color.White else Color(0xFF666666),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

fun generateRecurringNotifications(baseTime: Long, recurringType: RecurringType, count: Int = 10): List<NotificationTime> {
    val times = mutableListOf<NotificationTime>()
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = baseTime

    for (i in 0 until count) {
        times.add(NotificationTime(calendar.timeInMillis, recurringType))

        when (recurringType) {
            RecurringType.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            RecurringType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurringType.YEARLY -> calendar.add(Calendar.YEAR, 1)
            RecurringType.NONE -> break
        }
    }

    return times
}