package com.example.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RemainderrTheme

@Composable
fun AddNoteScreen(
    noteId: String?,
    viewModel: NotesViewModel,
    onBack: () -> Unit,
) {
    val palette = RemainderrTheme.palette

    var content by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }

    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            val existing = viewModel.getNoteById(noteId)
            if (existing != null) {
                content = when {
                    existing.title.isNotBlank() && existing.title != "Untitled Note" && existing.content.isNotBlank() ->
                        "${existing.title}\n${existing.content}"
                    existing.title.isNotBlank() && existing.title != "Untitled Note" ->
                        existing.title
                    else -> existing.content
                }
                isFavorite = existing.isFavorite
            }
        }
    }

    val updateContentWithHistory: (String) -> Unit = { next ->
        if (next != content) {
            undoStack.add(content)
            redoStack.clear()
            content = next
        }
    }

    val saveAndBack = {
        viewModel.saveNote(noteId, "", content, isFavorite, onBack)
    }

    BackHandler { saveAndBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = saveAndBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("note_back_save_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = palette.title,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("note_favorite_button"),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (isFavorite) palette.accent else palette.subtext,
                    )
                }
                IconButton(
                    onClick = saveAndBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("note_save_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.action_save),
                        tint = palette.accent,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            // Content Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp),
            ) {
                if (content.isEmpty()) {
                    Text(
                        text = "Tap to add text",
                        color = palette.subtext,
                        fontSize = 17.sp,
                        lineHeight = 25.sp,
                    )
                }
                BasicTextField(
                    value = content,
                    onValueChange = updateContentWithHistory,
                    textStyle = TextStyle(
                        color = palette.title,
                        fontSize = 17.sp,
                        lineHeight = 25.sp,
                    ),
                    cursorBrush = SolidColor(palette.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input"),
                )
            }
        }

        // Bottom Formatting & Undo/Redo Toolbar
        HorizontalDivider(color = palette.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { updateContentWithHistory(content + if (content.isEmpty() || content.endsWith("\n")) "# " else "\n# ") }) {
                Icon(Icons.Filled.Title, contentDescription = "Heading", tint = palette.subtext)
            }
            IconButton(onClick = { updateContentWithHistory("$content**bold**") }) {
                Icon(Icons.Filled.FormatBold, contentDescription = "Bold", tint = palette.subtext)
            }
            IconButton(onClick = { updateContentWithHistory("$content*italic*") }) {
                Icon(Icons.Filled.FormatItalic, contentDescription = "Italic", tint = palette.subtext)
            }
            IconButton(onClick = { updateContentWithHistory("${content}__underline__") }) {
                Icon(Icons.Filled.FormatUnderlined, contentDescription = "Underline", tint = palette.subtext)
            }
            IconButton(
                onClick = {
                    if (undoStack.isNotEmpty()) {
                        redoStack.add(content)
                        content = undoStack.removeAt(undoStack.lastIndex)
                    }
                },
                enabled = undoStack.isNotEmpty(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (undoStack.isNotEmpty()) palette.title else palette.subtext.copy(alpha = 0.4f),
                )
            }
            IconButton(
                onClick = {
                    if (redoStack.isNotEmpty()) {
                        undoStack.add(content)
                        content = redoStack.removeAt(redoStack.lastIndex)
                    }
                },
                enabled = redoStack.isNotEmpty(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (redoStack.isNotEmpty()) palette.title else palette.subtext.copy(alpha = 0.4f),
                )
            }
        }
    }
}
