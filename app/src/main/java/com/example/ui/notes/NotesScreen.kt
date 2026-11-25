package com.example.ui.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.NoteEntity
import com.example.ui.theme.RemainderrTheme
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    var backupSheetVisible by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { viewModel.backupNotes(it) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { viewModel.restoreNotes(it) } }

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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(palette.card)
                        .border(1.dp, palette.border, CircleShape)
                        .clickable { backupSheetVisible = true }
                        .testTag("notes_backup_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = stringResource(R.string.backup_and_restore),
                        tint = palette.title,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
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
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 15.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(palette.card)
                .border(1.dp, palette.border, RoundedCornerShape(23.dp))
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

        // Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .padding(bottom = 15.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            val tabs = listOf(
                NotesTab.ALL to stringResource(R.string.all_notes_tab),
                NotesTab.FAVORITES to stringResource(R.string.favorites_tab),
                NotesTab.CATEGORIES to stringResource(R.string.categories_tab),
            )
            tabs.forEach { (tab, label) ->
                val active = state.activeTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.setTab(tab) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
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
                                .width(64.dp)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Category: ")
                        withStyle(SpanStyle(color = palette.accent)) {
                            append(state.selectedCategory)
                        }
                    },
                    color = palette.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.card)
                        .border(1.dp, palette.border, RoundedCornerShape(20.dp))
                        .clickable { viewModel.selectCategory(null) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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

        // Content: either Category List or Masonry Notes Grid
        if (state.activeTab == NotesTab.CATEGORIES && state.selectedCategory == null) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.categories, key = { it }) { cat ->
                    val noteCount = state.allNotes.count { it.category == cat }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.card),
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectCategory(cat) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(22.dp),
                                )
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
                                    color = palette.subtext,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = palette.subtext,
                                    modifier = Modifier.size(16.dp),
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
                    NoteListCard(
                        note = note,
                        onToggleFavorite = { viewModel.toggleFavorite(note) },
                        onEdit = { onEditNoteClick(note.id) },
                        onPickCategory = {
                            isCategoryEditMode = false
                            categoryModalNote = note
                        },
                        onToggleComplete = { viewModel.toggleCompleted(note) },
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
                                    Text(
                                        text = cat,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (targetNote.category == cat) palette.accent else palette.title,
                                    )
                                    if (targetNote.category == cat) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = palette.accent,
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

    // Bottom Sheet Backup & Restore
    if (backupSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { backupSheetVisible = false },
            containerColor = palette.card,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text(
                    text = stringResource(R.string.backup_and_restore),
                    color = palette.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            backupSheetVisible = false
                            backupLauncher.launch(null)
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.FileUpload, contentDescription = null, tint = palette.accent)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Backup Data", color = palette.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Save notes and categories to local storage", color = palette.subtext, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = palette.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            backupSheetVisible = false
                            restoreLauncher.launch(null)
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = palette.accent)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Restore Data", color = palette.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Overwrite local data from a backup folder", color = palette.subtext, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    state.statusMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearStatusMessage() },
            containerColor = palette.card,
            title = { Text("Notes", color = palette.title, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = palette.subtext) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearStatusMessage() }) {
                    Text("OK", color = palette.accent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun NoteListCard(
    note: NoteEntity,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onPickCategory: () -> Unit,
    onToggleComplete: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = RemainderrTheme.palette
    var menuExpanded by remember { mutableStateOf(false) }

    val timeLabel = remember(note.createdAt) { formatRelativeTime(note.createdAt) }
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
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (note.isCompleted) 0.6f else 1f)
            .clickable(onClick = onEdit)
            .testTag("note_card_${note.id}"),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = timeLabel,
                        color = palette.subtext,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!note.category.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.accent.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = note.category,
                                color = palette.accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) palette.accent else palette.subtext,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(40.dp),
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
                            onDismissRequest = { menuExpanded = false },
                            containerColor = palette.card,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Note", color = palette.title, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = palette.title) },
                                onClick = { menuExpanded = false; onEdit() },
                            )
                            DropdownMenuItem(
                                text = { Text("Category", color = palette.title, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = palette.title) },
                                onClick = { menuExpanded = false; onPickCategory() },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (note.isCompleted) "Mark Incomplete" else "Mark Complete",
                                        color = palette.title,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (note.isCompleted) Icons.AutoMirrored.Filled.Undo else Icons.Filled.CheckCircleOutline,
                                        contentDescription = null,
                                        tint = palette.title,
                                    )
                                },
                                onClick = { menuExpanded = false; onToggleComplete() },
                            )
                            DropdownMenuItem(
                                text = { Text("Copy", color = palette.title, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = palette.title) },
                                onClick = { menuExpanded = false; onCopy() },
                            )
                            HorizontalDivider(color = palette.border)
                            DropdownMenuItem(
                                text = { Text("Delete", color = palette.delete, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = palette.delete) },
                                onClick = { menuExpanded = false; onDelete() },
                            )
                        }
                    }
                }
            }

            Text(
                text = displayBody,
                color = palette.title,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
    }
}

private fun formatRelativeTime(isoDate: String): String {
    val instant = runCatching { Instant.parse(isoDate) }.getOrNull() ?: return ""
    val diffSeconds = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)
    return when {
        diffSeconds < 60 -> "Just now"
        diffSeconds < 3600 -> "${diffSeconds / 60} hour ago"
        diffSeconds < 86400 -> "${diffSeconds / 3600} hours ago"
        else -> instant.atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }
}
