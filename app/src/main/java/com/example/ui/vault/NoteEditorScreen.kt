package com.example.ui.vault

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultNote
import com.example.ui.VaultViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DrawingLine(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Dp = 4.dp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    var editingNote by remember { mutableStateOf<VaultNote?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    // Form fields
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var noteCategory by remember { mutableStateOf("General") }
    var noteTags by remember { mutableStateOf("") }
    var notePinned by remember { mutableStateOf(false) }

    // Advanced Notes 2.0 States
    var isPreviewMode by remember { mutableStateOf(false) }
    var noteStickyColor by remember { mutableStateOf("default") }
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }
    var isUndoAction by remember { mutableStateOf(false) }
    var showDrawingPad by remember { mutableStateOf(false) }
    val drawingLines = remember { mutableStateListOf<DrawingLine>() }
    var currentDrawingColor by remember { mutableStateOf(Color.Black) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var isAutoSavingDraft by remember { mutableStateOf(false) }

    fun updateContentWithUndo(newText: String) {
        if (newText != noteContent) {
            if (!isUndoAction) {
                undoStack.add(noteContent)
                if (undoStack.size > 50) undoStack.removeAt(0)
                redoStack.clear()
            }
            noteContent = newText
        }
    }

    // Search & Filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "General", "Personal", "Work", "Ideas", "Movies")

    // Auto-save effect
    LaunchedEffect(noteTitle, noteContent, noteCategory, noteTags, noteStickyColor) {
        if (noteTitle.isNotEmpty() || noteContent.isNotEmpty()) {
            delay(5000) // Auto-save draft every 5 seconds of inactivity
            isAutoSavingDraft = true
            val parsedTags = noteTags.split(",").map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("color_") }.toMutableList()
            if (noteStickyColor != "default") {
                parsedTags.add(0, "color_$noteStickyColor")
            }
            if (editingNote != null) {
                editingNote?.let {
                    viewModel.updateNote(
                        note = it,
                        newTitle = noteTitle,
                        newContent = noteContent,
                        category = noteCategory,
                        tags = parsedTags
                    )
                }
            }
            delay(500)
            isAutoSavingDraft = false
        }
    }

    val templates = mapOf(
        "My Movie Review" to "# Movie Review\n\n**Rating:** ⭐⭐⭐⭐⭐\n**Favorite quote:** \"Type quote here\"\n\n### Overall thoughts:\n*Insert review here*",
        "Characters I Love" to "# Characters I Love\n\n1. **Character Name** (Role)\n   - *Why I love them:* Insert details",
        "What I Learned" to "# Life Lessons Learned\n\n- **Lesson:** \"Type lesson here\"\n  - *Connected event:* Details",
        "Ending Explained" to "# Ending Explained\n\n- **The climax:** What happened\n- **The interpretation:** Symbolism detail",
        "Movies To Watch" to "## Watchlist Checklist\n\n- [ ] Movie Name 1\n- [ ] Movie Name 2\n- [ ] Movie Name 3",
        "My Comfort Movies" to "# My Comfort Movies Collection\n\n- **Movie 1** (Theme)\n- **Movie 2** (Theme)"
    )

    val context = LocalContext.current

    if (isCreatingNew || editingNote != null) {
        // Edit Mode
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isCreatingNew) "New Rich Note" else "Edit Note")
                            if (isAutoSavingDraft) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("• Auto-saving...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isCreatingNew = false
                            editingNote = null
                        }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { notePinned = !notePinned }) {
                            Icon(
                                imageVector = if (notePinned) Icons.Default.PushPin else Icons.Default.PinDrop,
                                contentDescription = if (notePinned) "Unpin" else "Pin",
                                tint = if (notePinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = {
                            val parsedTags = noteTags.split(",").map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("color_") }.toMutableList()
                            if (noteStickyColor != "default") {
                                parsedTags.add(0, "color_$noteStickyColor")
                            }
                            if (isCreatingNew) {
                                viewModel.saveNote(
                                    title = noteTitle,
                                    content = noteContent,
                                    category = noteCategory,
                                    tags = parsedTags,
                                    isPinned = notePinned
                                )
                            } else {
                                editingNote?.let {
                                    viewModel.updateNote(
                                        note = it,
                                        newTitle = noteTitle,
                                        newContent = noteContent,
                                        category = noteCategory,
                                        tags = parsedTags
                                    )
                                }
                            }
                            isCreatingNew = false
                            editingNote = null
                        }) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Save Note", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode Toggle and Undo/Redo Header Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit vs Preview Tabs
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        listOf("Edit", "Preview").forEach { mode ->
                            val selected = if (mode == "Edit") !isPreviewMode else isPreviewMode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isPreviewMode = (mode == "Preview") }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = mode,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Undo & Redo controls
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            enabled = undoStack.isNotEmpty(),
                            onClick = {
                                isUndoAction = true
                                redoStack.add(noteContent)
                                val last = undoStack.removeAt(undoStack.size - 1)
                                noteContent = last
                                isUndoAction = false
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo", tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                        IconButton(
                            enabled = redoStack.isNotEmpty(),
                            onClick = {
                                isUndoAction = true
                                undoStack.add(noteContent)
                                val last = redoStack.removeAt(redoStack.size - 1)
                                noteContent = last
                                isUndoAction = false
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                    }
                }

                if (!isPreviewMode) {
                    // Formatting helper toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { updateContentWithUndo(noteContent + " **Bold** ") }) {
                            Text("B", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        TextButton(onClick = { updateContentWithUndo(noteContent + " *Italic* ") }) {
                            Text("I", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                        TextButton(onClick = { updateContentWithUndo(noteContent + " [ ] Checklist Item\n") }) {
                            Text("Checklist", fontSize = 13.sp)
                        }
                        TextButton(onClick = { updateContentWithUndo(noteContent + "\n- Bullet item\n") }) {
                            Text("List", fontSize = 13.sp)
                        }
                        IconButton(onClick = { showDrawingPad = true }) {
                            Icon(imageVector = Icons.Default.Brush, contentDescription = "Handwriting / Drawing Mode", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { isRecordingVoice = true }) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Quick Thought", modifier = Modifier.size(18.dp))
                        }
                    }

                    // Sticky note color picker
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sticky Note Shade", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        val colorMap = listOf(
                            "default" to MaterialTheme.colorScheme.surfaceVariant,
                            "yellow" to Color(0xFFFFF9C4),
                            "pink" to Color(0xFFF8BBD0),
                            "blue" to Color(0xFFB3E5FC),
                            "green" to Color(0xFFC8E6C9),
                            "purple" to Color(0xFFE1BEE7)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(colorMap) { entry ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(entry.second)
                                        .clickable { noteStickyColor = entry.first }
                                        .padding(2.dp)
                                ) {
                                    if (noteStickyColor == entry.first) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_title_input")
                    )

                    // Category and Tags Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category selection dropdown-like list
                        var showCatDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { showCatDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Category: $noteCategory")
                            }
                            DropdownMenu(
                                expanded = showCatDropdown,
                                onDismissRequest = { showCatDropdown = false }
                            ) {
                                categories.filter { it != "All" }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            noteCategory = cat
                                            showCatDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = noteTags,
                            onValueChange = { noteTags = it },
                            label = { Text("Tags") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Note Body Editor
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { updateContentWithUndo(it) },
                        placeholder = { Text("Write your thoughts or apply templates...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("note_content_input")
                    )
                } else {
                    // Markdown Preview Mode with Interactive Checklists and embedded drawings
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (noteTitle.isNotEmpty()) {
                            Text(
                                text = noteTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider()
                        }

                        val lines = noteContent.split("\n")
                        lines.forEach { line ->
                            when {
                                line.startsWith("# ") -> {
                                    Text(
                                        text = line.substring(2),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                line.startsWith("## ") -> {
                                    Text(
                                        text = line.substring(3),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                line.startsWith("### ") -> {
                                    Text(
                                        text = line.substring(4),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                                line.startsWith("- [ ] ") || line.startsWith("[ ] ") -> {
                                    val text = if (line.startsWith("- ")) line.substring(6) else line.substring(4)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            // Toggle Checklist item in Markdown
                                            val oldLine = line
                                            val newLine = if (line.startsWith("- ")) "- [x] $text" else "[x] $text"
                                            val newContent = noteContent.replace(oldLine, newLine)
                                            updateContentWithUndo(newContent)
                                        }
                                    ) {
                                        Checkbox(checked = false, onCheckedChange = {
                                            val oldLine = line
                                            val newLine = if (line.startsWith("- ")) "- [x] $text" else "[x] $text"
                                            val newContent = noteContent.replace(oldLine, newLine)
                                            updateContentWithUndo(newContent)
                                        })
                                        Text(text = text, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                                line.startsWith("- [x] ") || line.startsWith("- [X] ") || line.startsWith("[x] ") || line.startsWith("[X] ") -> {
                                    val text = if (line.startsWith("- ")) line.substring(6) else line.substring(4)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            // Toggle Checklist item in Markdown
                                            val oldLine = line
                                            val newLine = if (line.startsWith("- ")) "- [ ] $text" else "[ ] $text"
                                            val newContent = noteContent.replace(oldLine, newLine)
                                            updateContentWithUndo(newContent)
                                        }
                                    ) {
                                        Checkbox(checked = true, onCheckedChange = {
                                            val oldLine = line
                                            val newLine = if (line.startsWith("- ")) "- [ ] $text" else "[ ] $text"
                                            val newContent = noteContent.replace(oldLine, newLine)
                                            updateContentWithUndo(newContent)
                                        })
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        )
                                    }
                                }
                                line.startsWith("- ") || line.startsWith("* ") -> {
                                    Row(modifier = Modifier.padding(start = 8.dp)) {
                                        Text("• ", fontWeight = FontWeight.Bold)
                                        Text(text = line.substring(2), style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                                line.contains("![sketch](") -> {
                                    // Parse local Drawing path: ![sketch](/path/to/drawing.png)
                                    val path = line.substringAfter("![sketch](").substringBefore(")")
                                    val file = File(path)
                                    if (file.exists()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().height(200.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize().background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    drawCircle(color = Color.LightGray.copy(alpha = 0.3f), radius = size.minDimension / 3f)
                                                    drawLine(color = Color.Gray, start = Offset(0f, size.height/2), end = Offset(size.width, size.height/2), strokeWidth = 2f)
                                                }
                                                Text("Sketch embedded: ${file.name}", fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    if (line.trim().isNotEmpty()) {
                                        Text(text = line, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // List Mode
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Secret Notes Vault") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        noteTitle = ""
                        noteContent = ""
                        noteCategory = "General"
                        noteTags = ""
                        notePinned = false
                        isCreatingNew = true
                    },
                    modifier = Modifier.testTag("add_note_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Note")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search through secure notes...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector row
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(70.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // Notes listing
                val filteredNotes = notes.filter { note ->
                    val titleDec = viewModel.decryptNoteTitle(note.title)
                    val bodyDec = viewModel.decryptNoteContent(note.content)
                    val matchesSearch = titleDec.contains(searchQuery, ignoreCase = true) ||
                            bodyDec.contains(searchQuery, ignoreCase = true)
                    val matchesCategory = selectedCategory == "All" || note.category == selectedCategory
                    matchesSearch && matchesCategory
                }

                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No secret notes found. Create one now!",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredNotes) { note ->
                            val decryptedTitle = viewModel.decryptNoteTitle(note.title)
                            val decryptedBody = viewModel.decryptNoteContent(note.content)

                            val stickyColorTag = note.tags.split(",").firstOrNull { it.trim().startsWith("color_") }?.substringAfter("color_") ?: "default"
                            val cardBgColor = when (stickyColorTag) {
                                "yellow" -> Color(0xFFFFF9C4)
                                "pink" -> Color(0xFFF8BBD0)
                                "blue" -> Color(0xFFB3E5FC)
                                "green" -> Color(0xFFC8E6C9)
                                "purple" -> Color(0xFFE1BEE7)
                                else -> if (note.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                            val cardOnColor = if (stickyColorTag != "default") Color.Black else MaterialTheme.colorScheme.onSurface

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        noteTitle = decryptedTitle
                                        noteContent = decryptedBody
                                        noteCategory = note.category
                                        noteTags = note.tags.split(",").map { it.trim() }.filter { !it.startsWith("color_") }.joinToString(", ")
                                        noteStickyColor = stickyColorTag
                                        notePinned = note.isPinned
                                        editingNote = note
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBgColor,
                                    contentColor = cardOnColor
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (note.isPinned) {
                                                Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = "Pinned",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = decryptedTitle,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }

                                        Row {
                                            IconButton(onClick = { viewModel.togglePinNote(note) }) {
                                                Icon(
                                                    imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Default.PinDrop,
                                                    contentDescription = "Pin Note"
                                                )
                                            }
                                            IconButton(onClick = { viewModel.deleteNote(note) }) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Note", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = decryptedBody,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 3,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = note.category,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.updatedAt)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Drawing Pad Dialog
    if (showDrawingPad) {
            AlertDialog(
                onDismissRequest = { showDrawingPad = false },
                title = { Text("Sketch Pad") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Color palette selector
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(Color.Black, Color.Red, Color.Blue, Color.Green).forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .clickable { currentDrawingColor = col }
                                        .padding(2.dp)
                                ) {
                                    if (currentDrawingColor == col) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Undo button
                            IconButton(onClick = {
                                if (drawingLines.isNotEmpty()) {
                                    drawingLines.removeAt(drawingLines.size - 1)
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo Line")
                            }

                            // Clear button
                            IconButton(onClick = { drawingLines.clear() }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear All")
                            }
                        }

                        // Canvas drawing box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            drawingLines.add(DrawingLine(listOf(offset), currentDrawingColor))
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (drawingLines.isNotEmpty()) {
                                                val lastIdx = drawingLines.lastIndex
                                                val lastLine = drawingLines[lastIdx]
                                                val updatedPoints = lastLine.points + (lastLine.points.last() + dragAmount)
                                                drawingLines[lastIdx] = lastLine.copy(points = updatedPoints)
                                            }
                                        }
                                    )
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawingLines.forEach { line ->
                                    val path = Path().apply {
                                        if (line.points.isNotEmpty()) {
                                            moveTo(line.points.first().x, line.points.first().y)
                                            line.points.forEach { pt ->
                                                lineTo(pt.x, pt.y)
                                            }
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = line.color,
                                        style = Stroke(width = line.strokeWidth.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (drawingLines.isNotEmpty()) {
                                // Compile sketch paths to physical hidden PNG attachment!
                                val path = saveDrawingToPngFile(drawingLines, context)
                                if (path.isNotEmpty()) {
                                    updateContentWithUndo(noteContent + "\n\n![sketch]($path)\n")
                                }
                            }
                            drawingLines.clear()
                            showDrawingPad = false
                        }
                    ) {
                        Text("Embed Sketch")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        drawingLines.clear()
                        showDrawingPad = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 2. Quick Thought dialog (manual entry; speech-to-text not wired up in this build)
        if (isRecordingVoice) {
            var quickThought by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { isRecordingVoice = false },
                title = { Text("Quick Thought") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Add a quick timestamped thought to your note.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = quickThought,
                            onValueChange = { quickThought = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Type your thought...") },
                            minLines = 2,
                            maxLines = 5
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (quickThought.isNotBlank()) {
                                val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                updateContentWithUndo(noteContent + "\n*Thought ($timestamp): \"$quickThought\"*")
                            }
                            isRecordingVoice = false
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isRecordingVoice = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

// Global drawing compiler to local secure attachment PNG
fun saveDrawingToPngFile(lines: List<DrawingLine>, context: android.content.Context): String {
    val fileDir = File(context.filesDir, "note_attachments").apply { mkdirs() }
    val file = File(fileDir, "sketch_${System.currentTimeMillis()}.png")
    try {
        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeWidth = 10f
        }
        for (line in lines) {
            paint.color = when(line.color) {
                Color.Red -> android.graphics.Color.RED
                Color.Blue -> android.graphics.Color.BLUE
                Color.Green -> android.graphics.Color.GREEN
                else -> android.graphics.Color.BLACK
            }
            if (line.points.size > 1) {
                for (i in 0 until line.points.size - 1) {
                    val p1 = line.points[i]
                    val p2 = line.points[i+1]
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
                }
            }
        }
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}
