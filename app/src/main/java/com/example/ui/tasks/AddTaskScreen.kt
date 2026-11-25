package com.example.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RemainderrTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AddTaskScreen(
    onBack: () -> Unit,
    onSave: (title: String, description: String, date: LocalDate, startTime: LocalTime?, endTime: LocalTime?) -> Unit,
) {
    val palette = RemainderrTheme.palette
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember { mutableStateOf<LocalTime?>(null) }
    var endTime by remember { mutableStateOf<LocalTime?>(null) }
    var validationError by remember { mutableStateOf<Pair<String, String>?>(null) }

    val openDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                date = LocalDate.of(year, month + 1, dayOfMonth)
            },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth,
        ).show()
    }

    val openTimePicker: (Boolean) -> Unit = { isStart ->
        val initial = if (isStart) startTime ?: LocalTime.now() else endTime ?: LocalTime.now()
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val picked = LocalTime.of(hourOfDay, minute)
                if (isStart) startTime = picked else endTime = picked
            },
            initial.hour,
            initial.minute,
            false,
        ).show()
    }

    val attemptSave = {
        when {
            title.isBlank() -> validationError = "Error" to "Please enter a title"
            startTime != null && endTime != null && !endTime!!.isAfter(startTime) -> {
                validationError = "Invalid Time" to "End time must be after start time"
            }
            else -> {
                onSave(title, description, date, startTime, endTime)
                onBack()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("add_task_back"),
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = stringResource(R.string.action_back),
                    tint = palette.title,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(R.string.new_task_title),
                color = palette.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(palette.accent)
                    .testTag("save_task_button")
                    .clickable(onClick = attemptSave),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.action_save),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            FieldLabel("Title")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("What's on your mind?", color = palette.subtext.copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.card,
                    unfocusedContainerColor = palette.card,
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.border,
                    focusedTextColor = palette.title,
                    unfocusedTextColor = palette.title,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
            )

            FieldLabel("Date")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(palette.card)
                    .border(1.dp, palette.border, RoundedCornerShape(15.dp))
                    .clickable(onClick = openDatePicker)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .testTag("task_date_picker"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)),
                    color = palette.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                )
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = "Select date",
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp),
                )
            }

            FieldLabel("Time Range (Optional)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimeSelectorBox(
                    label = "Start Time",
                    time = startTime,
                    onClick = { openTimePicker(true) },
                    onClear = { startTime = null },
                    modifier = Modifier.weight(1f),
                )
                TimeSelectorBox(
                    label = "End Time",
                    time = endTime,
                    onClick = { openTimePicker(false) },
                    onClear = { endTime = null },
                    modifier = Modifier.weight(1f),
                )
            }

            FieldLabel("Description (Optional)")
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Tap to add notes...", color = palette.subtext.copy(alpha = 0.5f)) },
                minLines = 4,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.card,
                    unfocusedContainerColor = palette.card,
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.border,
                    focusedTextColor = palette.title,
                    unfocusedTextColor = palette.title,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_description_input"),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    validationError?.let { (errTitle, errMsg) ->
        AlertDialog(
            onDismissRequest = { validationError = null },
            containerColor = palette.card,
            title = { Text(errTitle, color = palette.title, fontWeight = FontWeight.Bold) },
            text = { Text(errMsg, color = palette.subtext) },
            confirmButton = {
                TextButton(onClick = { validationError = null }) {
                    Text("OK", color = palette.accent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    val palette = RemainderrTheme.palette
    Text(
        text = text,
        color = palette.subtext,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun TimeSelectorBox(
    label: String,
    time: LocalTime?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = RemainderrTheme.palette
    val display = time?.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US)) ?: "Select Time"

    Column(modifier = modifier) {
        Text(
            text = label,
            color = palette.subtext,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(palette.card)
                .border(1.dp, palette.border, RoundedCornerShape(15.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = display,
                color = if (time != null) palette.title else palette.subtext.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            if (time != null) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = "Clear $label",
                    tint = palette.subtext,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onClear),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = label,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
