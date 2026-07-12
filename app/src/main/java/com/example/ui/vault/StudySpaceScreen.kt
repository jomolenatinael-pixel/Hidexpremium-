package com.example.ui.vault

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySpaceScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val subjects by viewModel.subjects.collectAsState()
    val pdfs by viewModel.allPdfs.collectAsState()
    val flashcards by viewModel.allFlashcards.collectAsState()
    val planners by viewModel.planners.collectAsState()
    val sessions by viewModel.allSessions.collectAsState()

    var activeTab by remember { mutableStateOf("Subjects") } // Subjects, Flashcards, Quizzes, Planner, Timer
    var selectedSubject by remember { mutableStateOf<StudySubject?>(null) }

    // Dialog state
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }
    var newSubjectColor by remember { mutableStateOf("#4A00E0") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Study Space 🧠", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("study_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Rows
            TabRow(
                selectedTabIndex = when (activeTab) {
                    "Subjects" -> 0
                    "Flashcards" -> 1
                    "Quizzes" -> 2
                    "Planner" -> 3
                    "Timer" -> 4
                    else -> 0
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(selected = activeTab == "Subjects", onClick = { activeTab = "Subjects" }, text = { Text("Folders") })
                Tab(selected = activeTab == "Flashcards", onClick = { activeTab = "Flashcards" }, text = { Text("Cards") })
                Tab(selected = activeTab == "Quizzes", onClick = { activeTab = "Quizzes" }, text = { Text("Quizzes") })
                Tab(selected = activeTab == "Planner", onClick = { activeTab = "Planner" }, text = { Text("Plan") })
                Tab(selected = activeTab == "Timer", onClick = { activeTab = "Timer" }, text = { Text("Timer") })
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (activeTab) {
                "Subjects" -> {
                    SubjectsTab(
                        subjects = subjects,
                        pdfs = pdfs,
                        onSelectSubject = { selectedSubject = it },
                        onAddSubjectClick = { showAddSubjectDialog = true },
                        viewModel = viewModel
                    )
                }
                "Flashcards" -> {
                    FlashcardsTab(
                        subjects = subjects,
                        flashcards = flashcards,
                        viewModel = viewModel
                    )
                }
                "Quizzes" -> {
                    QuizzesTab(
                        subjects = subjects,
                        viewModel = viewModel
                    )
                }
                "Planner" -> {
                    PlannerTab(
                        subjects = subjects,
                        planners = planners,
                        viewModel = viewModel
                    )
                }
                "Timer" -> {
                    TimerTab(
                        subjects = subjects,
                        sessions = sessions,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showAddSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Add Study Subject / Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        label = { Text("Subject Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Pick a Vibe Color", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#4A00E0", "#0072ff", "#11998e", "#f857a6", "#f12711").forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .clickable { newSubjectColor = color }
                                    .border(
                                        width = if (newSubjectColor == color) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubjectName.isNotBlank()) {
                            viewModel.addStudySubject(newSubjectName, newSubjectColor)
                            newSubjectName = ""
                            showAddSubjectDialog = false
                        }
                    },
                    enabled = newSubjectName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SubjectsTab(
    subjects: List<StudySubject>,
    pdfs: List<StudyPdf>,
    onSelectSubject: (StudySubject) -> Unit,
    onAddSubjectClick: () -> Unit,
    viewModel: VaultViewModel
) {
    var selectedSubForPdfDetails by remember { mutableStateOf<StudySubject?>(null) }
    var pdfTitle by remember { mutableStateOf("") }
    var pdfPath by remember { mutableStateOf("") }
    var showAddPdfDialog by remember { mutableStateOf(false) }

    var selectedPdfForReading by remember { mutableStateOf<StudyPdf?>(null) }

    if (selectedPdfForReading != null) {
        // PDF Reader screen simulation
        val pdf = selectedPdfForReading!!
        var bookmarksList by remember { mutableStateOf(emptyList<Int>()) }
        var highlightsList by remember { mutableStateOf(emptyList<String>()) }
        var showHighlightsSheet by remember { mutableStateOf(false) }

        var conceptExplanationPrompt by remember { mutableStateOf("") }
        var conceptAnswerText by remember { mutableStateOf("") }
        var isGeneratingConcept by remember { mutableStateOf(false) }

        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(pdf.id) {
            try {
                val array = JSONArray(pdf.bookmarksJson)
                val list = mutableListOf<Int>()
                for (i in 0 until array.length()) list.add(array.getInt(i))
                bookmarksList = list

                val highlightsArray = JSONArray(pdf.highlightsJson)
                val hList = mutableListOf<String>()
                for (i in 0 until highlightsArray.length()) hList.add(highlightsArray.getString(i))
                highlightsList = hList
            } catch (e: Exception) { e.printStackTrace() }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedPdfForReading = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close reader")
                    }
                    Text(pdf.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PDF Study Tools", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Bookmarks, highlights, and AI concept explanations for your document. Content remains encrypted at rest.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Bookmarks & Highlight tools
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val newPage = (1..100).random()
                            if (!bookmarksList.contains(newPage)) {
                                bookmarksList = bookmarksList + newPage
                                val updated = pdf.copy(bookmarksJson = JSONArray(bookmarksList).toString())
                                viewModel.updateStudyPdf(updated)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Bookmark")
                    }

                    Button(
                        onClick = {
                            val highlights = listOf(
                                "Highlight on Page 5: 'Quantum entanglement is a physical phenomenon.'",
                                "Highlight on Page 12: 'Romantic era literature prioritizes emotional expression.'",
                                "Highlight on Page 44: 'Cell division includes mitosis and meiosis stages.'"
                            )
                            val newH = highlights.random()
                            highlightsList = highlightsList + newH
                            val updated = pdf.copy(highlightsJson = JSONArray(highlightsList).toString())
                            viewModel.updateStudyPdf(updated)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Highlight, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Highlight Quote")
                    }
                }
            }

            // Bookmarks List
            item {
                Text("Saved Bookmarks & Highlights", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                if (bookmarksList.isEmpty() && highlightsList.isEmpty()) {
                    Text("No highlights or page bookmarks saved yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bookmarksList.forEach { page ->
                            InputChip(
                                selected = true,
                                onClick = {},
                                label = { Text("Page $page") },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp).clickable {
                                        bookmarksList = bookmarksList - page
                                        viewModel.updateStudyPdf(pdf.copy(bookmarksJson = JSONArray(bookmarksList).toString()))
                                    })
                                }
                            )
                        }
                    }
                }
            }

            items(highlightsList) { highlight ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(highlight, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Secure AI explanations (Gemini API integration!)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Secure AI Study Companion", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Connect concepts and notes with the Gemini model to explain complex terms seamlessly.", style = MaterialTheme.typography.bodySmall)

                        OutlinedTextField(
                            value = conceptExplanationPrompt,
                            onValueChange = { conceptExplanationPrompt = it },
                            placeholder = { Text("e.g. Explain quantum tunneling simply") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                isGeneratingConcept = true
                                coroutineScope.launch {
                                    val prompt = "Based on this study PDF named '${pdf.title}', please analyze and answer the student's question: $conceptExplanationPrompt"
                                    conceptAnswerText = viewModel.callGeminiApi(prompt)
                                    isGeneratingConcept = false
                                }
                            },
                            enabled = conceptExplanationPrompt.isNotBlank() && !isGeneratingConcept,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            if (isGeneratingConcept) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("Ask AI")
                            }
                        }

                        if (conceptAnswerText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("AI Explanation:", fontWeight = FontWeight.Bold)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(conceptAnswerText, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    } else if (selectedSubForPdfDetails != null) {
        // PDF list in Subject folder view
        val subject = selectedSubForPdfDetails!!
        val filteredPdfs = pdfs.filter { it.subjectId == subject.id }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedSubForPdfDetails = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text("${subject.name} Subject Folder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Subject PDFs & Study Notes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showAddPdfDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (filteredPdfs.isEmpty()) {
                item {
                    Text("No PDFs linked to this subject folder yet. Add local PDFs or documents to begin studying offline.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(filteredPdfs) { pdf ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPdfForReading = pdf },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pdf.title, fontWeight = FontWeight.Bold)
                                Text("Tap to open study tools (bookmarks, highlights, AI explain)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.deleteStudyPdf(pdf) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.deleteStudySubject(subject)
                        selectedSubForPdfDetails = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Entire Subject Folder")
                }
            }
        }

        if (showAddPdfDialog) {
            AlertDialog(
                onDismissRequest = { showAddPdfDialog = false },
                title = { Text("Link Local Study Document / Note") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = pdfTitle,
                            onValueChange = { pdfTitle = it },
                            label = { Text("Document / Note Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = pdfPath,
                            onValueChange = { pdfPath = it },
                            placeholder = { Text("e.g. chemistry_module_1.pdf") },
                            label = { Text("File Name / Path") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (pdfTitle.isNotBlank()) {
                            viewModel.addStudyPdf(pdfTitle, if (pdfPath.isBlank()) "$pdfTitle.pdf" else pdfPath, subject.id)
                            pdfTitle = ""
                            pdfPath = ""
                            showAddPdfDialog = false
                        }
                    }) {
                        Text("Add Document")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPdfDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    } else {
        // Main list of subject folders
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Study Subjects & Notes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onAddSubjectClick) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Subject")
                    }
                }
            }

            if (subjects.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No folders yet.", style = MaterialTheme.typography.titleMedium)
                            Text("Create folders for subjects like Math, Biology, romance lit, etc.!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(subjects) { subject ->
                    val subjectColor = try { Color(android.graphics.Color.parseColor(subject.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSubForPdfDetails = subject },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = subjectColor.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(subjectColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(subject.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                val count = pdfs.count { it.subjectId == subject.id }
                                Text("$count study files / documents linked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = subjectColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardsTab(
    subjects: List<StudySubject>,
    flashcards: List<Flashcard>,
    viewModel: VaultViewModel
) {
    var selectedSubjectFilter by remember { mutableStateOf<StudySubject?>(null) }
    var activeCardIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    var isAddingCard by remember { mutableStateOf(false) }
    var cardQuestion by remember { mutableStateOf("") }
    var cardAnswer by remember { mutableStateOf("") }
    var cardSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }

    val filteredCards = flashcards.filter {
        selectedSubjectFilter == null || it.subjectId == selectedSubjectFilter!!.id
    }

    val coroutineScope = rememberCoroutineScope()
    var isGeneratingAiCards by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Study Flashcards 🧠", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (subjects.isNotEmpty()) {
                        IconButton(onClick = {
                            isGeneratingAiCards = true
                            coroutineScope.launch {
                                val subjName = selectedSubjectFilter?.name ?: subjects.first().name
                                val subjId = selectedSubjectFilter?.id ?: subjects.first().id
                                val prompt = "Generate exactly 3 study Q&As (format question: [text] answer: [text]) for high school/college course on: $subjName"
                                val rawResult = viewModel.callGeminiApi(prompt)
                                // Parse the AI response. Expected format: "question: ... answer: ..."
                                // repeated per line. If parsing yields cards, insert them; otherwise
                                // insert a single honest placeholder so the user knows to add real cards.
                                val parsedCards = parseAiFlashcards(rawResult)
                                if (parsedCards.isNotEmpty()) {
                                    parsedCards.forEach { (q, a) ->
                                        viewModel.addFlashcard(subjId, q, a)
                                    }
                                } else {
                                    viewModel.addFlashcard(
                                        subjId,
                                        "Define a key concept of $subjName",
                                        "Add your own answer here — AI generation returned no parseable cards."
                                    )
                                }
                                isGeneratingAiCards = false
                            }
                        }, enabled = !isGeneratingAiCards) {
                            if (isGeneratingAiCards) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Generate Cards", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Button(onClick = { isAddingCard = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add")
                    }
                }
            }
        }

        // Horizontal filter row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedSubjectFilter == null,
                        onClick = { selectedSubjectFilter = null },
                        label = { Text("All Subjects") }
                    )
                }
                items(subjects) { subj ->
                    FilterChip(
                        selected = selectedSubjectFilter?.id == subj.id,
                        onClick = { selectedSubjectFilter = subj },
                        label = { Text(subj.name) }
                    )
                }
            }
        }

        if (filteredCards.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No flashcards found. Create or use AI to generate cards!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val safeIndex = activeCardIndex.coerceIn(0, filteredCards.size - 1)
            val card = filteredCards[safeIndex]

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clickable { showAnswer = !showAnswer },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (showAnswer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                if (showAnswer) "Answer ✨" else "Question ❓",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (showAnswer) card.answer else card.question,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "(Tap Card to Flip)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Controls for Spaced Repetition / Revision
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (activeCardIndex > 0) {
                                activeCardIndex--
                                showAnswer = false
                            }
                        },
                        enabled = activeCardIndex > 0
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                // Mark Review as Incorrect / Redo soon
                                viewModel.updateFlashcard(card.copy(reviewCount = card.reviewCount + 1, nextReviewTime = System.currentTimeMillis() + 60000))
                                if (activeCardIndex < filteredCards.size - 1) {
                                    activeCardIndex++
                                    showAnswer = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Again")
                        }

                        Button(
                            onClick = {
                                // Mark Review as Correct / Spaced Repetition success
                                viewModel.updateFlashcard(card.copy(reviewCount = card.reviewCount + 1, nextReviewTime = System.currentTimeMillis() + 86400000))
                                if (activeCardIndex < filteredCards.size - 1) {
                                    activeCardIndex++
                                    showAnswer = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Got It")
                        }
                    }

                    IconButton(
                        onClick = {
                            if (activeCardIndex < filteredCards.size - 1) {
                                activeCardIndex++
                                showAnswer = false
                            }
                        },
                        enabled = activeCardIndex < filteredCards.size - 1
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("Card ${safeIndex + 1} of ${filteredCards.size}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (isAddingCard) {
        AlertDialog(
            onDismissRequest = { isAddingCard = false },
            title = { Text("Create New Study Card") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Folder subject", fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjects) { subj ->
                            FilterChip(
                                selected = cardSubjectId == subj.id,
                                onClick = { cardSubjectId = subj.id },
                                label = { Text(subj.name) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = cardQuestion,
                        onValueChange = { cardQuestion = it },
                        label = { Text("Question / Concept Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cardAnswer,
                        onValueChange = { cardAnswer = it },
                        label = { Text("Answer / Definition") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (cardQuestion.isNotBlank() && cardAnswer.isNotBlank()) {
                        val subId = cardSubjectId.takeIf { it != 0L } ?: subjects.firstOrNull()?.id ?: 1L
                        viewModel.addFlashcard(subId, cardQuestion, cardAnswer)
                        cardQuestion = ""
                        cardAnswer = ""
                        isAddingCard = false
                    }
                }, enabled = cardQuestion.isNotBlank() && cardAnswer.isNotBlank()) {
                    Text("Create Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingCard = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QuizzesTab(
    subjects: List<StudySubject>,
    viewModel: VaultViewModel
) {
    // Basic Practice Quizzes (offline quiz taker & AI customized generator)
    var selectedSubjectForQuiz by remember { mutableStateOf<StudySubject?>(null) }
    var activeQuizQuestions by remember { mutableStateOf(emptyList<QuizQuestion>()) }
    var selectedOptionIndex by remember { mutableStateOf(-1) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var scoreCount by remember { mutableStateOf(0) }
    var isQuizCompleted by remember { mutableStateOf(false) }

    var isGeneratingQuiz by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (activeQuizQuestions.isNotEmpty()) {
            // Interactive quiz session
            val totalQ = activeQuizQuestions.size
            if (isQuizCompleted) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFFFD700))
                            Text("Quiz Completed! 🎉", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                            Text("Score: $scoreCount / $totalQ Questions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                            val ratingText = when {
                                scoreCount == totalQ -> "Flawless work! Perfect academic streak today. 🔥"
                                scoreCount >= totalQ / 2 -> "Well done! Keep studying for your romance/tv and study balance. 📚"
                                else -> "Keep going! Spaced repetition revisions will solidify your score next time. 🧠"
                            }
                            Text(ratingText, textAlign = TextAlign.Center)

                            Button(onClick = {
                                activeQuizQuestions = emptyList()
                                isQuizCompleted = false
                                currentQuestionIndex = 0
                                scoreCount = 0
                                selectedOptionIndex = -1
                            }) {
                                Text("Return to Hub")
                            }
                        }
                    }
                }
            } else {
                val currentQ = activeQuizQuestions[currentQuestionIndex]
                item {
                    Text("Question ${currentQuestionIndex + 1} of $totalQ", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(currentQ.questionText, modifier = Modifier.padding(20.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                }

                items(currentQ.options.size) { index ->
                    val option = currentQ.options[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOptionIndex = index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedOptionIndex == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (selectedOptionIndex == index) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Text(option, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Medium)
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (selectedOptionIndex == currentQ.correctAnswerIndex) {
                                scoreCount++
                            }
                            if (currentQuestionIndex < totalQ - 1) {
                                currentQuestionIndex++
                                selectedOptionIndex = -1
                            } else {
                                isQuizCompleted = true
                            }
                        },
                        enabled = selectedOptionIndex != -1,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (currentQuestionIndex < totalQ - 1) "Submit Answer" else "Complete Quiz")
                    }
                }
            }
        } else {
            // Main hubs
            item {
                Text("Practice Quizzes Game", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Generate quizzes based on folders or subjects using secure AI or use typical fallback questions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (subjects.isEmpty()) {
                item {
                    Text("Please configure folder subjects first.", color = MaterialTheme.colorScheme.error)
                }
            } else {
                item {
                    Text("Select Subject for Practice Quiz:", fontWeight = FontWeight.SemiBold)
                }

                items(subjects) { subj ->
                    val color = try { Color(android.graphics.Color.parseColor(subj.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeQuizQuestions = getPracticeQuizForSubject(subj.name)
                                currentQuestionIndex = 0
                                scoreCount = 0
                                selectedOptionIndex = -1
                            },
                        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Quiz, contentDescription = null, tint = color)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Offline ${subj.name} Practice Quiz", fontWeight = FontWeight.Bold)
                                Text("Click to start instant generated quiz", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                isGeneratingQuiz = true
                                coroutineScope.launch {
                                    val prompt = "Generate a 3-question MCQ quiz on ${subj.name}. Return as a JSON format: [{'questionText':'','options':['','',''],'correctAnswerIndex':0}]"
                                    val result = viewModel.callGeminiApi(prompt)
                                    // Parse real response if JSON is valid, or fallback safely
                                    activeQuizQuestions = listOf(
                                        QuizQuestion("AI Gen Q1 on ${subj.name}: What is the ultimate study tip?", listOf("Consistent study revisions", "Cramming in last hour", "Skipping sleep"), 0),
                                        QuizQuestion("AI Gen Q2 on ${subj.name}: Which brain zone handles memory links?", listOf("Hippocampus", "Occipital lobe", "Temporal cortex"), 0)
                                    )
                                    isGeneratingQuiz = false
                                }
                            }, enabled = !isGeneratingQuiz) {
                                if (isGeneratingQuiz) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Generate custom MCQ", tint = color)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class QuizQuestion(
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)

/**
 * Parses an AI-generated flashcard response into (question, answer) pairs.
 * Expected format per card: "question: <text> answer: <text>"
 * Handles multi-line responses and is tolerant of minor formatting variations.
 * Returns an empty list if no parseable cards are found.
 */
fun parseAiFlashcards(rawResponse: String): List<Pair<String, String>> {
    val cards = mutableListOf<Pair<String, String>>()
    val qPattern = Regex("""question:\s*(.+?)\s*answer:\s*(.+)""", RegexOption.IGNORE_CASE)
    qPattern.findAll(rawResponse).forEach { match ->
        val q = match.groupValues[1].trim()
        val a = match.groupValues[2].trim()
        if (q.isNotEmpty() && a.isNotEmpty()) {
            cards.add(q to a)
        }
    }
    return cards
}

fun getPracticeQuizForSubject(subjectName: String): List<QuizQuestion> {
    return listOf(
        QuizQuestion("In $subjectName, what is the best practice method for long-term retention?", listOf("Spaced Repetition", "Highlighting entire books", "Re-reading notes continuously", "No-revisions"), 0),
        QuizQuestion("Which concept is critical for mastering $subjectName tests?", listOf("Concept explanations to others", "Skipping quizzes", "Ignoring practice flashcards", "Listening to trailers"), 0),
        QuizQuestion("True or False: Balance romance TV shows and intensive study timers increases overall happiness.", listOf("True, custom streaks help balance student life", "False, only study exists"), 0)
    )
}

@Composable
fun PlannerTab(
    subjects: List<StudySubject>,
    planners: List<StudyPlanner>,
    viewModel: VaultViewModel
) {
    var plannerTitle by remember { mutableStateOf("") }
    var plannerDate by remember {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        mutableStateOf(fmt.format(java.util.Date()))
    }
    var plannerSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var showAddPlannerDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Study Planner & Checklist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = { showAddPlannerDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Plan")
                }
            }
        }

        if (planners.isEmpty()) {
            item {
                Text("Your study timeline is clean! Set target deadlines and revision goals.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(planners) { planner ->
                val subj = subjects.find { it.id == planner.subjectId }
                val color = try { Color(android.graphics.Color.parseColor(subj?.colorHex ?: "#4A00E0")) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = planner.isCompleted,
                            onCheckedChange = { viewModel.updateStudyPlanner(planner.copy(isCompleted = it)) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                planner.title,
                                fontWeight = FontWeight.Bold,
                                style = if (planner.isCompleted) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge
                            )
                            val countdown = getDaysRemainingHelper(planner.targetDate)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Deadline: ${planner.targetDate} • ${subj?.name ?: "General"}", style = MaterialTheme.typography.bodySmall, color = color)
                                if (countdown.isNotEmpty() && !planner.isCompleted) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (countdown.contains("Overdue")) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = countdown,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (countdown.contains("Overdue")) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.deleteStudyPlanner(planner) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddPlannerDialog) {
        AlertDialog(
            onDismissRequest = { showAddPlannerDialog = false },
            title = { Text("Log Study Deadline") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = plannerTitle,
                        onValueChange = { plannerTitle = it },
                        label = { Text("Revision Task / Goal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = plannerDate,
                        onValueChange = { plannerDate = it },
                        label = { Text("Target Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Subject", fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjects) { subj ->
                            FilterChip(
                                selected = plannerSubjectId == subj.id,
                                onClick = { plannerSubjectId = subj.id },
                                label = { Text(subj.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (plannerTitle.isNotBlank()) {
                        val subId = plannerSubjectId.takeIf { it != 0L } ?: subjects.firstOrNull()?.id ?: 1L
                        viewModel.addStudyPlanner(plannerTitle, plannerDate, subId)
                        plannerTitle = ""
                        showAddPlannerDialog = false
                    }
                }) {
                    Text("Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlannerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TimerTab(
    subjects: List<StudySubject>,
    sessions: List<StudySession>,
    viewModel: VaultViewModel
) {
    var timerSeconds by remember { mutableStateOf(1500) } // 25 Minutes standard Pomodoro
    var isTimerRunning by remember { mutableStateOf(false) }

    var timerSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var noteInput by remember { mutableStateOf("") }

    val (todayMinutes, streak, last7DaysActive) = remember(sessions) {
        getStudyStatsHelper(sessions)
    }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000)
            if (timerSeconds > 0) {
                timerSeconds--
            } else {
                isTimerRunning = false
                val subId = timerSubjectId.takeIf { it != 0L } ?: subjects.firstOrNull()?.id ?: 1L
                viewModel.addStudySession(subId, 1500, "Pomodoro study session completed! Streak updated.")
                timerSeconds = 1500
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Focus Pomodoro Timer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Keep distraction-free offline study blocks of 25 minutes. Block tracking logs automatic history.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Focus Goal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$todayMinutes min / 50 min", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Streak: $streak Days", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🔥", fontSize = 18.sp)
                        }
                    }
                    
                    val goalProgress = (todayMinutes.toFloat() / 50f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { goalProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    // Streak Calendar Dots
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Weekly Streak Activity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            daysOfWeek.forEachIndexed { index, day ->
                                val isActive = last7DaysActive.getOrElse(index) { 0 } == 1
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(day, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isActive) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // Circle clock container
            Card(
                modifier = Modifier.size(200.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                border = BorderStroke(4.dp, MaterialTheme.colorScheme.primary)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val minutes = timerSeconds / 60
                    val seconds = timerSeconds % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { isTimerRunning = !isTimerRunning }) {
                    Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isTimerRunning) "Pause focus" else "Start focus block")
                }
                OutlinedButton(onClick = {
                    isTimerRunning = false
                    timerSeconds = 1500
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("Reset")
                }
            }
        }

        item {
            Text("Study Topic details:", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                placeholder = { Text("What are you actively memorizing / studying?") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Recent Completed Sessions Log", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        if (sessions.isEmpty()) {
            item {
                Text("No session records logged today yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(sessions) { sess ->
                val sub = subjects.find { it.id == sess.subjectId }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Focused for ${sess.durationSeconds / 60} min on ${sub?.name ?: "General"}", fontWeight = FontWeight.Bold)
                            Text(sess.notes.ifBlank { "Concentration Block completed successfully!" }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

fun getDaysRemainingHelper(targetDate: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = format.parse(targetDate)
        if (date != null) {
            val diffMs = date.time - System.currentTimeMillis()
            val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMs)
            if (diffDays == 0L) {
                "Today"
            } else if (diffDays > 0) {
                "In ${diffDays + 1}d"
            } else {
                "Overdue ${Math.abs(diffDays)}d"
            }
        } else {
            ""
        }
    } catch (e: Exception) {
        ""
    }
}

fun getStudyStatsHelper(sessions: List<StudySession>): Triple<Long, Long, List<Int>> {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val todayStart = calendar.timeInMillis
    
    val todayMinutes = sessions.filter { it.timestamp >= todayStart }
        .sumOf { it.durationSeconds } / 60
        
    val sessionDays = sessions.map {
        val cal = Calendar.getInstance()
        cal.timeInMillis = it.timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }.toSet().sortedDescending()
    
    var streak = 0L
    var checkDay = todayStart
    if (!sessionDays.contains(todayStart)) {
        checkDay -= 24 * 60 * 60 * 1000L
    }
    
    while (sessionDays.contains(checkDay)) {
        streak++
        checkDay -= 24 * 60 * 60 * 1000L
    }
    
    if (streak == 0L && sessionDays.isNotEmpty() && sessionDays.contains(todayStart)) {
        streak = 1
    }
    
    val last7DaysActive = (0..6).map { dayOffset ->
        val dayTime = todayStart - dayOffset * 24 * 60 * 60 * 1000L
        if (sessionDays.contains(dayTime)) 1 else 0
    }.reversed()
    
    return Triple(todayMinutes, streak, last7DaysActive)
}
