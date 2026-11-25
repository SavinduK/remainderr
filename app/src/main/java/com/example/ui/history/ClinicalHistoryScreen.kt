package com.example.ui.history

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.ClinicalHistoryFile
import com.example.ui.theme.RemainderrTheme

private val HistoryBg = Color(0xFF000000)
private val HistoryCard = Color(0xFF1C1C1E)
private val HistoryIconBg = Color(0xFF2C2C2E)
private val HistoryAccent = Color(0xFF007AFF)
private val HistorySubtext = Color(0xFF8E8E93)
private val HistoryDelete = Color(0xFFFF453A)

@Composable
fun ClinicalHistoryScreen(
    viewModel: ClinicalHistoryViewModel,
    onOpenChecklist: () -> Unit,
    onOpenInput: () -> Unit,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()
    val palette = RemainderrTheme.palette
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadHistoryFiles()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HistoryBg)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HistoryBg)
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.patient_histories_title),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(palette.card)
                        .border(1.dp, palette.border, CircleShape)
                        .clickable(onClick = onOpenChecklist)
                        .testTag("open_clinical_checklist_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.history_checklist_title),
                        tint = if (palette.isDark) Color.White else palette.title,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(HistoryAccent)
                        .clickable(onClick = onOpenInput)
                        .testTag("open_clinical_upload_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.upload_note_title),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        if (state.files.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = Color(0xFF3A3A3C),
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No histories found.",
                    color = HistorySubtext,
                    fontSize = 16.sp,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.files, key = { it.fileName }) { fileItem ->
                    HistoryFileCard(
                        item = fileItem,
                        onOpen = { onOpenFile(fileItem.fileName) },
                        onShare = {
                            runCatching {
                                val file = viewModel.getShareFile(fileItem.fileName)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file,
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Share ${fileItem.fileName}"),
                                )
                            }.onFailure {
                                viewModel.setListError("Failed to share file.")
                            }
                        },
                        onDelete = { viewModel.requestDeleteFile(fileItem) },
                    )
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    state.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteFile() },
            containerColor = HistoryCard,
            title = {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ${target.displayName}?",
                    color = HistorySubtext,
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteFile() }) {
                    Text(stringResource(R.string.action_cancel), color = HistorySubtext)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteFile() },
                    colors = ButtonDefaults.buttonColors(containerColor = HistoryDelete),
                ) {
                    Text(stringResource(R.string.action_delete), color = Color.White)
                }
            },
        )
    }

    state.statusError?.let { err ->
        AlertDialog(
            onDismissRequest = { viewModel.setListError(null) },
            containerColor = HistoryCard,
            title = { Text("Error", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text(err, color = HistorySubtext) },
            confirmButton = {
                TextButton(onClick = { viewModel.setListError(null) }) {
                    Text("OK", color = HistoryAccent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun HistoryFileCard(
    item: ClinicalHistoryFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuVisible by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HistoryCard),
        border = BorderStroke(0.dp, Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("history_file_${item.fileName}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(HistoryIconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = HistorySubtext,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.formattedDate.ifBlank { "JSON Document" },
                    color = HistorySubtext,
                    fontSize = 13.sp,
                )
            }

            Box {
                IconButton(
                    onClick = { menuVisible = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("history_menu_${item.fileName}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "File options",
                        tint = HistorySubtext,
                        modifier = Modifier.size(20.dp),
                    )
                }

                DropdownMenu(
                    expanded = menuVisible,
                    onDismissRequest = { menuVisible = false },
                    containerColor = HistoryIconBg,
                ) {
                    DropdownMenuItem(
                        text = { Text("Share File", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null, tint = HistoryAccent, modifier = Modifier.size(18.dp)) },
                        onClick = { menuVisible = false; onShare() },
                    )
                    HorizontalDivider(color = Color(0xFF3A3A3C))
                    DropdownMenuItem(
                        text = { Text("Delete File", color = HistoryDelete, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = HistoryDelete, modifier = Modifier.size(18.dp)) },
                        onClick = { menuVisible = false; onDelete() },
                    )
                }
            }
        }
    }
}
