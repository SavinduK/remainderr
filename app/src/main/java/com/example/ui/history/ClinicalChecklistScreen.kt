package com.example.ui.history

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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.ChecklistItemEntity
import com.example.ui.theme.RemainderrTheme
import java.util.Locale

@Composable
fun ClinicalChecklistScreen(
    viewModel: ClinicalHistoryViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.checklistState.collectAsStateWithLifecycle()
    val palette = RemainderrTheme.palette

    var addModalVisible by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("Systems Review: General") }
    var newTitle by remember { mutableStateOf("") }

    val groupedSections = remember(state.items) {
        val map = linkedMapOf<String, MutableList<ChecklistItemEntity>>()
        state.items.forEach { item ->
            map.getOrPut(item.category) { mutableListOf() }.add(item)
        }
        map.entries.toList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 15.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.clinical_workflow_subtitle),
                    color = palette.subtext,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    text = stringResource(R.string.history_checklist_title),
                    color = palette.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (state.isEditMode) palette.delete.copy(alpha = 0.14f) else palette.card)
                        .border(1.dp, palette.border, RoundedCornerShape(20.dp))
                        .clickable { viewModel.toggleChecklistEditMode() }
                        .testTag("checklist_edit_toggle")
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.isEditMode) stringResource(R.string.action_done) else stringResource(R.string.action_edit),
                        color = if (state.isEditMode) palette.delete else palette.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                        .clickable { addModalVisible = true }
                        .testTag("add_checklist_item_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Clinical Item",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "single_card_container") {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.card),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        groupedSections.forEachIndexed { sIndex, (sectionTitle, sectionItems) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 6.dp),
                            ) {
                                Text(
                                    text = sectionTitle.uppercase(Locale.US),
                                    color = palette.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
                                )

                                sectionItems.forEachIndexed { iIndex, checkItem ->
                                    val isLastItem = sIndex == groupedSections.lastIndex && iIndex == sectionItems.lastIndex

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp)
                                            .clickable {
                                                if (state.isEditMode) {
                                                    viewModel.requestDeleteChecklistItem(checkItem.id)
                                                } else {
                                                    viewModel.toggleChecklistItem(checkItem)
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 12.dp)
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (checkItem.checked) palette.accent else Color.Transparent)
                                                .border(
                                                    width = 1.5.dp,
                                                    color = if (checkItem.checked) palette.accent else palette.subtext,
                                                    shape = RoundedCornerShape(6.dp),
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (checkItem.checked) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "Checked",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp),
                                                )
                                            }
                                        }

                                        Text(
                                            text = checkItem.title,
                                            color = palette.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 19.sp,
                                            textDecoration = if (checkItem.checked) TextDecoration.LineThrough else TextDecoration.None,
                                            modifier = Modifier
                                                .weight(1f)
                                                .alpha(if (checkItem.checked) 0.5f else 1f),
                                        )

                                        if (state.isEditMode) {
                                            Text(
                                                text = "Remove",
                                                color = palette.delete,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .padding(start = 8.dp)
                                                    .clickable { viewModel.requestDeleteChecklistItem(checkItem.id) },
                                            )
                                        }
                                    }

                                    if (!isLastItem) {
                                        HorizontalDivider(
                                            color = palette.border.copy(alpha = 0.25f),
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(start = 32.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(36.dp)) }
        }
    }

    if (state.deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteChecklistItem() },
            containerColor = palette.card,
            title = { Text("Delete Clinical Item?", color = palette.title, fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    text = "This item will be removed permanently from your list.",
                    color = palette.subtext,
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteChecklistItem() }) {
                    Text(stringResource(R.string.action_cancel), color = palette.subtext, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteChecklistItem() },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.delete.copy(alpha = 0.14f)),
                ) {
                    Text(stringResource(R.string.action_delete), color = palette.delete, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    if (addModalVisible) {
        AlertDialog(
            onDismissRequest = { addModalVisible = false },
            containerColor = palette.card,
            title = { Text("Add Clinical Item", color = palette.title, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        placeholder = { Text("Category (e.g., Systems Review: CNS)", color = palette.subtext, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = palette.title,
                            unfocusedTextColor = palette.title,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checklist_new_category_input"),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = { Text("Symptom or Requirement title", color = palette.subtext, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = palette.title,
                            unfocusedTextColor = palette.title,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checklist_new_title_input"),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { addModalVisible = false }) {
                    Text(stringResource(R.string.action_cancel), color = palette.subtext, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.addChecklistItem(newCategory, newTitle)
                            newTitle = ""
                            addModalVisible = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                ) {
                    Text("Add Item", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}
