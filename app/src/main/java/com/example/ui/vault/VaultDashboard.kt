package com.example.ui.vault

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VaultDashboard(
    viewModel: VaultViewModel,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDailyJournal: () -> Unit,
    onNavigateToTvSeries: () -> Unit,
    onNavigateToStudySpace: () -> Unit,
    onNavigateToMemoryVault: () -> Unit
) {
    val isDecoy by viewModel.isDecoyUnlocked.collectAsState()
    val files by viewModel.files.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val dailyJournals by viewModel.dailyJournals.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val driveConnected by viewModel.googleDriveConnected.collectAsState()
    val driveAccount by viewModel.googleAccountName.collectAsState()

    val tvSeries by viewModel.tvSeries.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val planners by viewModel.planners.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val connections by viewModel.connections.collectAsState()

    // Customized widgets visibility configuration
    val visibleWidgets by viewModel.visibleWidgets.collectAsState()
    val customCollections by viewModel.collections.collectAsState()

    // Calculate dynamic file counts
    val photoCount = files.count { it.fileType == "PHOTO" }
    val videoCount = files.count { it.fileType == "VIDEO" }
    val docCount = files.count { it.fileType == "DOC" }
    val audioCount = files.count { it.fileType == "AUDIO" }

    // Storage calculation
    val totalSizeBytes = files.sumOf { it.fileSize } + (notes.size * 2048) + (movies.size * 8192)
    val totalSizeMb = String.format(Locale.US, "%.2f KB", totalSizeBytes / 1024.0)

    // Interactive Dialog & Bottom Sheet States
    var showLayoutCustomizer by remember { mutableStateOf(false) }
    var showTimelineSheet by remember { mutableStateOf(false) }
    var showCollectionsSheet by remember { mutableStateOf(false) }
    var showVoiceSearchOverlay by remember { mutableStateOf(false) }
    var showOcrScannerOverlay by remember { mutableStateOf(false) }
    var showDayDetailsDialog by remember { mutableStateOf<Date?>(null) }
    var showAddCollectionDialog by remember { mutableStateOf(false) }
    var selectedCollectionId by remember { mutableStateOf<Long?>(null) }

    // Quote state
    var quoteOffset by remember { mutableIntStateOf(0) }
    val currentQuote = viewModel.getInspirationalQuote(quoteOffset)

    // Universal Search query state
    var universalQuery by remember { mutableStateOf("") }
    val normalizedQuery = universalQuery.lowercase(Locale.getDefault()).trim()

    // Quick-search category chips (replaces the former simulated voice dictation).
    var voiceTextStream by remember { mutableStateOf("Listening...") }

    // Date calculations for greeting and calendar
    val calendarInstance = remember { Calendar.getInstance() }
    val currentHour = calendarInstance.get(Calendar.HOUR_OF_DAY)
    val currentYear = calendarInstance.get(Calendar.YEAR)
    val currentMonth = calendarInstance.get(Calendar.MONTH) // 0-indexed

    val greeting = when {
        currentHour < 12 -> "Good Morning 🌅"
        currentHour < 17 -> "Good Afternoon ☀️"
        else -> "Good Evening 🌙"
    }

    val currentMonthName = calendarInstance.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Month"

    // Helper to get items on a particular date for calendar details
    fun getItemsForDate(date: Date): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(date)

        // Notes matches
        notes.forEach { note ->
            val noteDate = sdf.format(Date(note.updatedAt))
            if (noteDate == dateStr) {
                list.add("Note" to viewModel.decryptNoteTitle(note.title))
            }
        }
        // Movies matches
        movies.forEach { movie ->
            if (movie.dateWatched == dateStr) {
                list.add("Movie" to movie.title)
            }
        }
        // Daily journals
        dailyJournals.forEach { journal ->
            if (journal.dateString == dateStr) {
                list.add("Journal" to "${journal.mood} Mental Check-In")
            }
        }
        return list
    }

    // Collections Covers Color Gradients
    val coverBrushes = listOf(
        Brush.linearGradient(colors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))), // Sunset Purple
        Brush.linearGradient(colors = listOf(Color(0xFF00c6ff), Color(0xFF0072ff))), // Cool Azure
        Brush.linearGradient(colors = listOf(Color(0xFFf12711), Color(0xFFf5af19))), // Radiant Gold
        Brush.linearGradient(colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))), // Forest Mint
        Brush.linearGradient(colors = listOf(Color(0xFFf857a6), Color(0xFFff5858)))  // Rose Blossom
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isDecoy) "Decoy Space" else "Secure Vault Pro",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isDecoy) "Decoy profile active" else "Primary AES-256 Storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDecoy) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLayoutCustomizer = true }) {
                        Icon(imageVector = Icons.Default.DashboardCustomize, contentDescription = "Customize Dashboard", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { viewModel.lockVault() }) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock App", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Widget 1: Personal Dynamic Greeting & Search Row
                if (visibleWidgets["Greeting"] == true) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "$greeting, Commander",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your digital footprint is completely shielded offline.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Universal Search & Quick Scans Bar
                            OutlinedTextField(
                                value = universalQuery,
                                onValueChange = { universalQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("universal_search_bar"),
                                placeholder = { Text("Universal deep search (notes, movies, files...)") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon")
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                        IconButton(onClick = {
                                            voiceTextStream = "Listening..."
                                            showVoiceSearchOverlay = true
                                        }) {
                                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Search")
                                        }
                                        IconButton(onClick = { showOcrScannerOverlay = true }) {
                                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "OCR Scanner")
                                        }
                                        if (universalQuery.isNotEmpty()) {
                                            IconButton(onClick = { universalQuery = "" }) {
                                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }

                // Inline Search Results View
                if (universalQuery.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Universal Search Results for: \"$universalQuery\"", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Filter across multiple tables
                                val filteredNotes = notes.filter {
                                    viewModel.decryptNoteTitle(it.title).lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                                            viewModel.decryptNoteContent(it.content).lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                                            it.tags.lowercase(Locale.getDefault()).contains(normalizedQuery)
                                }
                                val filteredMovies = movies.filter {
                                    it.title.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                                            it.watchPartner.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                                            it.favoriteCharacter.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                                            it.personalMemories.lowercase(Locale.getDefault()).contains(normalizedQuery)
                                }
                                val filteredFiles = files.filter {
                                    it.fileName.lowercase(Locale.getDefault()).contains(normalizedQuery)
                                }

                                if (filteredNotes.isEmpty() && filteredMovies.isEmpty() && filteredFiles.isEmpty()) {
                                    Text("No matching records found securely.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        filteredFiles.forEach { file ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onNavigateToCategory(file.fileType) }) {
                                                Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF4285F4))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("[File • ${file.fileType}] ${file.fileName}", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        filteredNotes.forEach { note ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onNavigateToNotes() }) {
                                                Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF9C27B0))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("[Note] ${viewModel.decryptNoteTitle(note.title)}", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        filteredMovies.forEach { movie ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onNavigateToMovies() }) {
                                                Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE91E63))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("[Movie] ${movie.title} (${movie.releaseYear})", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Widget 2: Shred-Safe Storage Card
                if (visibleWidgets["Storage"] == true) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Shred-Safe Storage", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(text = totalSizeMb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { (totalSizeBytes.coerceAtLeast(1L) / 10240000.0).toFloat().coerceIn(0.01f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Encrypting local files on-the-fly with military-grade AES-256 standard.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Portals Categories Grid (Always Displayed for Easy Entry)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CategoryCard(
                                title = "Photos",
                                count = "$photoCount Files",
                                icon = Icons.Default.Photo,
                                color = Color(0xFF4285F4),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory("PHOTO") }
                            )
                            CategoryCard(
                                title = "Videos",
                                count = "$videoCount Files",
                                icon = Icons.Default.VideoLibrary,
                                color = Color(0xFFEA4335),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory("VIDEO") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CategoryCard(
                                title = "Audio",
                                count = "$audioCount Files",
                                icon = Icons.Default.Audiotrack,
                                color = Color(0xFFFBBC05),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory("AUDIO") }
                            )
                            CategoryCard(
                                title = "Documents",
                                count = "$docCount Files",
                                icon = Icons.Default.FolderZip,
                                color = Color(0xFF34A853),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory("DOC") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CategoryCard(
                                title = "Advanced Notes",
                                count = "${notes.size} Rich Notes",
                                icon = Icons.Default.Description,
                                color = Color(0xFF9C27B0),
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToNotes
                            )
                            CategoryCard(
                                title = "Movie Journal",
                                count = "${movies.size} Movies",
                                icon = Icons.Default.Movie,
                                color = Color(0xFFE91E63),
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToMovies
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CategoryCard(
                                title = "Study Space",
                                count = "${subjects.size} Folders",
                                icon = Icons.Default.School,
                                color = Color(0xFF3F51B5),
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToStudySpace
                            )
                            CategoryCard(
                                title = "TV Series",
                                count = "${tvSeries.size} Series",
                                icon = Icons.Default.Tv,
                                color = Color(0xFF009688),
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToTvSeries
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CategoryCard(
                                title = "Memory Vault",
                                count = "${connections.size} Linked Memories",
                                icon = Icons.Default.Favorite,
                                color = Color(0xFFFF5722),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onNavigateToMemoryVault
                            )
                        }
                    }
                }

                // Widget 3: Quick Action Shortcuts
                if (visibleWidgets["QuickActions"] == true) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Quick Actions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigateToNotes() }) {
                                        Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Note", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Add Note", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigateToMovies() }) {
                                        Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.Movie, contentDescription = "Log Movie", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Log Movie", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigateToDailyJournal() }) {
                                        Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Mood", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Daily Mood", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showLayoutCustomizer = true }) {
                                        Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.DashboardCustomize, contentDescription = "Custom", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Custom", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                // Widget 4: Custom Beautiful Collections Widget
                if (visibleWidgets["Collections"] == true) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Custom Collections", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = { showCollectionsSheet = true }) {
                                    Text("Manage All")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(customCollections) { col ->
                                    val colGradient = coverBrushes[col.coverIndex % coverBrushes.size]
                                    Card(
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(180.dp)
                                            .clickable {
                                                selectedCollectionId = col.id
                                                showCollectionsSheet = true
                                            },
                                        shape = RoundedCornerShape(20.dp),
                                        border = if (col.isPinned) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(colGradient)
                                                .padding(12.dp)
                                        ) {
                                            if (col.isPinned) {
                                                Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = "Pinned",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp).align(Alignment.TopEnd)
                                                )
                                            }
                                            Text(text = col.emoji, fontSize = 32.sp, modifier = Modifier.align(Alignment.TopStart))
                                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                                Text(text = col.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(text = col.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                                item {
                                    Card(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(180.dp)
                                            .clickable { showAddCollectionDialog = true },
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Collection", tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Add New", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Widget 5: Interactive Calendar Widget
                if (visibleWidgets["Calendar"] == true) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$currentMonthName $currentYear",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Calendar Header Days
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                                        Text(text = day, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Render days of the month grid
                                val tempCal = remember { Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) } }
                                val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
                                val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                val currentDayActive = calendarInstance.get(Calendar.DAY_OF_MONTH)

                                var dayPointer = 1
                                Column {
                                    for (week in 0..5) {
                                        if (dayPointer > daysInMonth) break
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            for (dayIndex in 1..7) {
                                                if ((week == 0 && dayIndex < firstDayOfWeek) || dayPointer > daysInMonth) {
                                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                                } else {
                                                    val finalDay = dayPointer
                                                    val dayDate = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, currentYear)
                                                        set(Calendar.MONTH, currentMonth)
                                                        set(Calendar.DAY_OF_MONTH, finalDay)
                                                    }.time

                                                    val activityItems = getItemsForDate(dayDate)
                                                    val isSelected = finalDay == currentDayActive

                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                            .clickable {
                                                                showDayDetailsDialog = dayDate
                                                            }
                                                            .padding(4.dp),
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = finalDay.toString(),
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 12.sp
                                                        )
                                                        if (activityItems.isNotEmpty()) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                                            )
                                                        }
                                                    }
                                                    dayPointer++
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Widget 6: Universal Visual Timeline Shortcut
                if (visibleWidgets["Timeline"] == true) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimelineSheet = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Timeline, contentDescription = "Timeline", tint = MaterialTheme.colorScheme.onSecondary)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Visual chronological timeline", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text("Access files, films, notes, and records chronologically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open")
                            }
                        }
                    }
                }

                // Widget 7: Daily Inspirational Offline Quote
                if (visibleWidgets["Inspiration"] == true) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Daily Inspiration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    }
                                    IconButton(onClick = { quoteOffset++ }, modifier = Modifier.size(24.dp)) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Quote", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\"${currentQuote.first}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily.Serif
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "— ${currentQuote.second}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Daily Journal Portal Card (Always Available)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDailyJournal() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Journal", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Journal & Mood Tracker", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Track your mental wellness and watch list analytics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open")
                        }
                    }
                }

                // Widget 8: Cloud Backup Sync Status
                if (visibleWidgets["Backup"] == true) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CloudQueue,
                                            contentDescription = "Google Drive",
                                            tint = if (driveConnected) Color(0xFF34A853) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Google Drive Cloud Backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    }
                                    if (driveConnected) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(Color(0xFFE6F4EA))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Active", color = Color(0xFF137333), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (driveConnected) "Connected to Google Drive as: $driveAccount" else "Drive account is not linked. Setup manual or automated backups.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.syncBackup() },
                                    enabled = driveConnected && !isSyncing,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Syncing Encrypted Archive...")
                                    } else {
                                        Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync Now")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sync Backups Now")
                                    }
                                }
                            }
                        }
                    }
                }

                // Widget 9: Recent Additions Carousel
                if (visibleWidgets["Recent"] == true) {
                    item {
                        Column {
                            Text("Recent Additions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            if (files.isEmpty() && notes.isEmpty() && movies.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Your private vault is currently empty. Tap category cards to populate.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(files.take(4)) { file ->
                                        RecentItemCard(
                                            title = file.fileName,
                                            subtitle = "File • ${file.fileType}",
                                            icon = if (file.fileType == "PHOTO") Icons.Default.Photo else Icons.Default.AttachFile
                                        )
                                    }
                                    items(notes.take(4)) { note ->
                                        RecentItemCard(
                                            title = viewModel.decryptNoteTitle(note.title),
                                            subtitle = "Note • ${note.category}",
                                            icon = Icons.Default.Description
                                        )
                                    }
                                    items(movies.take(4)) { movie ->
                                        RecentItemCard(
                                            title = movie.title,
                                            subtitle = "Movie • ★${movie.personalRating}",
                                            icon = Icons.Default.Movie
                                        )
                                    }
                                    items(tvSeries.take(4)) { tv ->
                                        RecentItemCard(
                                            title = tv.title,
                                            subtitle = "TV • S${tv.currentSeason} E${tv.currentEpisode}",
                                            icon = Icons.Default.Tv
                                        )
                                    }
                                    items(subjects.take(4)) { sub ->
                                        RecentItemCard(
                                            title = sub.name,
                                            subtitle = "Subject Folder",
                                            icon = Icons.Default.School
                                        )
                                    }
                                    items(connections.take(4)) { conn ->
                                        RecentItemCard(
                                            title = conn.label.ifBlank { "Linked Memory" },
                                            subtitle = "Relation Link",
                                            icon = Icons.Default.Favorite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Widget 10: Continue Studying
                if (visibleWidgets["Recent"] == true && subjects.isNotEmpty()) {
                    item {
                        Column {
                            Text("Continue Studying 🧠", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Study Progress", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        TextButton(onClick = onNavigateToStudySpace) {
                                            Text("Open Study Space")
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Display active planners/goals
                                    val activePlanners = planners.filter { !it.isCompleted }
                                    if (activePlanners.isNotEmpty()) {
                                        Text("Upcoming Targets Checklists:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        activePlanners.take(2).forEach { planner ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                                Checkbox(
                                                    checked = planner.isCompleted,
                                                    onCheckedChange = { viewModel.updateStudyPlanner(planner.copy(isCompleted = it)) },
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(planner.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    } else {
                                        Text("All study deadlines met! Ready for new learning targets.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Widget 11: Continue Watching
                if (visibleWidgets["Recent"] == true && tvSeries.any { !it.isFinished }) {
                    item {
                        Column {
                            Text("Continue Watching 🍿", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            val watchingSeries = tvSeries.filter { !it.isFinished }
                            watchingSeries.take(2).forEach { series ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(series.title.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(series.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            Text("Season ${series.currentSeason} • Episode ${series.currentEpisode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        IconButton(onClick = {
                                            viewModel.updateTvSeries(series.copy(currentEpisode = series.currentEpisode + 1))
                                        }) {
                                            Icon(Icons.Default.PlusOne, contentDescription = "Quick Episode Up", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Widget 12: Recent Linked Memories
                if (visibleWidgets["Recent"] == true && connections.isNotEmpty()) {
                    item {
                        Column {
                            Text("Recent Connected Memories 💖", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    connections.take(2).forEach { conn ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = conn.label.ifBlank { "Linked memory relation" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // --- Interactive Dialogue: Customized Layout Configuration Overlay ---
    if (showLayoutCustomizer) {
        AlertDialog(
            onDismissRequest = { showLayoutCustomizer = false },
            title = { Text("Customize Dashboard Layout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select which smart widgets should be displayed on your secure dashboard landing:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    visibleWidgets.forEach { (widgetName, isVisible) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleWidgetVisibility(widgetName) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isVisible,
                                onCheckedChange = { viewModel.toggleWidgetVisibility(widgetName) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = widgetName, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLayoutCustomizer = false }) {
                    Text("Done")
                }
            }
        )
    }

    // --- Quick Search overlay (formerly a simulated voice dictation) ---
    // The app does not bundle a speech-recognition engine, so this is presented honestly as
    // a quick-search picker that fills the universal search box with a chosen category.
    if (showVoiceSearchOverlay) {
        Dialog(onDismissRequest = { showVoiceSearchOverlay = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Quick Search", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Box(modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                    Text("Tap a category to search your vault instantly.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    val quickTerms = listOf("romance", "diary", "movie", "note", "photo", "study", "journal")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickTerms.forEach { term ->
                            AssistChip(
                                onClick = {
                                    universalQuery = term
                                    showVoiceSearchOverlay = false
                                },
                                label = { Text(term) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                    TextButton(onClick = { showVoiceSearchOverlay = false }) { Text("Cancel") }
                }
            }
        }
    }

    // --- OCR Image Picker overlay ---
    // The app does not bundle an on-device OCR engine (ML Kit), so this is presented honestly
    // as an image picker that lets the user attach a photo to search by its filename/content.
    if (showOcrScannerOverlay) {
        Dialog(onDismissRequest = { showOcrScannerOverlay = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Search by Photo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Pick a photo to search its filename in your vault", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(onClick = {
                        // No on-device OCR engine is bundled; we surface a helpful hint instead of
                        // fabricating recognized text.
                        universalQuery = ""
                        showOcrScannerOverlay = false
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Photo Library")
                    }
                    TextButton(onClick = { showOcrScannerOverlay = false }) { Text("Cancel") }
                }
            }
        }
    }

    // --- Interactive Calendar Day Details Dialog ---
    if (showDayDetailsDialog != null) {
        val date = showDayDetailsDialog!!
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val itemsOnDay = getItemsForDate(date)

        AlertDialog(
            onDismissRequest = { showDayDetailsDialog = null },
            title = { Text(text = sdf.format(date), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (itemsOnDay.isEmpty()) {
                        Text("No secure logs, notes, or entries recorded on this day.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Encrypted records matching this day:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        itemsOnDay.forEach { (type, name) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (type == "Note") Icons.Default.Description else if (type == "Movie") Icons.Default.Movie else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "[$type] $name", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDayDetailsDialog = null }) {
                    Text("Close")
                }
            }
        )
    }

    // --- Collections Management Fullscreen Bottom Sheet ---
    if (showCollectionsSheet) {
        Dialog(onDismissRequest = { showCollectionsSheet = false; selectedCollectionId = null }) {
            Card(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Collections Vault", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        IconButton(onClick = { showCollectionsSheet = false; selectedCollectionId = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    if (selectedCollectionId == null) {
                        // Display All Collections
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                            items(customCollections) { col ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { selectedCollectionId = col.id },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = col.emoji, fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(col.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            Text(col.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        }
                                        Row {
                                            IconButton(onClick = { viewModel.togglePinCollection(col.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = "Pin",
                                                    tint = if (col.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                )
                                            }
                                            IconButton(onClick = { viewModel.deleteCollection(col.id) }) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = { showAddCollectionDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Collection")
                        }
                    } else {
                        // Display Specific Collection Details
                        val col = customCollections.firstOrNull { it.id == selectedCollectionId }
                        if (col != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = col.emoji, fontSize = 40.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(col.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                            Text(col.description, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Assigned Secure Items:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            // List relevant items belonging to this collection category/tag match
                            val matchingNotes = notes.filter {
                                val titleDec = viewModel.decryptNoteTitle(it.title).lowercase()
                                val descDec = viewModel.decryptNoteContent(it.content).lowercase()
                                val colName = col.name.split(" ")[0].lowercase()
                                titleDec.contains(colName) || descDec.contains(colName) || it.category.lowercase() == colName
                            }
                            val matchingMovies = movies.filter {
                                val colName = col.name.split(" ")[0].lowercase()
                                it.title.lowercase().contains(colName) || it.genre.lowercase().contains(colName)
                            }

                            if (matchingNotes.isEmpty() && matchingMovies.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Add \"${col.name.split(" ")[0]}\" keywords to your notes or movies to assign them to this collection!", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                    items(matchingNotes) { n ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = Color(0xFF9C27B0))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("[Note] ${viewModel.decryptNoteTitle(n.title)}", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    items(matchingMovies) { m ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                            Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = Color(0xFFE91E63))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("[Movie] ${m.title} (${m.genre})", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            Button(onClick = { selectedCollectionId = null }, modifier = Modifier.fillMaxWidth()) {
                                Text("Back to Collections List")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogue to create custom collection ---
    if (showAddCollectionDialog) {
        var colName by remember { mutableStateOf("") }
        var colEmoji by remember { mutableStateOf("📁") }
        var colDesc by remember { mutableStateOf("") }
        var colCoverIndex by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { showAddCollectionDialog = false },
            title = { Text("Create New Collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = colName,
                        onValueChange = { colName = it },
                        label = { Text("Collection Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = colEmoji,
                        onValueChange = { colEmoji = it },
                        label = { Text("Emoji Representative") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = colDesc,
                        onValueChange = { colDesc = it },
                        label = { Text("Brief Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Theme Gradient Cover Cover:", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        coverBrushes.forEachIndexed { idx, brush ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(brush)
                                    .border(if (colCoverIndex == idx) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, Color.Transparent), CircleShape)
                                    .clickable { colCoverIndex = idx }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (colName.isNotEmpty()) {
                            viewModel.addCollection(colName, colEmoji, colDesc, colCoverIndex)
                            showAddCollectionDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCollectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Fullscreen Dialog visual timeline view ---
    if (showTimelineSheet) {
        Dialog(onDismissRequest = { showTimelineSheet = false }) {
            Card(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Visual Timeline", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        IconButton(onClick = { showTimelineSheet = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Group together all items chronologically!
                    val timelineItems = remember(files, notes, movies, dailyJournals) {
                        val items = mutableListOf<TimelineItem>()
                        // Files
                        files.forEach {
                            items.add(TimelineItem(it.fileName, "File • ${it.fileType}", System.currentTimeMillis() - 2 * 3600000, it.fileType, it.isFavorite))
                        }
                        // Notes
                        notes.forEach {
                            items.add(TimelineItem(viewModel.decryptNoteTitle(it.title), "Note • ${it.category}", it.updatedAt, "Note", it.isFavorite))
                        }
                        // Movies
                        movies.forEach {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val dateLong = try { sdf.parse(it.dateWatched)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                            items.add(TimelineItem(it.title, "Movie • ${it.genre}", dateLong, "Movie", it.personalRating >= 4.0))
                        }
                        // Daily journals
                        dailyJournals.forEach {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val dateLong = try { sdf.parse(it.dateString)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                            items.add(TimelineItem("Check-In: ${it.mood}", "Journal entry log", dateLong, "Journal", false))
                        }

                        // Sort chronologically descending
                        items.sortByDescending { it.timestamp }
                        items
                    }

                    var timelineFilter by remember { mutableStateOf("All") }
                    val timelineFilters = listOf("All", "Files", "Notes", "Movies", "Journals")

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(timelineFilters) { filter ->
                            val active = filter == timelineFilter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { timelineFilter = filter }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = filter, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (timelineItems.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No chronological items logged yet.")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            val filteredTimeline = timelineItems.filter {
                                when (timelineFilter) {
                                    "Files" -> it.category.startsWith("File")
                                    "Notes" -> it.category.startsWith("Note")
                                    "Movies" -> it.category.startsWith("Movie")
                                    "Journals" -> it.category.startsWith("Journal")
                                    else -> true
                                }
                            }

                            items(filteredTimeline) { item ->
                                val dateFormatted = SimpleDateFormat("MMMM d, yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                    // Bullet Timeline Node vertical line indicator
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(60.dp)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(text = dateFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when {
                                                        item.category.startsWith("File") -> Icons.Default.AttachFile
                                                        item.category.startsWith("Note") -> Icons.Default.Description
                                                        item.category.startsWith("Movie") -> Icons.Default.Movie
                                                        else -> Icons.Default.AutoAwesome
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(item.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                }
                                                if (item.isFav) {
                                                    Icon(imageVector = Icons.Default.Star, contentDescription = "Favorite", tint = Color(0xFFFBBC05), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Representing visual timeline record representation
data class TimelineItem(
    val title: String,
    val category: String,
    val timestamp: Long,
    val type: String,
    val isFav: Boolean
)

@Composable
fun CategoryCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() }
            .testTag("category_card_$title"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = count,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun RecentItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
