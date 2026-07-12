package com.example.ui.vault

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.TvSeries
import com.example.ui.VaultViewModel
import java.text.SimpleDateFormat
import java.util.*

fun getTvSeriesGradient(tv: TvSeries): Brush {
    val hash = tv.title.hashCode()
    val hue1 = (Math.abs(hash) % 360).toFloat()
    val hue2 = ((Math.abs(hash) + 160) % 360).toFloat()
    val color1 = Color.hsv(hue1, 0.6f, 0.7f)
    val color2 = Color.hsv(hue2, 0.7f, 0.4f)
    return Brush.linearGradient(listOf(color1, color2))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvSeriesScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val seriesList by viewModel.tvSeries.collectAsState()

    var activeTab by remember { mutableStateOf("Watching") } // Watching, Finished
    var selectedTv by remember { mutableStateOf<TvSeries?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isAddingTv by remember { mutableStateOf(false) }

    // Add Form states
    var title by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var currentSeason by remember { mutableStateOf("1") }
    var currentEpisode by remember { mutableStateOf("1") }
    var totalSeasons by remember { mutableStateOf("1") }
    var isFinished by remember { mutableStateOf(false) }
    var favoriteEpisode by remember { mutableStateOf("") }
    var favoriteCharacter by remember { mutableStateOf("") }
    var bestSeason by remember { mutableStateOf("") }
    var personalReview by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(4.5f) }
    var moodAfterWatching by remember { mutableStateOf("Calm") }
    var tags by remember { mutableStateOf("") }
    var nextSeasonReleaseDate by remember { mutableStateOf("") } // YYYY-MM-DD for countdowns

    val filteredSeries = seriesList.filter {
        val matchesSearch = it.title.lowercase(Locale.ROOT).contains(searchQuery.lowercase(Locale.ROOT))
        val matchesTab = if (activeTab == "Watching") !it.isFinished else it.isFinished
        matchesSearch && matchesTab
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My TV Series space", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("tv_series_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (!isAddingTv && selectedTv == null) {
                ExtendedFloatingActionButton(
                    text = { Text("Log New Series") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        isAddingTv = true
                        title = ""
                        posterUrl = ""
                        currentSeason = "1"
                        currentEpisode = "1"
                        totalSeasons = "1"
                        isFinished = false
                        favoriteEpisode = ""
                        favoriteCharacter = ""
                        bestSeason = ""
                        personalReview = ""
                        rating = 4.5f
                        moodAfterWatching = "Calm"
                        tags = ""
                        nextSeasonReleaseDate = ""
                    },
                    modifier = Modifier.testTag("add_tv_fab")
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isAddingTv) {
                // Form view
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Log TV Show & Series Review", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add details of romance dramas, school shows, and favorite TV releases to your encrypted second brain.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Series Title *") },
                            modifier = Modifier.fillMaxWidth().testTag("tv_title_input"),
                            singleLine = true
                        )
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = currentSeason,
                                onValueChange = { currentSeason = it },
                                label = { Text("Season") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentEpisode,
                                onValueChange = { currentEpisode = it },
                                label = { Text("Episode") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = totalSeasons,
                                onValueChange = { totalSeasons = it },
                                label = { Text("Total Seasons") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isFinished, onCheckedChange = { isFinished = it })
                            Text("Mark as Finished / Completed Series", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    item {
                        Column {
                            Text("Your Rating: ${String.format(Locale.US, "%.1f", rating)} Stars", fontWeight = FontWeight.Bold)
                            Slider(
                                value = rating,
                                onValueChange = { rating = it },
                                valueRange = 0f..5f,
                                steps = 9
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = favoriteEpisode,
                            onValueChange = { favoriteEpisode = it },
                            label = { Text("Favorite Episode") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = favoriteCharacter,
                            onValueChange = { favoriteCharacter = it },
                            label = { Text("Favorite Character / Ultimate Crush") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bestSeason,
                            onValueChange = { bestSeason = it },
                            label = { Text("Best Season in your opinion") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = moodAfterWatching,
                            onValueChange = { moodAfterWatching = it },
                            label = { Text("Mood / Emotional Impact (e.g. Heartwarming, Crying, Giggly)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = nextSeasonReleaseDate,
                            onValueChange = { nextSeasonReleaseDate = it },
                            placeholder = { Text("YYYY-MM-DD") },
                            label = { Text("Next Season Release Date (for countdown)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            placeholder = { Text("Romance, School, Comfort") },
                            label = { Text("Custom Tags (comma separated)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = personalReview,
                            onValueChange = { personalReview = it },
                            label = { Text("Your Personal Review / Memory Connected") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isAddingTv = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        viewModel.addTvSeries(
                                            TvSeries(
                                                title = title,
                                                posterUrl = posterUrl,
                                                currentSeason = currentSeason.toIntOrNull() ?: 1,
                                                currentEpisode = currentEpisode.toIntOrNull() ?: 1,
                                                totalSeasons = totalSeasons.toIntOrNull() ?: 1,
                                                isFinished = isFinished,
                                                favoriteEpisode = favoriteEpisode,
                                                favoriteCharacter = favoriteCharacter,
                                                bestSeason = bestSeason,
                                                personalReview = personalReview,
                                                rating = rating,
                                                moodAfterWatching = moodAfterWatching,
                                                tags = tags,
                                                nextSeasonReleaseDate = nextSeasonReleaseDate
                                            )
                                        )
                                        isAddingTv = false
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("save_tv_button"),
                                enabled = title.isNotBlank()
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            } else if (selectedTv != null) {
                // Detail view
                val tv = selectedTv!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { selectedTv = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close details")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Series Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                viewModel.deleteTvSeries(tv)
                                selectedTv = null
                            }, modifier = Modifier.testTag("delete_tv_button")) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(getTvSeriesGradient(tv))
                                    .padding(24.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(tv.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${tv.rating} / 5.0", color = Color.White, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(if (tv.isFinished) "Finished 🎉" else "Watching 🍿") },
                                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color.White)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Progress update controls (Students love easy increments!)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Current Progress Tracker", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Season ${tv.currentSeason} of ${tv.totalSeasons}", style = MaterialTheme.typography.bodyLarge)
                                        Text("Episode ${tv.currentEpisode}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = {
                                                if (tv.currentEpisode > 1) {
                                                    viewModel.updateTvSeries(tv.copy(currentEpisode = tv.currentEpisode - 1))
                                                    selectedTv = tv.copy(currentEpisode = tv.currentEpisode - 1)
                                                }
                                            },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Prev Episode")
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.updateTvSeries(tv.copy(currentEpisode = tv.currentEpisode + 1))
                                                selectedTv = tv.copy(currentEpisode = tv.currentEpisode + 1)
                                            },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Next Episode", tint = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        val nextSeason = tv.currentSeason + 1
                                        viewModel.updateTvSeries(tv.copy(currentSeason = nextSeason, currentEpisode = 1))
                                        selectedTv = tv.copy(currentSeason = nextSeason, currentEpisode = 1)
                                    }) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Next Season")
                                    }
                                }
                            }
                        }
                    }

                    // Next Season Release Countdown
                    if (tv.nextSeasonReleaseDate.isNotBlank()) {
                        item {
                            val countdownText = remember(tv.nextSeasonReleaseDate) {
                                try {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val target = sdf.parse(tv.nextSeasonReleaseDate)
                                    val diffMs = (target?.time ?: 0L) - System.currentTimeMillis()
                                    if (diffMs > 0) {
                                        val days = diffMs / (1000 * 60 * 60 * 24)
                                        "$days Days until next season premiere!"
                                    } else {
                                        "Season is out or date passed!"
                                    }
                                } catch (e: Exception) {
                                    "Launch Date: ${tv.nextSeasonReleaseDate}"
                                }
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(countdownText, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Metadata details
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (tv.favoriteCharacter.isNotBlank()) {
                                DetailRow(label = "Favorite Character / Crush", value = tv.favoriteCharacter, icon = Icons.Default.Favorite)
                            }
                            if (tv.favoriteEpisode.isNotBlank()) {
                                DetailRow(label = "Favorite Episode", value = tv.favoriteEpisode, icon = Icons.Default.Tv)
                            }
                            if (tv.bestSeason.isNotBlank()) {
                                DetailRow(label = "Best Season", value = tv.bestSeason, icon = Icons.Default.AutoAwesome)
                            }
                            if (tv.moodAfterWatching.isNotBlank()) {
                                DetailRow(label = "Vibe / Feelings", value = tv.moodAfterWatching, icon = Icons.Default.EmojiEmotions)
                            }
                            if (tv.tags.isNotBlank()) {
                                DetailRow(label = "Tags", value = tv.tags, icon = Icons.Default.LocalOffer)
                            }
                            if (tv.personalReview.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("My Personal Memory & Thoughts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        tv.personalReview,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // List view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    TabRow(
                        selectedTabIndex = if (activeTab == "Watching") 0 else 1,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = activeTab == "Watching",
                            onClick = { activeTab = "Watching" },
                            text = { Text("Watching 🍿") }
                        )
                        Tab(
                            selected = activeTab == "Finished",
                            onClick = { activeTab = "Finished" },
                            text = { Text("Finished 🎉") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search TV series...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredSeries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.TvOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No TV series logged here yet.", style = MaterialTheme.typography.titleMedium)
                                Text("Tap '+' to log your favorite series, reviews, and crushes!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize().weight(1f)
                        ) {
                            items(filteredSeries) { tv ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedTv = tv }
                                        .testTag("tv_item_card_${tv.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(getTvSeriesGradient(tv)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(tv.title.firstOrNull()?.uppercase() ?: "?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tv.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Season ${tv.currentSeason} • Ep ${tv.currentEpisode}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (tv.favoriteCharacter.isNotBlank()) {
                                                Text("Crush: ${tv.favoriteCharacter}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        IconButton(onClick = {
                                            // Quick increment button
                                            viewModel.updateTvSeries(tv.copy(currentEpisode = tv.currentEpisode + 1))
                                        }) {
                                            Icon(Icons.Default.PlusOne, contentDescription = "Increment Episode", tint = MaterialTheme.colorScheme.primary)
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

@Composable
fun DetailRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
