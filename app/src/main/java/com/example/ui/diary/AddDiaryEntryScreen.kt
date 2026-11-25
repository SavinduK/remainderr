package com.example.ui.diary

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.RemainderrTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDiaryEntryScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val palette = RemainderrTheme.palette
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    val imageUris = remember { mutableStateListOf<Uri>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            imageUris.addAll(uris)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (palette.isDark) Color.Black else Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("diary_add_back"),
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = stringResource(R.string.action_back),
                    tint = palette.title,
                    modifier = Modifier.size(26.dp),
                )
            }

            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)),
                color = palette.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(onClick = openDatePicker)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )

            TextButton(
                onClick = {
                    if (imageUris.isEmpty() && text.isBlank() && title.isBlank()) return@TextButton
                    val iso = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
                    viewModel.createEntry(title, text, iso, imageUris.toList(), onSaved)
                },
                modifier = Modifier.testTag("diary_add_done"),
            ) {
                Text(
                    text = stringResource(R.string.action_done),
                    color = palette.accent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Photo Gallery Grid
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2,
            ) {
                imageUris.forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .then(
                                if (imageUris.size == 1) Modifier.fillMaxWidth().height(220.dp)
                                else Modifier.weight(1f).height(160.dp),
                            )
                            .clip(RoundedCornerShape(16.dp)),
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xCCFFFFFF))
                                .clickable { imageUris.removeAt(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove photo",
                                tint = Color(0xFF555555),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .then(
                            if (imageUris.isEmpty()) Modifier.fillMaxWidth().height(220.dp)
                            else Modifier.fillMaxWidth(0.485f).height(160.dp),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.secondarySurface)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                        .testTag("diary_pick_photos"),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = palette.subtext,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add Photos",
                            color = palette.subtext,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            // Title input
            Box(modifier = Modifier.padding(bottom = 12.dp)) {
                if (title.isEmpty()) {
                    Text(
                        text = "Title (Optional)",
                        color = palette.subtext.copy(alpha = 0.7f),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        color = palette.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    cursorBrush = SolidColor(palette.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diary_title_input"),
                )
            }

            // Body input
            Box(modifier = Modifier.fillMaxWidth()) {
                if (text.isEmpty()) {
                    Text(
                        text = "Write about your day...",
                        color = palette.subtext.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(
                        color = palette.title,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    ),
                    cursorBrush = SolidColor(palette.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .testTag("diary_body_input"),
                )
            }
        }
    }
}
