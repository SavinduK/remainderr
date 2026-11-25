package com.example.ui.diary

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.RemainderrTheme
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EditDiaryEntryScreen(
    folderPath: String,
    viewModel: DiaryViewModel,
    onBack: () -> Unit,
) {
    val palette = RemainderrTheme.palette
    val context = LocalContext.current

    var isEditing by remember { mutableStateOf(false) }
    var entryId by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    val imageSources = remember { mutableStateListOf<String>() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var validationAlert by remember { mutableStateOf(false) }

    LaunchedEffect(folderPath) {
        val loaded = viewModel.loadSingleEntry(folderPath)
        if (loaded != null) {
            entryId = loaded.id
            title = loaded.title
            text = loaded.text
            date = runCatching {
                Instant.parse(loaded.dateIso).atZone(ZoneId.systemDefault()).toLocalDate()
            }.getOrDefault(LocalDate.now())
            imageSources.clear()
            imageSources.addAll(loaded.imagePaths)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            imageSources.addAll(uris.map { it.toString() })
        }
    }

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

    val saveChanges = {
        if (imageSources.isEmpty() && text.isBlank() && title.isBlank()) {
            validationAlert = true
        } else {
            val iso = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
            viewModel.updateEntry(
                folderPath = folderPath,
                existingId = entryId,
                title = title,
                text = text,
                dateIso = iso,
                sources = imageSources.toList(),
                onSaved = { isEditing = false },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // Top Header Media Carousel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
        ) {
            if (imageSources.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { imageSources.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val src = imageSources[page]
                    val model: Any = if (src.startsWith("/")) File(src) else src
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = model,
                            contentDescription = "Journal image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (isEditing) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .statusBarsPadding()
                                    .padding(top = 56.dp, end = 20.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x99000000))
                                    .clickable { imageSources.removeAt(page) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.secondarySurface)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                            isEditing = true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = palette.subtext,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to Add Photos",
                        color = palette.subtext,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Floating Top Nav Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = onBack),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("diary_edit_toggle_button")
                        .clickable {
                            if (isEditing) saveChanges() else isEditing = true
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isEditing) Icons.Filled.Check else Icons.Outlined.Create,
                            contentDescription = if (isEditing) "Save" else "Edit",
                            tint = if (isEditing) palette.accent else Color(0xFF1A1A1A),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Floating Action Pills in Edit Mode
            if (isEditing) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFEF4444))
                            .clickable { showDeleteConfirm = true }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Delete", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF18181B))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Add Photos", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Overlapping Bottom Sheet Content Area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = palette.card,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                val formattedDate = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
                if (isEditing) {
                    Text(
                        text = "TITLE",
                        color = palette.subtext,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Enter Title...", color = palette.subtext) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.secondarySurface,
                            unfocusedContainerColor = palette.secondarySurface,
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = palette.title,
                            unfocusedTextColor = palette.title,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = "DATE",
                        color = palette.subtext,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.secondarySurface)
                            .clickable(onClick = openDatePicker)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text(text = formattedDate, color = palette.title, fontSize = 16.sp)
                    }

                    Text(
                        text = "MEMORIES & DESCRIPTION",
                        color = palette.subtext,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("No details provided.", color = palette.subtext) },
                        minLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.secondarySurface,
                            unfocusedContainerColor = palette.secondarySurface,
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = palette.title,
                            unfocusedTextColor = palette.title,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = saveChanges,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text("Save Changes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = formattedDate,
                            color = palette.subtext,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Text(
                        text = title.ifBlank { stringResource(R.string.untitled_entry) },
                        color = palette.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 34.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = palette.border,
                    )

                    Text(
                        text = text.ifBlank { stringResource(R.string.no_details_provided) },
                        color = if (text.isNotBlank()) palette.title else palette.subtext,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = palette.card,
            title = { Text(stringResource(R.string.delete_entry_title), color = palette.title, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_entry_confirm), color = palette.subtext) },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel), color = palette.subtext)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteEntry(folderPath, onBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.delete),
                ) {
                    Text(stringResource(R.string.action_delete), color = Color.White)
                }
            },
        )
    }

    if (validationAlert) {
        AlertDialog(
            onDismissRequest = { validationAlert = false },
            containerColor = palette.card,
            title = { Text("Empty Entry", color = palette.title, fontWeight = FontWeight.Bold) },
            text = { Text("Please add at least a title, note, or photo.", color = palette.subtext) },
            confirmButton = {
                TextButton(onClick = { validationAlert = false }) {
                    Text("OK", color = palette.accent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}
