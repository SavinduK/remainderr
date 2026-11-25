package com.example.ui.history

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

private val ViewBg = Color(0xFF000000)
private val ViewCard = Color(0xFF1C1C1E)
private val ViewEditPill = Color(0xFF2C2C2E)
private val ViewAccent = Color(0xFF007AFF)
private val ViewGreen = Color(0xFF34C759)
private val ViewSubtext = Color(0xFF8E8E93)
private val ViewValueText = Color(0xFFEBEBF5)

@Composable
fun ClinicalViewScreen(
    fileName: String,
    viewModel: ClinicalHistoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(fileName) {
        viewModel.loadHistoryContent(fileName)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ViewBg)
            .statusBarsPadding()
            .imePadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ViewBg)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("clinical_view_back_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = stringResource(R.string.action_back),
                    tint = ViewAccent,
                    modifier = Modifier.size(26.dp),
                )
            }

            Text(
                text = fileName.removeSuffix(".json"),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            )

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ViewEditPill)
                    .clickable { viewModel.toggleViewEditing() }
                    .testTag("clinical_view_edit_toggle")
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (state.isEditing) stringResource(R.string.action_cancel) else stringResource(R.string.action_edit),
                    color = ViewAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        HorizontalDivider(color = ViewCard, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            if (state.isEditing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 450.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ViewCard)
                        .padding(12.dp),
                ) {
                    BasicTextField(
                        value = state.jsonContent,
                        onValueChange = { viewModel.updateViewJson(it) },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                        ),
                        cursorBrush = SolidColor(ViewAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 426.dp)
                            .testTag("clinical_json_editor"),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.saveViewJson() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ViewGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_clinical_json_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save Changes",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                val parsed = remember(state.jsonContent) {
                    runCatching { Json.parseToJsonElement(state.jsonContent) }.getOrNull()
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ViewCard)
                        .padding(16.dp),
                ) {
                    if (parsed != null) {
                        RenderJsonElement(element = parsed, depth = 0)
                    } else {
                        Text(
                            text = state.jsonContent,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }

    if (state.alertTitle != null && state.alertMessage != null) {
        val failed = state.loadFailed
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissViewAlert()
                if (failed) onBack()
            },
            containerColor = ViewCard,
            title = { Text(state.alertTitle.orEmpty(), color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text(state.alertMessage.orEmpty(), color = ViewSubtext) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissViewAlert()
                        if (failed) onBack()
                    },
                ) {
                    Text("OK", color = ViewAccent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

private fun formatClinicalKeyName(key: String): String {
    return key
        .replace(Regex("^[\\d_]+"), "")
        .replace("_", " ")
        .split(" ")
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
            }
        }
}

private fun isEmptyOrNull(element: JsonElement): Boolean {
    return when (element) {
        is JsonNull -> true
        is JsonArray -> element.isEmpty()
        is JsonObject -> element.isEmpty()
        is JsonPrimitive -> element.content == "null" && !element.isString
    }
}

@Composable
private fun RenderJsonElement(element: JsonElement, depth: Int) {
    when (element) {
        is JsonNull -> Unit
        is JsonPrimitive -> {
            val content = element.content
            if (!element.isString && content == "null") return
            Text(
                text = content,
                color = ViewValueText,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
        is JsonArray -> {
            if (element.isEmpty()) return
            Column(modifier = Modifier.padding(start = (depth * 10).coerceAtMost(30).dp)) {
                element.forEach { item ->
                    if (!isEmptyOrNull(item)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp, end = 8.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ViewAccent),
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (item is JsonObject || item is JsonArray) {
                                    RenderJsonElement(item, depth + 1)
                                } else {
                                    Text(
                                        text = (item as? JsonPrimitive)?.content.orEmpty(),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        is JsonObject -> {
            val nonNullEntries = element.entries.filter { (_, v) -> !isEmptyOrNull(v) }
            if (nonNullEntries.isEmpty()) return
            Column(modifier = Modifier.padding(start = if (depth > 0) 10.dp else 0.dp)) {
                nonNullEntries.forEach { (key, value) ->
                    val isTopLevel = depth == 0
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 8.dp,
                                bottom = if (isTopLevel) 12.dp else 4.dp,
                            ),
                    ) {
                        Text(
                            text = formatClinicalKeyName(key),
                            color = if (isTopLevel) ViewAccent else ViewSubtext,
                            fontSize = if (isTopLevel) 16.sp else 14.sp,
                            fontWeight = if (isTopLevel) FontWeight.Bold else FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = if (isTopLevel) 6.dp else 4.dp),
                        )
                        RenderJsonElement(element = value, depth = depth + 1)
                        if (isTopLevel) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = ViewEditPill, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}
