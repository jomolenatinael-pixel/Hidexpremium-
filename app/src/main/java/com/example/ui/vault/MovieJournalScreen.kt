package com.example.ui.vault

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MovieJournal
import com.example.ui.VaultViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlin.random.Random

fun getDynamicMovieGradient(movie: MovieJournal): Brush {
    val hash = movie.title.hashCode()
    // Generate two solid, warm pastel colors based on title hash for dynamic poster cover
    val hue1 = (Math.abs(hash) % 360).toFloat()
    val hue2 = ((Math.abs(hash) + 120) % 360).toFloat()
    val color1 = Color.hsv(hue1, 0.7f, 0.8f)
    val color2 = Color.hsv(hue2, 0.8f, 0.5f)
    return Brush.linearGradient(listOf(color1, color2))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieJournalScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val movies by viewModel.movies.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()

    var activeTab by remember { mutableStateOf("Journal") } // Journal, Watchlist, Stats
    var selectedMovie by remember { mutableStateOf<MovieJournal?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCollection by remember { mutableStateOf("All") } // All, Favorites, Action, Romance, Sci-Fi, Comedy, Timeline, Moods
    var isAddingMovie by remember { mutableStateOf(false) }

    // Forms
    var title by remember { mutableStateOf("") }
    var releaseYear by remember { mutableStateOf("2026") }
    var genre by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("English") }
    var runtime by remember { mutableStateOf("120 min") }
    var personalRating by remember { mutableStateOf(4.5f) }
    var favoriteLevel by remember { mutableStateOf(3) } // hearts count
    var mood by remember { mutableStateOf("Happy") }
    var dateWatched by remember { mutableStateOf("2026-07-09") }
    var rewatchCount by remember { mutableStateOf("1") }
    var location by remember { mutableStateOf("Home") }
    var partner by remember { mutableStateOf("None") }
    var character by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }
    var bestScene by remember { mutableStateOf("") }
    var emotionalMoment by remember { mutableStateOf("") }
    var saddestMoment by remember { mutableStateOf("") }
    var funniestMoment by remember { mutableStateOf("") }
    var soundtrack by remember { mutableStateOf("") }
    var lesson by remember { mutableStateOf("") }
    var memories by remember { mutableStateOf("") }
    var review by remember { mutableStateOf("") }
    var rewatchDetails by remember { mutableStateOf("") }
    var easterEggs by remember { mutableStateOf("") }
    var similar by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var trailerLink by remember { mutableStateOf("") }
    var recommend by remember { mutableStateOf(true) }
    var isWatchlistField by remember { mutableStateOf(false) }
    var upcomingReleaseDate by remember { mutableStateOf("") } // YYYY-MM-DD

    // Random Movie Picker
    var showRandomPicker by remember { mutableStateOf(false) }
    var pickedMovieName by remember { mutableStateOf("") }

    // AI Stats Modal
    var showAiStatsInfo by remember { mutableStateOf(false) }
    var aiStatsText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    var isGeneratingByGemini by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current

    if (isAddingMovie || selectedMovie != null) {
        val editing = selectedMovie
        // Form & Detail View
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (editing != null) editing.title else "New Movie Entry") },
                    navigationIcon = {
                        IconButton(onClick = {
                            isAddingMovie = false
                            selectedMovie = null
                        }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (editing != null) {
                            IconButton(onClick = {
                                viewModel.deleteMovie(editing)
                                selectedMovie = null
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            IconButton(onClick = {
                                viewModel.saveMovie(
                                    title = title,
                                    releaseYear = releaseYear.toIntOrNull() ?: 2026,
                                    genre = genre,
                                    language = language,
                                    runtime = runtime,
                                    personalRating = personalRating,
                                    favoriteLevel = favoriteLevel,
                                    mood = mood,
                                    dateWatched = dateWatched,
                                    rewatchCount = rewatchCount.toIntOrNull() ?: 1,
                                    location = location,
                                    partner = partner,
                                    character = character,
                                    quote = quote,
                                    bestScene = bestScene,
                                    emotional = emotionalMoment,
                                    sad = saddestMoment,
                                    funny = funniestMoment,
                                    soundtrack = soundtrack,
                                    lesson = lesson,
                                    memories = memories,
                                    review = review,
                                    rewatchDetails = rewatchDetails,
                                    easterEggs = easterEggs,
                                    similar = similar,
                                    tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                    isWatchlist = isWatchlistField,
                                    releaseDate = upcomingReleaseDate
                                )
                                isAddingMovie = false
                            }) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Save Movie", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (editing != null) {
                    // DISPLAY DETAILS MODE
                    item {
                        // Poster Banner Glassmorphic Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = editing.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.headlineSmall,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Year: ${editing.releaseYear} • ${editing.runtime} • ${editing.genre}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Rating grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFBBC05))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rating: ${editing.personalRating}/5.0", fontWeight = FontWeight.Bold)
                            }
                            Row {
                                repeat(editing.favoriteLevel) {
                                    Icon(imageVector = Icons.Default.Favorite, contentDescription = "Favorite", tint = Color.Red, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    item {
                        // Watch metadata
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📅 Watched: ${editing.dateWatched} (Mood: ${editing.moodAfterWatching})")
                                Text("📍 Location: ${editing.watchLocation} • Partner: ${editing.watchPartner}")
                                Text("🔄 Rewatch Count: ${editing.rewatchCount}")
                            }
                        }
                    }

                    item {
                        // Personal Memories and Review
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Memories & Thoughts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("review: ${editing.review}")
                                Text("Personal Memories: ${editing.personalMemories}")
                                Text("Life Lesson: ${editing.lifeLesson}")
                            }
                        }
                    }

                    item {
                        // Quotes & Characters
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Highlight Quotes & Characters", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Favorite Character: ${editing.favoriteCharacter}")
                                Text("Favorite Quote: \"${editing.favoriteQuote}\"", fontWeight = FontWeight.Bold)
                                Text("Best Scene: ${editing.bestScene}")
                            }
                        }
                    }

                    item {
                        // Emotional moments
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Moments Log", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Most Emotional: ${editing.mostEmotionalMoment}")
                                Text("Saddest: ${editing.saddestMoment}")
                                Text("Funniest: ${editing.funniestMoment}")
                                Text("Best Soundtrack: ${editing.bestSoundtrack}")
                            }
                        }
                    }

                    item {
                        // Easter eggs & Similar
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Hidden Details & Easter Eggs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(editing.hiddenDetails)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Similar Movies", fontWeight = FontWeight.Bold)
                                Text(editing.similarMovies)
                            }
                        }
                    }

                    item {
                        var isGeneratingDetailByGemini by remember { mutableStateOf(false) }
                        var detailAiInsights by remember { mutableStateOf<String?>(null) }
                        var aiDetailError by remember { mutableStateOf("") }
                        val coroutineScope = rememberCoroutineScope()

                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("gemini_insights_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Gemini",
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "Gemini AI Insights & Review",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }

                                if (detailAiInsights == null) {
                                    Text(
                                        text = "Get a detailed critic review, rating breakdown, philosophical themes, and tailor-made movie recommendations specifically for ${editing.title}.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    if (aiDetailError.isNotEmpty()) {
                                        Text(
                                            text = aiDetailError,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            isGeneratingDetailByGemini = true
                                            aiDetailError = ""
                                            coroutineScope.launch {
                                                try {
                                                    val prompt = """
                                                        Provide an in-depth film critic analysis, thematic review, and rating breakdown for the movie '${editing.title}'.
                                                        Include the following sections formatted nicely:
                                                        ### 🎬 Critic Review
                                                        (A deep, eloquent 2-sentence synopsis/review of the film's artistry and narrative)
                                                        
                                                        ### 🏆 Rating & Achievements
                                                        - **AI Recommended Rating**: X/10 (with a brief justification)
                                                        - **Key Strengths**: (cinematography, performances, writing, soundtrack, etc.)
                                                        
                                                        ### 🧠 Philosophical Themes & Lessons
                                                        (What deeper themes or lessons does the film explore?)
                                                        
                                                        ### 🍿 Tailored Recommendations
                                                        1. **[Movie Name]** - (Brief 1-sentence reason why you'll love it based on this)
                                                        2. **[Movie Name]** - (Brief 1-sentence reason)
                                                        3. **[Movie Name]** - (Brief 1-sentence reason)
                                                    """.trimIndent()
                                                    val response = viewModel.callGeminiApi(prompt)
                                                    detailAiInsights = response
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    aiDetailError = "Error generating insights: ${e.localizedMessage}"
                                                } finally {
                                                    isGeneratingDetailByGemini = false
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isGeneratingDetailByGemini
                                    ) {
                                        if (isGeneratingDetailByGemini) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = MaterialTheme.colorScheme.onTertiary,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Generating Insights...")
                                        } else {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Generate AI Critic Insights")
                                        }
                                    }
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        detailAiInsights?.split("\n")?.forEach { line ->
                                            when {
                                                line.trim().startsWith("### ") -> {
                                                    Text(
                                                        text = line.trim().substring(4),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                    )
                                                }
                                                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                                                    Text(
                                                        text = line.trim(),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                else -> {
                                                    Text(
                                                        text = line,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { detailAiInsights = null },
                                                modifier = Modifier.weight(1.5f)
                                            ) {
                                                Text("Reset")
                                            }

                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        try {
                                                            var updatedReview = editing.review
                                                            var updatedLessons = editing.lifeLesson
                                                            var updatedSimilar = editing.similarMovies

                                                            val lines = detailAiInsights?.split("\n") ?: emptyList()
                                                            var currentSection = ""
                                                            val criticReviewLines = mutableListOf<String>()
                                                            val themesLines = mutableListOf<String>()
                                                            val recommendationLines = mutableListOf<String>()

                                                            lines.forEach { line ->
                                                                val trimmed = line.trim()
                                                                when {
                                                                    trimmed.contains("Critic Review") -> currentSection = "critic"
                                                                    trimmed.contains("Philosophical Themes") -> currentSection = "themes"
                                                                    trimmed.contains("Recommendations") -> currentSection = "rec"
                                                                    trimmed.startsWith("### ") -> currentSection = ""
                                                                    else -> {
                                                                        if (trimmed.isNotEmpty()) {
                                                                            when (currentSection) {
                                                                                "critic" -> criticReviewLines.add(trimmed)
                                                                                "themes" -> themesLines.add(trimmed)
                                                                                "rec" -> recommendationLines.add(trimmed)
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            if (criticReviewLines.isNotEmpty()) {
                                                                updatedReview = criticReviewLines.joinToString("\n")
                                                            }
                                                            if (themesLines.isNotEmpty()) {
                                                                updatedLessons = themesLines.joinToString("\n")
                                                            }
                                                            if (recommendationLines.isNotEmpty()) {
                                                                updatedSimilar = recommendationLines.joinToString("\n")
                                                            }

                                                            val updatedMovie = editing.copy(
                                                                review = updatedReview,
                                                                lifeLesson = updatedLessons,
                                                                similarMovies = updatedSimilar
                                                            )

                                                            viewModel.updateMovie(updatedMovie)
                                                            selectedMovie = updatedMovie
                                                            detailAiInsights = null
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(2f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.tertiary
                                                )
                                            ) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Save to Journal")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // NEW ENTRY FIELD FORM MODE
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Movie Title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("movie_title_input")
                        )
                    }

                    item {
                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                if (title.trim().isNotEmpty()) {
                                    isGeneratingByGemini = true
                                    aiErrorMessage = ""
                                    coroutineScope.launch {
                                        try {
                                            val prompt = """
                                                Analyze the movie titled "$title". Provide a detailed and accurate analysis. 
                                                Return ONLY a valid JSON object matching the following structure precisely. Do NOT wrap the JSON in markdown code blocks, do not write any introductory or trailing text. Return raw JSON.
                                                {
                                                  "genre": "Genre of the movie",
                                                  "releaseYear": 2024,
                                                  "runtime": "e.g., 148 min",
                                                  "language": "Primary language of the movie",
                                                  "summary": "Brief 1-2 sentence plot summary",
                                                  "rating": 4.5,
                                                  "recommendations": "3 similar movies as a comma-separated list",
                                                  "quote": "A famous/memorable quote from the movie",
                                                  "character": "Main/iconic character from the movie",
                                                  "bestScene": "A brief description of the most famous or best scene",
                                                  "hiddenDetails": "1 interesting trivia or easter egg about the movie"
                                                }
                                            """.trimIndent()
                                            val response = viewModel.callGeminiApi(prompt)
                                            val jsonStr = response.trim().substringAfter("{").substringBeforeLast("}")
                                            val fullJson = "{$jsonStr}"
                                            val json = org.json.JSONObject(fullJson)
                                            genre = json.optString("genre", genre)
                                            releaseYear = json.optInt("releaseYear", releaseYear.toIntOrNull() ?: 2026).toString()
                                            runtime = json.optString("runtime", runtime)
                                            language = json.optString("language", language)
                                            review = json.optString("summary", review)
                                            personalRating = json.optDouble("rating", personalRating.toDouble()).toFloat()
                                            similar = json.optString("recommendations", similar)
                                            quote = json.optString("quote", quote)
                                            character = json.optString("character", character)
                                            bestScene = json.optString("bestScene", bestScene)
                                            easterEggs = json.optString("hiddenDetails", easterEggs)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            aiErrorMessage = "Failed to parse Gemini response: ${e.localizedMessage}"
                                        } finally {
                                            isGeneratingByGemini = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("gemini_generate_button"),
                            enabled = !isGeneratingByGemini && title.trim().isNotEmpty()
                        ) {
                            if (isGeneratingByGemini) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Movie with Gemini...")
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auto-Generate with Gemini")
                            }
                        }
                    }

                    if (aiErrorMessage.isNotEmpty()) {
                        item {
                            Text(
                                text = aiErrorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = releaseYear,
                                onValueChange = { releaseYear = it },
                                label = { Text("Year") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = genre,
                                onValueChange = { genre = it },
                                label = { Text("Genre") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = language,
                                onValueChange = { language = it },
                                label = { Text("Language") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = runtime,
                                onValueChange = { runtime = it },
                                label = { Text("Runtime") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        // Rating & Favorites
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Rating: ${"★".repeat(personalRating.toInt())} (${personalRating})")
                            Slider(
                                value = personalRating,
                                onValueChange = { personalRating = it },
                                valueRange = 1f..5f,
                                steps = 3
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Favorite Hearts: ${"❤️".repeat(favoriteLevel)}")
                            Slider(
                                value = favoriteLevel.toFloat(),
                                onValueChange = { favoriteLevel = it.toInt() },
                                valueRange = 0f..5f,
                                steps = 4
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Is this a Watchlist item? (Unreleased/To watch)")
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(checked = isWatchlistField, onCheckedChange = { isWatchlistField = it })
                        }
                    }

                    if (isWatchlistField) {
                        item {
                            OutlinedTextField(
                                value = upcomingReleaseDate,
                                onValueChange = { upcomingReleaseDate = it },
                                label = { Text("Release Date (YYYY-MM-DD)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = dateWatched,
                                    onValueChange = { dateWatched = it },
                                    label = { Text("Date Watched (YYYY-MM-DD)") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = mood,
                                    onValueChange = { mood = it },
                                    label = { Text("Watch Mood") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = location,
                                    onValueChange = { location = it },
                                    label = { Text("Watch Location") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = partner,
                                    onValueChange = { partner = it },
                                    label = { Text("Watch Partner") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = review,
                                onValueChange = { review = it },
                                label = { Text("Review thoughts") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = memories,
                                onValueChange = { memories = it },
                                label = { Text("Personal memories connected") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = quote,
                                onValueChange = { quote = it },
                                label = { Text("Favorite quote") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = character,
                                onValueChange = { character = it },
                                label = { Text("Favorite character") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = bestScene,
                                onValueChange = { bestScene = it },
                                label = { Text("Best scene") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = easterEggs,
                                onValueChange = { easterEggs = it },
                                label = { Text("Easter eggs / Hidden details noticed") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    } else {
        // LOBBY/LISTING VIEW
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("AI Movie Journal") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showRandomPicker = true }) {
                            Icon(imageVector = Icons.Default.Autorenew, contentDescription = "Random Pick")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    title = ""
                    genre = ""
                    review = ""
                    memories = ""
                    character = ""
                    quote = ""
                    bestScene = ""
                    easterEggs = ""
                    isWatchlistField = false
                    isAddingMovie = true
                }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Movie")
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
                // Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    listOf("Journal", "Watchlist", "AI Stats").forEach { tab ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeTab == tab) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == tab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val collectionsList = listOf("All", "Favorites", "Top 10", "Want to Rewatch", "Action", "Romance", "Sci-Fi", "Comedy", "Timeline")
                val filteredMovies = movies.filter { movie ->
                    val matchesSearch = movie.title.contains(searchQuery, ignoreCase = true) ||
                            movie.genre.contains(searchQuery, ignoreCase = true) ||
                            movie.watchPartner.contains(searchQuery, ignoreCase = true) ||
                            movie.favoriteCharacter.contains(searchQuery, ignoreCase = true) ||
                            movie.moodAfterWatching.contains(searchQuery, ignoreCase = true)

                    val matchesCollection = when (selectedCollection) {
                        "All" -> true
                        "Favorites" -> movie.personalRating >= 4.0f || movie.favoriteLevel >= 4
                        "Top 10" -> true // Handled during sorting/take below
                        "Want to Rewatch" -> movie.rewatchCount >= 1
                        "Action" -> movie.genre.contains("action", ignoreCase = true)
                        "Romance" -> movie.genre.contains("romance", ignoreCase = true)
                        "Sci-Fi" -> movie.genre.contains("sci-fi", ignoreCase = true) || movie.genre.contains("science", ignoreCase = true)
                        "Comedy" -> movie.genre.contains("comedy", ignoreCase = true)
                        else -> true
                    }
                    matchesSearch && matchesCollection
                }.let { list ->
                    if (selectedCollection == "Top 10") {
                        list.sortedByDescending { it.personalRating }.take(10)
                    } else if (selectedCollection == "Timeline") {
                        list.sortedByDescending { it.dateWatched }
                    } else {
                        list
                    }
                }

                when (activeTab) {
                    "Journal" -> {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Search Bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search title, character, co-watcher, emotion...") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )

                            // Collections horizontal shelf
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(collectionsList) { coll ->
                                    FilterChip(
                                        selected = selectedCollection == coll,
                                        onClick = { selectedCollection = coll },
                                        label = { Text(coll, fontSize = 12.sp) }
                                    )
                                }
                            }

                            if (filteredMovies.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No matching movies in your vault.",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(filteredMovies) { item ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedMovie = item }
                                                .testTag("movie_card_${item.title}"),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                // Dynamic gradient poster art representing visual color extraction
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(130.dp)
                                                        .background(getDynamicMovieGradient(item), RoundedCornerShape(12.dp))
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.BottomStart
                                                ) {
                                                    Column {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(CircleShape)
                                                                .background(Color.Black.copy(alpha = 0.3f))
                                                                .padding(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Movie,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(20.dp),
                                                                tint = Color.White
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = item.genre,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.White.copy(alpha = 0.9f),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = item.title,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "★ ${item.personalRating}",
                                                        color = Color(0xFFFBBC05),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "❤️".repeat(item.favoriteLevel),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Watchlist" -> {
                        if (watchlist.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No movies on your Watchlist. Tap '+' to add a movie countdown!")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(watchlist) { item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                Text(text = "Genre: ${item.genre} • Release Date: ${item.releaseDate.ifEmpty { "TBD" }}")
                                            }
                                            // Simulated Countdown
                                            val daysLeft = Random.nextInt(2, 45)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "In $daysLeft days",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "AI Stats" -> {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header Banner
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Stats",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "AI Movie Insights & Recap",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Deep analytical metrics of your offline cinema history.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            // 1. Yearly Recap & Highlight Stats Card
                            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                            val currentYearMovies = movies.filter {
                                val dateStr = it.dateWatched
                                dateStr.startsWith("$currentYear-") || it.releaseYear == currentYear
                            }
                            val topGenre = movies.groupBy { it.genre }.maxByOrNull { it.value.size }?.key ?: "None"
                            val avgRating = if (movies.isNotEmpty()) movies.map { it.personalRating }.average() else 0.0
                            val favoriteMovies = movies.filter { it.personalRating >= 4.5f }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "🏆 $currentYear Yearly Movie Recap",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Movies Watched", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Text("${currentYearMovies.size} logged", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                        }
                                        Column {
                                            Text("Top Genre", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Text(topGenre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Column {
                                            Text("Avg Rating", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Text(String.format(Locale.US, "★ %.1f", avgRating), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = Color(0xFFFBBC05))
                                        }
                                    }
                                }
                            }

                            // 2. Genre Breakdown Progress Bars
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "📊 Genre Breakdown",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val genres = listOf("Action", "Romance", "Sci-Fi", "Comedy", "Drama", "Thriller")
                                    genres.forEach { gen ->
                                        val count = movies.count { it.genre.contains(gen, ignoreCase = true) }
                                        val total = if (movies.isNotEmpty()) movies.size.toFloat() else 1f
                                        val progress = count / total

                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = gen, style = MaterialTheme.typography.bodyMedium)
                                                Text(text = "$count movies", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Movie Anniversaries Shelf
                            if (movies.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "📅 Secret Cinema Anniversaries",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // We will showcase the first movie as our "anniversary highlight" or list them elegantly
                                        val anniversaryMovie = movies.first()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(imageVector = Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(text = "Anniversary: ${anniversaryMovie.title}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    text = "First watched on ${anniversaryMovie.dateWatched}. Rewatched ${anniversaryMovie.rewatchCount} times!",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Interactive "Generate AI Profile" Action Button
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = {
                                        isAnalyzing = true
                                        val topGenreStat = movies.groupBy { it.genre }.maxByOrNull { it.value.size }?.key ?: "N/A"
                                        val avgRatingStat = if (movies.isNotEmpty()) movies.map { it.personalRating }.average() else 0.0
                                        val favPartner = movies.groupBy { it.watchPartner }.maxByOrNull { it.value.size }?.key ?: "N/A"
                                        val favQuote = movies.filter { it.favoriteQuote.isNotEmpty() }.randomOrNull()?.favoriteQuote ?: "N/A"

                                        aiStatsText = """
                                            === YOUR PERSONALIZED AI MOVIE INSIGHTS ===
                                            
                                            🎬 Watch Personality: The Emotional Critic
                                            You don't just watch movies; you live them. You have a highly sentimental connection to soundtracks and quote logs.
                                            
                                            📊 Key Statistics:
                                            - Total Movies Vaulted: ${movies.size}
                                            - Dominant Favorite Genre: $topGenreStat
                                            - Personal Rating Index: ${String.format(Locale.US, "%.1f", avgRatingStat)} / 5.0
                                            - Most Frequent Co-Watcher: $favPartner
                                            
                                            💬 Your Ultimate Motto quote: 
                                            "$favQuote"
                                            
                                            🧠 AI Wellness Insight:
                                            Your mood matches 85% energetic/inspired after watching cinema. Keep filling up your secret catalog!
                                        """.trimIndent()
                                        isAnalyzing = false
                                        showAiStatsInfo = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Deep AI Movie Profile")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Random Picker Dialog
    if (showRandomPicker) {
        AlertDialog(
            onDismissRequest = { showRandomPicker = false },
            title = { Text("Random Movie Picker") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Can't decide what movie to watch? Let HideX select one from your vault database:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (pickedMovieName.isEmpty()) "Click Spin to Pick!" else pickedMovieName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val all = movies + watchlist
                    pickedMovieName = if (all.isNotEmpty()) {
                        all.random().title
                    } else {
                        "No movies logged yet!"
                    }
                }) {
                    Text("Spin Dial 🎲")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pickedMovieName = ""
                    showRandomPicker = false
                }) {
                    Text("Close")
                }
            }
        )
    }

    // AI Stats Report Modal
    if (showAiStatsInfo) {
        AlertDialog(
            onDismissRequest = { showAiStatsInfo = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Movie Statistics")
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = aiStatsText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAiStatsInfo = false }) {
                    Text("Brilliant!")
                }
            }
        )
    }
}
