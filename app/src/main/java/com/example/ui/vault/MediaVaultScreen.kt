package com.example.ui.vault

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultFile
import com.example.ui.VaultViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaVaultScreen(
    viewModel: VaultViewModel,
    categoryType: String, // PHOTO, VIDEO, AUDIO, DOC
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val files by viewModel.files.collectAsState()
    val activeFiles = files.filter { it.fileType == categoryType }

    // Media viewer overlays
    var activePhotoViewerFile by remember { mutableStateOf<VaultFile?>(null) }
    var activeVideoPlayerFile by remember { mutableStateOf<VaultFile?>(null) }
    var activeAudioPlayerFile by remember { mutableStateOf<VaultFile?>(null) }

    // Voice recorder simulation state
    var isRecordingAudio by remember { mutableStateOf(false) }
    var secondsRecorded by remember { mutableStateOf(0) }

    // Video Speed simulation
    var playbackSpeed by remember { mutableStateOf(1.0f) }

    // Photo Slideshow State
    var isSlideshowActive by remember { mutableStateOf(false) }

    // System Picker launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFileFromUri(context, it, categoryType)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure ${categoryType.lowercase().replaceFirstChar { it.uppercase() }} Vault") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { pickerLauncher.launch("$categoryType/*") }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Import File")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Content Area based on category
            if (categoryType == "AUDIO") {
                // Built-in Voice Recorder Module
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Secure Tape Recorder",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = if (isRecordingAudio) String.format("Recording: %02d:%02d", secondsRecorded / 60, secondsRecorded % 60) else "00:00",
                            style = MaterialTheme.typography.displayMedium,
                            fontFamily = FontFamily.Monospace,
                            color = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isRecordingAudio) {
                                FloatingActionButton(
                                    onClick = {
                                        isRecordingAudio = true
                                        secondsRecorded = 0
                                    },
                                    containerColor = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("start_audio_rec")
                                ) {
                                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Record", modifier = Modifier.size(28.dp))
                                }
                            } else {
                                FloatingActionButton(
                                    onClick = {
                                        isRecordingAudio = false
                                        // Save simulated audio voice file
                                        viewModel.importFileFromUri(
                                            context,
                                            Uri.fromFile(File(context.filesDir, "mock_voice.mp3")),
                                            "AUDIO"
                                        )
                                    },
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.testTag("stop_audio_rec")
                                ) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Grid list of Files
            if (activeFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp))
                        Text(
                            "No files hidden in this directory. Tap + to secure your first file!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                if (categoryType == "PHOTO" || categoryType == "VIDEO") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(activeFiles) { item ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (categoryType == "PHOTO") activePhotoViewerFile = item
                                        if (categoryType == "VIDEO") activeVideoPlayerFile = item
                                    }
                                    .testTag("file_item_${item.fileName}")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = if (categoryType == "PHOTO") Icons.Default.Photo else Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.fileName,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Documents or Audio simple list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeFiles) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (categoryType == "AUDIO") activeAudioPlayerFile = item
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (categoryType == "DOC") Icons.Default.Description else Icons.Default.Audiotrack,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.fileName, fontWeight = FontWeight.Bold)
                                        Text(text = "${item.fileSize / 1024} KB • Secure Encrypted", fontSize = 12.sp)
                                    }
                                    IconButton(onClick = { viewModel.deleteFile(item) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete File", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Overlays & Viewers ---

        // 1. Photo Slideshow & Viewer Overlay
        if (activePhotoViewerFile != null) {
            val file = activePhotoViewerFile!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = file.fileName, color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { activePhotoViewerFile = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Main display container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(120.dp), tint = Color.White.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Secure High-Resolution Photo Viewer",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Navigation bar on bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val index = activeFiles.indexOf(file)
                            if (index > 0) activePhotoViewerFile = activeFiles[index - 1]
                        }) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.White)
                        }

                        Button(onClick = { isSlideshowActive = !isSlideshowActive }) {
                            Text(if (isSlideshowActive) "Stop Slideshow ⏸" else "Start Slideshow ▶")
                        }

                        IconButton(onClick = {
                            val index = activeFiles.indexOf(file)
                            if (index < activeFiles.size - 1) activePhotoViewerFile = activeFiles[index + 1]
                        }) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                        }
                    }
                }
            }
        }

        // 2. Built-in video player
        if (activeVideoPlayerFile != null) {
            val file = activeVideoPlayerFile!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = file.fileName, color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { activeVideoPlayerFile = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Playing Decrypted Stream at ${playbackSpeed}x", color = Color.White.copy(alpha = 0.7f))
                        }
                    }

                    // Video progress and speed adjustments
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Simulated playback position slider
                        Slider(value = 0.35f, onValueChange = {}, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { playbackSpeed = 0.5f }) { Text("0.5x", color = Color.White) }
                            TextButton(onClick = { playbackSpeed = 1.0f }) { Text("1.0x", color = Color.White) }
                            TextButton(onClick = { playbackSpeed = 1.5f }) { Text("1.5x", color = Color.White) }
                            TextButton(onClick = { playbackSpeed = 2.0f }) { Text("2.0x", color = Color.White) }
                        }

                        Button(
                            onClick = { activeVideoPlayerFile = null },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Resume Playback Later 💾")
                        }
                    }
                }
            }
        }

        // 3. Built-in custom Audio playback bar
        if (activeAudioPlayerFile != null) {
            val file = activeAudioPlayerFile!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { activeAudioPlayerFile = null },
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Tape: ${file.fileName}", fontWeight = FontWeight.Bold)

                        Slider(value = 0.25f, onValueChange = {})

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {}) { Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Prev") }
                            IconButton(onClick = {}) { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play") }
                            IconButton(onClick = {}) { Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next") }
                        }

                        Button(onClick = { activeAudioPlayerFile = null }) {
                            Text("Minimize")
                        }
                    }
                }
            }
        }
    }
}
