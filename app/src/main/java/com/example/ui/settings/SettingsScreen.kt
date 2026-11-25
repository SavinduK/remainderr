package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R

private val SettingsBg = Color(0xFF000000)
private val SettingsCard = Color(0xFF1C1C1E)
private val SettingsInputBg = Color(0xFF2C2C2E)
private val SettingsSegmentActive = Color(0xFF3A3A3C)
private val SettingsAccent = Color(0xFF007AFF)
private val SettingsGreen = Color(0xFF34C759)
private val SettingsSubtext = Color(0xFF8E8E93)
private val SettingsMuted = Color(0xFF636366)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenPasswords: () -> Unit,
    onOpenClinicalTracker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { viewModel.backupAll(it) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { viewModel.restoreAll(it) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SettingsBg)
            .statusBarsPadding()
            .imePadding(),
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SettingsBg)
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            // Collapsible Section: AI Configuration
            CollapsibleSectionHeader(
                icon = Icons.Filled.AutoAwesome,
                title = stringResource(R.string.ai_configuration),
                isCollapsed = state.isAiCollapsed,
                testTag = "ai_config_header",
                onClick = { viewModel.toggleAiCollapsed() },
            )

            AnimatedVisibility(visible = !state.isAiCollapsed) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SettingsCard)
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "GEMINI API KEY",
                            color = SettingsSubtext,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.toggleShowKey() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("toggle_api_key_visibility"),
                            ) {
                                Icon(
                                    imageVector = if (state.showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (state.showKey) "Hide API Key" else "Show API Key",
                                    tint = SettingsSubtext,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (state.apiKey.isBlank()) {
                                        viewModel.showAlert("Notice", "No API key to copy.")
                                    } else {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Gemini API Key", state.apiKey))
                                        viewModel.showAlert("Copied", "API Key copied to clipboard.")
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("copy_api_key_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy API Key",
                                    tint = SettingsSubtext,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SettingsInputBg)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.apiKey.isEmpty()) {
                            Text(
                                text = "Enter API Key",
                                color = SettingsMuted,
                                fontSize = 16.sp,
                            )
                        }
                        BasicTextField(
                            value = state.apiKey,
                            onValueChange = { viewModel.updateApiKey(it) },
                            singleLine = true,
                            visualTransformation = if (state.showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(SettingsAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_api_key_input"),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "MODEL SELECTION",
                        color = SettingsSubtext,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SettingsInputBg)
                            .padding(3.dp),
                    ) {
                        val models = listOf(
                            "gemini-2.5-flash" to "2.5 Flash",
                            "gemini-3.5-flash" to "3.5 Flash",
                        )
                        models.forEach { (modelId, label) ->
                            val active = state.model == modelId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) SettingsSegmentActive else Color.Transparent)
                                    .clickable { viewModel.selectModel(modelId) }
                                    .testTag("model_segment_$modelId")
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    color = if (active) Color.White else SettingsSubtext,
                                    fontSize = 14.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                )
                            }
                        }
                    }

                    Text(
                        text = "Active Model: ${state.model}",
                        color = SettingsMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.saveSettings() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_settings_button"),
                    ) {
                        Text(
                            text = "Save Settings",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Collapsible Section: Backup & Restore
            CollapsibleSectionHeader(
                icon = Icons.Outlined.CloudUpload,
                title = stringResource(R.string.backup_and_restore),
                isCollapsed = state.isBackupCollapsed,
                testTag = "backup_restore_header",
                onClick = { viewModel.toggleBackupCollapsed() },
            )

            AnimatedVisibility(visible = !state.isBackupCollapsed) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SettingsCard)
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Export all app notes, passwords, transactions, and local files into a directory or restore them from an existing backup.",
                        color = SettingsSubtext,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    Button(
                        onClick = { backupLauncher.launch(null) },
                        enabled = !state.isBusy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("backup_data_button"),
                    ) {
                        if (state.isBusy) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Backup Data",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { restoreLauncher.launch(null) },
                        enabled = !state.isBusy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("restore_data_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Restore Data",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Password Tracker Navigation Card
            SettingsNavigationCard(
                icon = Icons.Filled.Key,
                title = stringResource(R.string.password_tracker),
                testTag = "nav_password_tracker",
                onClick = onOpenPasswords,
            )

            // Clinical Tracker Navigation Card
            SettingsNavigationCard(
                icon = Icons.Filled.MedicalServices,
                title = stringResource(R.string.clinical_tracker),
                testTag = "nav_clinical_tracker",
                onClick = onOpenClinicalTracker,
            )
        }
    }

    if (state.alertTitle != null && state.alertMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            containerColor = SettingsCard,
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
                    color = SettingsSubtext,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAlert() }) {
                    Text("OK", color = SettingsAccent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun CollapsibleSectionHeader(
    icon: ImageVector,
    title: String,
    isCollapsed: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SettingsCard)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsAccent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Icon(
            imageVector = if (isCollapsed) Icons.Filled.ChevronRight else Icons.Filled.ExpandMore,
            contentDescription = if (isCollapsed) "Expand $title" else "Collapse $title",
            tint = SettingsSubtext,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsNavigationCard(
    icon: ImageVector,
    title: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SettingsCard)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsAccent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = title,
            tint = SettingsSubtext,
            modifier = Modifier.size(20.dp),
        )
    }
}
