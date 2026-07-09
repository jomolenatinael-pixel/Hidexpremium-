package com.example.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VaultViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboard(
    viewModel: VaultViewModel,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDailyJournal: () -> Unit
) {
    val isDecoy by viewModel.isDecoyUnlocked.collectAsState()
    val files by viewModel.files.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val driveConnected by viewModel.googleDriveConnected.collectAsState()
    val driveAccount by viewModel.googleAccountName.collectAsState()

    // Calculate dynamic counts
    val photoCount = files.count { it.fileType == "PHOTO" }
    val videoCount = files.count { it.fileType == "VIDEO" }
    val docCount = files.count { it.fileType == "DOC" }
    val audioCount = files.count { it.fileType == "AUDIO" }

    // Storage calculations
    val totalSizeBytes = files.sumOf { it.fileSize } + (notes.size * 2048) + (movies.size * 8192)
    val totalSizeMb = String.format(Locale.US, "%.2f KB", totalSizeBytes / 1024.0)

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
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { viewModel.lockVault() }) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock App", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
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
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                // Storage and Header Summary
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Shred-Safe Storage",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = totalSizeMb,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            // Progress bar
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
                                text = "Encrypting local files on-the-fly with military-grade standard",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Portals/Categories Grid
                item {
                    Text(
                        text = "Encrypted Categories",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

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
                    }
                }

                // AI Special Feature / Shortcut Row
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDailyJournal() },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
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
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Journal",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Journal & Mood Tracker",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Track your mental wellness and watch list analytics",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open")
                        }
                    }
                }

                // Cloud Backup State Widget
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
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
                                    Text(
                                        text = "Google Drive Cloud Backup",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                if (driveConnected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFFE6F4EA))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Active",
                                            color = Color(0xFF137333),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
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
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
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

                // Recent entries placeholder
                item {
                    Text(
                        text = "Recent Additions",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (files.isEmpty() && notes.isEmpty() && movies.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Your private vault is currently empty. Tap category cards to populate.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    item {
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
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

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
