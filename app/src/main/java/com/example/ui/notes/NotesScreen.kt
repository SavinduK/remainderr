package com.example.ui.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.NoteEntity
import com.example.ui.theme.RemainderrTheme
import kotlin.math.abs

private val CategoryPalette = listOf(
    Color(0xFF2563EB), // Blue - Work
    Color(0xFF10B981), // Emerald - Personal
    Color(0xFFF59E0B), // Amber - Ideas
    Color(0xFFEC4899), // Rose Pink - To-Do
    Color(0xFF8B5CF6), // Violet
    Color(0xFF06B6D4), // Cyan
    Color(0xFFEF4444), // Coral Red
    Color(0xFF14B8A6), // Teal
    Color(0xFFF97316), // Tangerine
    Color(0xFF6366F1), // Indigo
    Color(0xFF84CC16), // Lime
    Color(0xFFD946EF), // Fuchsia
)

private val FixedCategoryColors = mapOf(
    "work" to CategoryPalette[0],
    "personal" to CategoryPalette[1],
    "ideas" to CategoryPalette[2],
    "to-do" to CategoryPalette[3],
)

private fun buildCategoryColorMap(categories: List<String>): Map<String, Color> {
    val result = linkedMapOf<String, Color>()
    val used = mutableSetOf<Color>()

    categories.forEach { cat ->
        val fixed = FixedCategoryColors[cat.trim().lowercase()]
        if (fixed != null && fixed !in used) {
            result[cat] = fixed
            used.add(fixed)
        }
    }

    var paletteIdx = 0
    var overflowIdx = 0
    categories.forEach { cat ->
        if (cat !in result) {
            var assigned: Color? = null
            while (paletteIdx < CategoryPalette.size) {
                val candidate = CategoryPalette[paletteIdx++]
                if (candidate !in used) {
                    assigned = candidate
                    used.add(candidate)
                    break
                }
            }
            if (assigned == null) {
                val hue = ((overflowIdx * 137.508f) + 25f) % 360f
                overflowIdx++
                assigned = Color.hsv(hue, 0.72f, 0.85f)
            }
            result[cat] = assigned
        }
    }
    return result
}

private fun fallbackCategoryColor(category: String): Color {
    FixedCategoryColors[category.trim().lowercase()]?.let { return it }
    val index = abs(category.trim().lowercase().hashCode()) % CategoryPalette.size
    return CategoryPalette[index]
}

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onAddNoteClick: () -> Unit,
    onEditNoteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = RemainderrTheme.palette
    val context = LocalContext.current

    var categoryModalNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isCategoryEditMode by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var deleteNoteTarget by remember { mutableStateOf<NoteEntity?>(null) }

    val categoryColorMap = remember(state.categories) {
        buildCategoryColorMap(state.categories)
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
                .padding(horizontal = 25.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.notes_title),
                color = palette.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.accent)
                    .clickable(onClick = onAddNoteClick)
                    .testTag("add_note_button"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New note",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 15.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.card)
                .border(1.dp, palette.border, RoundedCornerShape(24.dp))
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = palette.subtext,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (state.searchQuery.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        color = palette.subtext,
                        fontSize = 15.sp,
                    )
                }
                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    singleLine = true,
                    textStyle = TextStyle(color = palette.title, fontSize = 15.sp),
                    cursorBrush = SolidColor(palette.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_search_input"),
                )
            }
        }

        // Navigation Tabs (All Notes & Categories)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 15.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val tabs = listOf(
                NotesTab.ALL to stringResource(R.string.all_notes_tab),
                NotesTab.CATEGORIES to stringResource(R.string.categories_tab),
            )
            tabs.forEach { (tab, label) ->
                val active = state.activeTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.setTab(tab) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("notes_tab_${tab.name.lowercase()}"),
                ) {
                    Text(
                        text = label,
                        color = if (active) palette.accent else palette.subtext,
                        fontSize = 15.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (active) {
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(palette.accent),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }

        // Category filter header when category selected
        if (state.activeTab == NotesTab.CATEGORIES && state.selectedCategory != null) {
            val selCat = state.selectedCategory.orEmpty()
            val selColor = categoryColorMap[selCat] ?: fallbackCategoryColor(selCat)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(selColor),
                    )
                    Text(
                        text = buildAnnotatedString {
                            append("Category: ")
                            withStyle(SpanStyle(color = selColor)) {
                                append(selCat)
                            }
                        },
                        color = palette.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.card)
                        .border(1.dp, palette.border, RoundedCornerShape(20.dp))
                        .clickable { viewModel.selectCategory(null) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = palette.subtext,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("All Categories", color = palette.subtext, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Content: either Category List or Full-Width Notes List
        if (state.activeTab == NotesTab.CATEGORIES && state.selectedCategory == null) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.categories, key = { it }) { cat ->
                    val noteCount = state.allNotes.count { it.category == cat }
                    val catColor = categoryColorMap[cat] ?: fallbackCategoryColor(cat)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.card),
                        border = BorderStroke(1.dp, catColor.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectCategory(cat) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(catColor.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = cat,
                                    color = palette.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$noteCount ${if (noteCount == 1) "note" else "notes"}",
                                    color = catColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = palette.subtext,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        } else if (state.filteredNotes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                    contentDescription = null,
                    tint = palette.border,
                    modifier = Modifier.size(50.dp),
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = if (state.activeTab == NotesTab.CATEGORIES) {
                        "No notes in \"${state.selectedCategory}\""
                    } else {
                        stringResource(R.string.no_notes_found)
                    },
                    color = palette.subtext,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.filteredNotes, key = { it.id }) { note ->
                    val copyText = if (note.content.isNotBlank()) note.content else note.title
                    val noteCategoryColor = note.category?.takeIf { it.isNotBlank() }?.let { cat ->
                        categoryColorMap[cat] ?: fallbackCategoryColor(cat)
                    }
                    NoteListCard(
                        note = note,
                        categoryColor = noteCategoryColor,
                        onEdit = { onEditNoteClick(note.id) },
                        onPickCategory = {
                            isCategoryEditMode = false
                            categoryModalNote = note
                        },
                        onCopy = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Note", copyText))
                        },
                        onDelete = { deleteNoteTarget = note },
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // Category Picker & Edit Modal
    categoryModalNote?.let { targetNote ->
        AlertDialog(
            onDismissRequest = { categoryModalNote = null },
            containerColor = palette.card,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Select Category", color = palette.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { isCategoryEditMode = !isCategoryEditMode }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit categories",
                            tint = if (isCategoryEditMode) palette.accent else palette.subtext,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isCategoryEditMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                placeholder = { Text("New category name", color = palette.subtext, fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.accent,
                                    unfocusedBorderColor = palette.border,
                                    focusedTextColor = palette.title,
                                    unfocusedTextColor = palette.title,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    viewModel.addCategory(newCategoryName)
                                    newCategoryName = ""
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text("Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        state.categories.forEach { cat ->
                            val catColor = categoryColorMap[cat] ?: fallbackCategoryColor(cat)
                            val isSelected = targetNote.category == cat
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setNoteCategory(targetNote.id, cat)
                                        categoryModalNote = null
                                        isCategoryEditMode = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(catColor),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cat,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) catColor else palette.title,
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                                if (isCategoryEditMode) {
                                    IconButton(
                                        onClick = { viewModel.deleteCategory(cat) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete category",
                                            tint = palette.delete,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = palette.border)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { categoryModalNote = null }) {
                    Text(stringResource(R.string.action_cancel), color = palette.subtext, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

    // Delete Note Confirm Modal
    deleteNoteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteNoteTarget = null },
            containerColor = palette.card,
            title = { Text("Delete Note", color = palette.title, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this note?", color = palette.subtext) },
            dismissButton = {
                TextButton(onClick = { deleteNoteTarget = null }) {
                    Text(stringResource(R.string.action_cancel), color = palette.subtext)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(target.id)
                        deleteNoteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.delete),
                ) {
                    Text(stringResource(R.string.action_delete), color = Color.White)
                }
            },
        )
    }
}

@Composable
private fun NoteListCard(
    note: NoteEntity,
    categoryColor: Color?,
    onEdit: () -> Unit,
    onPickCategory: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = RemainderrTheme.palette
    var menuExpanded by remember { mutableStateOf(false) }

    val displayBody = remember(note.title, note.content) {
        when {
            note.content.isNotBlank() && note.title.isNotBlank() && note.title != "Untitled Note" ->
                "${note.title}\n${note.content}"
            note.content.isNotBlank() -> note.content
            note.title != "Untitled Note" -> note.title
            else -> ""
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.card),
        border = BorderStroke(
            width = if (categoryColor != null) 1.5.dp else 1.dp,
            color = categoryColor?.copy(alpha = 0.55f) ?: palette.border,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("note_card_${note.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            if (categoryColor != null) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(categoryColor),
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = displayBody,
                    color = palette.title,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 6.dp, end = 8.dp),
                )
                NoteCardMenuButton(
                    menuExpanded = menuExpanded,
                    onOpenMenu = { menuExpanded = true },
                    onDismissMenu = { menuExpanded = false },
                    onPickCategory = onPickCategory,
                    onCopy = onCopy,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun NoteCardMenuButton(
    menuExpanded: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onPickCategory: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = RemainderrTheme.palette
    Box {
        IconButton(
            onClick = onOpenMenu,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Note menu",
                tint = palette.subtext,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = onDismissMenu,
            containerColor = palette.card,
        ) {
            DropdownMenuItem(
                text = { Text("Category", color = palette.title, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = palette.title) },
                onClick = { onDismissMenu(); onPickCategory() },
            )
            DropdownMenuItem(
                text = { Text("Copy", color = palette.title, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = palette.title) },
                onClick = { onDismissMenu(); onCopy() },
            )
            HorizontalDivider(color = palette.border)
            DropdownMenuItem(
                text = { Text("Delete", color = palette.delete, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = palette.delete) },
                onClick = { onDismissMenu(); onDelete() },
            )
        }
    }
}
