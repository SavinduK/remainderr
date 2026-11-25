package com.example.ui.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R

private val InputBg = Color(0xFF000000)
private val InputCard = Color(0xFF1C1C1E)
private val InputIconCircle = Color(0xFF2C2C2E)
private val InputAccent = Color(0xFF007AFF)
private val InputSubtext = Color(0xFF8E8E93)
private val InputDelete = Color(0xFFFF453A)

@Composable
fun ClinicalInputScreen(
    viewModel: ClinicalHistoryViewModel,
    onExtractedSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.inputState.collectAsStateWithLifecycle()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        viewModel.addSelectedImages(uris)
    }

    val openPicker = {
        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(InputBg)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(InputBg)
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.upload_note_title),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            if (state.selectedImages.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearSelectedImages() },
                    modifier = Modifier.testTag("clear_selected_images_button"),
                ) {
                    Text(
                        text = "Clear All",
                        color = InputDelete,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            if (state.selectedImages.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                ) {
                    itemsIndexed(state.selectedImages) { index, uri ->
                        Box(
                            modifier = Modifier
                                .width(260.dp)
                                .height(380.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(InputCard),
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected clinical page ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            IconButton(
                                onClick = { viewModel.removeSelectedImage(index) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x88000000)),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove page ${index + 1}",
                                    tint = InputDelete,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xB3000000))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "Page ${index + 1}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(InputCard)
                        .clickable(onClick = openPicker)
                        .testTag("pick_images_placeholder")
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(InputIconCircle),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = InputAccent,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap to select handwritten history page(s)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Supports multiple JPEG or PNG images",
                        color = InputSubtext,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Secondary Button: Pick / Add More Images
            Button(
                onClick = openPicker,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InputCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pick_images_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = InputAccent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.selectedImages.isNotEmpty()) "Add More Images" else "Pick Images",
                    color = InputAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val extractEnabled = state.selectedImages.isNotEmpty() && !state.isExtracting
            val pageCount = state.selectedImages.size
            Button(
                onClick = { viewModel.extractAndSave() },
                enabled = extractEnabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InputAccent,
                    disabledContainerColor = InputIconCircle,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .alpha(if (extractEnabled) 1f else 0.6f)
                    .testTag("extract_and_save_button"),
            ) {
                if (state.isExtracting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Extract & Save ($pageCount ${if (pageCount == 1) "Page" else "Pages"})",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    if (state.alertTitle != null && state.alertMessage != null) {
        val wasSuccess = state.isSuccess
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissInputAlert()
                if (wasSuccess) onExtractedSuccess()
            },
            containerColor = InputCard,
            title = {
                Text(
                    text = state.alertTitle.orEmpty(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = state.alertMessage.orEmpty(),
                    color = InputSubtext,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissInputAlert()
                        if (wasSuccess) onExtractedSuccess()
                    },
                ) {
                    Text("OK", color = InputAccent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}
