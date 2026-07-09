package com.example.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyJournal
import com.example.ui.VaultViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyJournalScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val journals by viewModel.dailyJournals.collectAsState()

    var showAddJournalDialog by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf("Happy") }
    var journalText by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("2026-07-09") }

    val moods = mapOf(
        "Happy" to "😊",
        "Calm" to "🧘",
        "Neutral" to "😐",
        "Energetic" to "⚡",
        "Sad" to "😢",
        "Stressed" to "🤯"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Journal & Moods") },
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
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dateString = sdf.format(Date())
                    selectedMood = "Happy"
                    journalText = ""
                    showAddJournalDialog = true
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Entry")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mood Trend Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mental Wellness Analytics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (journals.isEmpty()) {
                        Text("No logs yet. Add your first mental check-in today!", fontSize = 12.sp)
                    } else {
                        val moodGroups = journals.groupBy { it.mood }
                        val dominantMood = moodGroups.maxByOrNull { it.value.size }?.key ?: "N/A"
                        val emoji = moods[dominantMood] ?: ""
                        
                        Text(
                            text = "Dominant Emotional Trend: $dominantMood $emoji",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Text("Historical Reflection Timeline", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            if (journals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Mental history is blank. Tap '+' to write today's log.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(journals) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
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
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = moods[item.mood] ?: "❓", fontSize = 24.sp)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.dateString, fontWeight = FontWeight.Bold)
                                    Text(text = item.journalText, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                }

                                IconButton(onClick = { viewModel.deleteDailyJournal(item) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddJournalDialog) {
        AlertDialog(
            onDismissRequest = { showAddJournalDialog = false },
            title = { Text("Log Mental Wellness") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose Today's Mood:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        moods.forEach { (name, icon) ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedMood == name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedMood = name },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = icon, fontSize = 18.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dateString,
                        onValueChange = { dateString = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = journalText,
                        onValueChange = { journalText = it },
                        label = { Text("What happened today? (Diary Thoughts)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (journalText.isNotEmpty()) {
                        viewModel.saveDailyJournal(dateString, selectedMood, journalText)
                        showAddJournalDialog = false
                    }
                }) {
                    Text("Record Reflection")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddJournalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
