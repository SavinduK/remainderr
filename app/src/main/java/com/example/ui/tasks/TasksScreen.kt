package com.example.ui.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.TaskEntity
import com.example.ui.theme.RemainderrTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onAddTaskClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = RemainderrTheme.palette
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    val monthGrid = generateMonthGrid(state.viewMonth)
    val selectedDateTasks = state.tasks.filter { task ->
        parseTaskLocalDate(task.date, zone) == state.selectedDate
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding(),
    ) {
        // Calendar Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tasks_title),
                    color = palette.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(palette.taskBlue)
                        .testTag("add_task_button")
                        .clickable(onClick = onAddTaskClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.new_task_title),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Month navigation header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.viewMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                    color = palette.subtext,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.changeMonth(-1) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("prev_month_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "Previous month",
                            tint = palette.subtext,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.changeMonth(1) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("next_month_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Next month",
                            tint = palette.subtext,
                        )
                    }
                }
            }

            // Weekday row
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        color = palette.subtext,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 5x7 Calendar Grid
            Column {
                monthGrid.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { dateItem ->
                            val isCurrentMonth = YearMonth.from(dateItem) == state.viewMonth
                            val isSelected = dateItem == state.selectedDate
                            val isToday = dateItem == today
                            val dayTasks = state.tasks.filter { parseTaskLocalDate(it.date, zone) == dateItem }

                            val dayColor = when {
                                dayTasks.isEmpty() -> if (isCurrentMonth) palette.text else Color(0xFFC5C5C5)
                                dayTasks.any { !it.completed && dateItem < today } -> palette.taskRed
                                dayTasks.any { !it.completed && dateItem >= today } -> palette.taskBlue
                                dayTasks.all { it.completed } -> palette.taskGreen
                                else -> if (isCurrentMonth) palette.subtext else Color(0xFFC5C5C5)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable { viewModel.selectDate(dateItem) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (isSelected) {
                                                Modifier
                                                    .background(Color(0x143A86FF))
                                                    .drawBehind {
                                                        drawLine(
                                                            color = Color(0xFF3A86FF),
                                                            start = Offset(0f, size.height),
                                                            end = Offset(size.width, size.height),
                                                            strokeWidth = 2.dp.toPx(),
                                                        )
                                                    }
                                            } else if (isToday) {
                                                Modifier.border(1.dp, Color(0xFFA0A0A0), CircleShape)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = dateItem.dayOfMonth.toString(),
                                        color = dayColor,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Sheet Container for Selected Date Tasks
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = palette.card,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.selectedDate.format(DateTimeFormatter.ofPattern("MMMM d", Locale.US)),
                        color = palette.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                    )
                    IconButton(
                        onClick = { viewModel.toggleEditMode() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_tasks_edit_button"),
                    ) {
                        Icon(
                            imageVector = if (state.isEditMode) Icons.Filled.Check else Icons.Filled.Edit,
                            contentDescription = if (state.isEditMode) "Finish editing" else "Edit tasks",
                            tint = if (state.isEditMode) palette.taskGreen else palette.subtext,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                if (selectedDateTasks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EventAvailable,
                            contentDescription = null,
                            tint = palette.border,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.no_tasks_for_day),
                            color = palette.subtext,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(selectedDateTasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                isEditMode = state.isEditMode,
                                onToggle = { viewModel.toggleComplete(task) },
                                onDelete = { viewModel.requestDelete(task.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDelete() },
            containerColor = palette.card,
            title = {
                Text(
                    text = stringResource(R.string.delete_task_title),
                    color = palette.title,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_task_subtitle),
                    color = palette.subtext,
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDelete() }) {
                    Text(stringResource(R.string.action_cancel), color = palette.subtext, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.taskRed),
                ) {
                    Text(stringResource(R.string.action_delete), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    isEditMode: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = RemainderrTheme.palette
    val scale by animateFloatAsState(
        targetValue = if (task.completed) 0.99f else 1f,
        animationSpec = spring(),
        label = "task_scale",
    )
    val timeText = formatTaskTime(task.startTime)
        ?: formatTaskTime(task.endTime)
        ?: stringResource(R.string.all_day)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Circular checkbox
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (task.completed) {
                        Modifier.background(palette.taskGreen)
                    } else {
                        Modifier.border(2.dp, palette.border, CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (task.completed) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = task.title,
                color = palette.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.alpha(if (task.completed) 0.4f else 1f),
            )
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    color = palette.subtext,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .alpha(0.6f),
                )
            }
        }

        if (isEditMode) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("delete_task_${task.id}"),
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = palette.taskRed,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Text(
                text = timeText,
                color = palette.subtext,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(0.7f),
            )
        }
    }
}

private fun generateMonthGrid(month: YearMonth): List<LocalDate> {
    val firstDayOfMonth = month.atDay(1)
    // DayOfWeek: MONDAY=1..SUNDAY=7 -> convert so SUNDAY=0, MONDAY=1..SATURDAY=6
    val startingDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val startDate = firstDayOfMonth.minusDays(startingDayOfWeek.toLong())
    return (0 until 35).map { offset -> startDate.plusDays(offset.toLong()) }
}

private fun parseTaskLocalDate(iso: String, zone: ZoneId): LocalDate? {
    return runCatching { Instant.parse(iso).atZone(zone).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()
}

private fun formatTaskTime(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return runCatching {
        Instant.parse(iso)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
            .lowercase(Locale.US)
    }.getOrNull()
}
