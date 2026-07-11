package com.example.ui.vault

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.VaultViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryVaultScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val connections by viewModel.connections.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val files by viewModel.files.collectAsState()
    val journals by viewModel.dailyJournals.collectAsState()
    val pdfs by viewModel.allPdfs.collectAsState()

    var showAddConnectionDialog by remember { mutableStateOf(false) }
    var connectionLabel by remember { mutableStateOf("") }

    // Selected source
    var sourceId by remember { mutableStateOf(0L) }
    var sourceType by remember { mutableStateOf("NOTE") } // NOTE, MOVIE, PHOTO, JOURNAL, PDF

    // Selected target
    var targetId by remember { mutableStateOf(0L) }
    var targetType by remember { mutableStateOf("PHOTO") } // NOTE, MOVIE, PHOTO, JOURNAL, PDF

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Memory Vault 💖", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("memory_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Link Memories") },
                icon = { Icon(Icons.Default.Link, contentDescription = null) },
                onClick = {
                    showAddConnectionDialog = true
                    connectionLabel = ""
                    sourceId = notes.firstOrNull()?.id ?: 0L
                    sourceType = "NOTE"
                    targetId = files.firstOrNull { it.fileType == "PHOTO" }?.id ?: 0L
                    targetType = "PHOTO"
                },
                modifier = Modifier.testTag("add_memory_connection_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Your Second Brain Linker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Everything you save in the vault should grow into a linked memory. Link romance movies with photos, diaries with notes, and study files together naturally.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (connections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No memory connections established yet.", fontWeight = FontWeight.Bold)
                        Text("Tap 'Link Memories' below to connect your items!", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(connections) { conn ->
                        val sourceName = getMemoryItemName(conn.sourceType, conn.sourceId, notes, movies, files, journals, pdfs)
                        val targetName = getMemoryItemName(conn.targetType, conn.targetId, notes, movies, files, journals, pdfs)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Icon(
                                        imageVector = getMemoryIcon(conn.sourceType),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        sourceName,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.deleteMemoryConnection(conn) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Link", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = conn.label.ifBlank { "Linked memory relation" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getMemoryIcon(conn.targetType),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        targetName,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddConnectionDialog) {
        AlertDialog(
            onDismissRequest = { showAddConnectionDialog = false },
            title = { Text("Link Two Memories Together") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text("Relation / Connection Tag", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = connectionLabel,
                            onValueChange = { connectionLabel = it },
                            placeholder = { Text("e.g. Photo taken while reviewing this, Romance movie inspiration") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Source
                    item {
                        Text("Connect From (Source):", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("NOTE", "MOVIE", "PHOTO", "JOURNAL", "PDF").forEach { t ->
                                FilterChip(
                                    selected = sourceType == t,
                                    onClick = {
                                        sourceType = t
                                        sourceId = getFirstIdForType(t, notes, movies, files, journals, pdfs)
                                    },
                                    label = { Text(t) }
                                )
                            }
                        }
                    }

                    item {
                        val sourceItems = getItemsForType(sourceType, notes, movies, files, journals, pdfs)
                        if (sourceItems.isEmpty()) {
                            Text("No items of type $sourceType found in your secure vault.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Select specific item:", fontWeight = FontWeight.SemiBold)
                            Column {
                                sourceItems.forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { sourceId = item.first }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(selected = sourceId == item.first, onClick = { sourceId = item.first })
                                        Text(item.second, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Target
                    item {
                        Text("Connect To (Target):", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("NOTE", "MOVIE", "PHOTO", "JOURNAL", "PDF").forEach { t ->
                                FilterChip(
                                    selected = targetType == t,
                                    onClick = {
                                        targetType = t
                                        targetId = getFirstIdForType(t, notes, movies, files, journals, pdfs)
                                    },
                                    label = { Text(t) }
                                )
                            }
                        }
                    }

                    item {
                        val targetItems = getItemsForType(targetType, notes, movies, files, journals, pdfs)
                        if (targetItems.isEmpty()) {
                            Text("No items of type $targetType found in your secure vault.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Select specific item:", fontWeight = FontWeight.SemiBold)
                            Column {
                                targetItems.forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { targetId = item.first }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(selected = targetId == item.first, onClick = { targetId = item.first })
                                        Text(item.second, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sourceId != 0L && targetId != 0L) {
                            viewModel.addMemoryConnection(connectionLabel, sourceId, sourceType, targetId, targetType)
                            showAddConnectionDialog = false
                        }
                    },
                    enabled = sourceId != 0L && targetId != 0L
                ) {
                    Text("Link Memories")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddConnectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun getMemoryIcon(type: String): ImageVector {
    return when (type) {
        "NOTE" -> Icons.Default.Description
        "MOVIE" -> Icons.Default.Movie
        "PHOTO" -> Icons.Default.Photo
        "JOURNAL" -> Icons.Default.Book
        "PDF" -> Icons.Default.PictureAsPdf
        else -> Icons.Default.Help
    }
}

fun getMemoryItemName(
    type: String,
    id: Long,
    notes: List<VaultNote>,
    movies: List<MovieJournal>,
    files: List<VaultFile>,
    journals: List<DailyJournal>,
    pdfs: List<StudyPdf>
): String {
    return when (type) {
        "NOTE" -> notes.find { it.id == id }?.title?.let { "Note: $it" } ?: "Encrypted Note"
        "MOVIE" -> movies.find { it.id == id }?.title ?: "Movie Journal"
        "PHOTO" -> files.find { it.id == id }?.fileName ?: "Encrypted Image Asset"
        "JOURNAL" -> journals.find { it.id == id }?.dateString?.let { "Diary: $it" } ?: "Journal entry"
        "PDF" -> pdfs.find { it.id == id }?.title ?: "Study Document"
        else -> "Unknown Memory Item"
    }
}

fun getFirstIdForType(
    type: String,
    notes: List<VaultNote>,
    movies: List<MovieJournal>,
    files: List<VaultFile>,
    journals: List<DailyJournal>,
    pdfs: List<StudyPdf>
): Long {
    return when (type) {
        "NOTE" -> notes.firstOrNull()?.id ?: 0L
        "MOVIE" -> movies.firstOrNull()?.id ?: 0L
        "PHOTO" -> files.firstOrNull { it.fileType == "PHOTO" }?.id ?: 0L
        "JOURNAL" -> journals.firstOrNull()?.id ?: 0L
        "PDF" -> pdfs.firstOrNull()?.id ?: 0L
        else -> 0L
    }
}

fun getItemsForType(
    type: String,
    notes: List<VaultNote>,
    movies: List<MovieJournal>,
    files: List<VaultFile>,
    journals: List<DailyJournal>,
    pdfs: List<StudyPdf>
): List<Pair<Long, String>> {
    return when (type) {
        "NOTE" -> notes.map { it.id to (it.title.ifBlank { "Untitled Note" }) }
        "MOVIE" -> movies.map { it.id to it.title }
        "PHOTO" -> files.filter { it.fileType == "PHOTO" }.map { it.id to it.fileName }
        "JOURNAL" -> journals.map { it.id to "${it.dateString} (${it.mood})" }
        "PDF" -> pdfs.map { it.id to it.title }
        else -> emptyList()
    }
}
