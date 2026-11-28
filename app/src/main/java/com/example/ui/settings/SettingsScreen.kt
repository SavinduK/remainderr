package com.example.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.RemainderrTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenPasswords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = RemainderrTheme.palette
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        activity?.let { act ->
            viewModel.handleAuthorizationIntentResult(act, result.resultCode, result.data)
        }
    }

    LaunchedEffect(state.pendingResolution) {
        val intentSender = state.pendingResolution ?: return@LaunchedEffect
        viewModel.consumeResolution()
        authLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
    }

    val lastSyncLabel = remember(state.lastSyncedAt) {
        if (state.lastSyncedAt <= 0L) {
            "Never synced"
        } else {
            runCatching {
                Instant.ofEpochMilli(state.lastSyncedAt)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", Locale.ENGLISH))
            }.getOrDefault("Synced")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding(),
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                color = palette.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Google Drive Sync Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = palette.card),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Cloud,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.google_drive_sync),
                                    color = palette.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = if (state.isConnected) state.connectedEmail else "Cloud backup & device sync",
                                    color = palette.subtext,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        val statusColor = if (state.isConnected) palette.success else palette.subtext
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(statusColor.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (state.isConnected) "Connected" else "Offline",
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dedicated folder & synced datasets summary
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.secondarySurface)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Drive Folder: ${state.folderName}",
                                color = palette.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf("Notes", "Tasks", "Transactions", "Passwords").forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(palette.card)
                                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(palette.accent),
                                    )
                                    Text(
                                        text = item,
                                        color = palette.text,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Last synced: $lastSyncLabel",
                            color = palette.subtext,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!state.isConnected) {
                        Button(
                            onClick = { activity?.let { viewModel.startDriveAction(it, DriveAction.CONNECT) } },
                            enabled = !state.isSyncing && activity != null,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("connect_google_drive_button"),
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Cloud,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connect Google Drive",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { activity?.let { viewModel.startDriveAction(it, DriveAction.SYNC) } },
                            enabled = !state.isSyncing && activity != null,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("drive_sync_now_button"),
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Syncing...",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sync Now",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = { activity?.let { viewModel.startDriveAction(it, DriveAction.UPLOAD) } },
                                enabled = !state.isSyncing && activity != null,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, palette.border),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("drive_upload_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    tint = palette.title,
                                    modifier = Modifier.size(17.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Upload",
                                    color = palette.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.showRestoreConfirm() },
                                enabled = !state.isSyncing && activity != null,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, palette.border),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("drive_restore_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = null,
                                    tint = palette.title,
                                    modifier = Modifier.size(17.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Restore",
                                    color = palette.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = palette.border)

                        TextButton(
                            onClick = { viewModel.disconnectDrive() },
                            enabled = !state.isSyncing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .testTag("drive_disconnect_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LinkOff,
                                contentDescription = null,
                                tint = palette.delete,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Disconnect Account",
                                color = palette.delete,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Password Tracker Navigation Card
            SettingsNavigationCard(
                icon = Icons.Filled.Key,
                title = stringResource(R.string.password_tracker),
                subtitle = "Manage saved logins and credentials",
                testTag = "nav_password_tracker",
                onClick = onOpenPasswords,
            )
        }
    }

    if (state.restoreConfirmVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreConfirm() },
            containerColor = palette.card,
            title = {
                Text(
                    text = "Restore from Google Drive",
                    color = palette.title,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "This will replace your local notes, tasks, transactions, and passwords with the data stored in \"${state.folderName}\" on Google Drive.",
                    color = palette.subtext,
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestoreConfirm() }) {
                    Text(stringResource(R.string.action_cancel), color = palette.subtext)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissRestoreConfirm()
                        activity?.let { viewModel.startDriveAction(it, DriveAction.RESTORE) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                ) {
                    Text("Restore", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    if (state.alertTitle != null && state.alertMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            containerColor = palette.card,
            title = {
                Text(
                    text = state.alertTitle.orEmpty(),
                    color = palette.title,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = state.alertMessage.orEmpty(),
                    color = palette.subtext,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAlert() }) {
                    Text("OK", color = palette.accent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun SettingsNavigationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val palette = RemainderrTheme.palette
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.card),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(palette.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = title,
                        color = palette.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = subtitle,
                        color = palette.subtext,
                        fontSize = 12.sp,
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = title,
                tint = palette.subtext,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
