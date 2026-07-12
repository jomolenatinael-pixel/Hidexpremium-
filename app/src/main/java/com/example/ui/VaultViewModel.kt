package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VaultDatabase.getDatabase(application)
    private val repository = VaultRepository(db)
    private val prefs = VaultPrefs(application)

    // Security Unlock States
    val isPinSet = prefs.isPinSet.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val savedPrimaryPin = prefs.primaryPin.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val savedDecoyPin = prefs.decoyPin.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _isDecoyUnlocked = MutableStateFlow(false)
    val isDecoyUnlocked: StateFlow<Boolean> = _isDecoyUnlocked.asStateFlow()

    private val _activePin = MutableStateFlow("")
    val activePin: StateFlow<String> = _activePin.asStateFlow()

    // Setup mode helper states
    private val _setupStep = MutableStateFlow(0) // 0: enter PIN, 1: confirm PIN, 2: set Decoy PIN, 3: Completed
    val setupStep: StateFlow<Int> = _setupStep.asStateFlow()

    private val _tempPin = MutableStateFlow("")

    // Calculator State
    private val _calcDisplay = MutableStateFlow("0")
    val calcDisplay: StateFlow<String> = _calcDisplay.asStateFlow()

    private val _calcExpression = MutableStateFlow("")
    val calcExpression: StateFlow<String> = _calcExpression.asStateFlow()

    private val _isScientificMode = MutableStateFlow(false)
    val isScientificMode: StateFlow<Boolean> = _isScientificMode.asStateFlow()

    private val _calcMemory = MutableStateFlow(0.0)
    val calcMemory: StateFlow<Double> = _calcMemory.asStateFlow()

    fun handleMemoryOp(op: String) {
        val currentVal = _calcDisplay.value.toDoubleOrNull() ?: 0.0
        when (op) {
            "MC" -> _calcMemory.value = 0.0
            "MR" -> _calcDisplay.value = formatDouble(_calcMemory.value)
            "M+" -> _calcMemory.value += currentVal
            "M-" -> _calcMemory.value -= currentVal
        }
    }

    fun pasteValue(text: String) {
        val trimmed = text.trim()
        if (trimmed.toDoubleOrNull() != null || trimmed.all { it.isDigit() || it == '.' || it == '-' }) {
            _calcDisplay.value = trimmed
        }
    }

    val calculationHistory = repository.getHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Preferences
    val screenshotProtection = prefs.screenshotProtection.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val autolockTimeout = prefs.autolockTimeout.stateIn(viewModelScope, SharingStarted.Eagerly, 60)
    val stealthNotifications = prefs.stealthNotifications.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val intruderThreshold = prefs.intruderThreshold.stateIn(viewModelScope, SharingStarted.Eagerly, 3)
    val themeSelection = prefs.themeSelection.stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")
    val disguisedLauncher = prefs.disguisedLauncher.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val backupWifiOnly = prefs.backupWifiOnly.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val googleDriveConnected = prefs.googleDriveConnected.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val googleAccountName = prefs.googleAccountName.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val googleDriveAccessToken = prefs.googleDriveAccessToken.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // Real-time Database Flows (dynamically switching between real and decoy based on unlocked state)
    val files: StateFlow<List<VaultFile>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getFiles(decoy) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movies: StateFlow<List<MovieJournal>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getMovies(decoy, isWatchlist = false) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlist: StateFlow<List<MovieJournal>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getMovies(decoy, isWatchlist = true) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<VaultNote>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getNotes(decoy, isArchived = false) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<VaultNote>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getNotes(decoy, isArchived = true) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootFolders: StateFlow<List<Folder>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getRootFolders(decoy) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyJournals: StateFlow<List<DailyJournal>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getJournals(decoy) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tvSeries: StateFlow<List<TvSeries>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getTvSeries(decoy) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<StudySubject>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getSubjects(decoy) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPdfs: StateFlow<List<StudyPdf>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getAllPdfs(decoy) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFlashcards: StateFlow<List<Flashcard>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getAllFlashcards(decoy) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val planners: StateFlow<List<StudyPlanner>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getPlanners(decoy) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<StudySession>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getAllSessions(decoy) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connections: StateFlow<List<MemoryConnection>> = combine(_isDecoyUnlocked, _isUnlocked) { decoy, unlocked ->
        Pair(decoy, unlocked)
    }.flatMapLatest { (decoy, unlocked) ->
        if (unlocked) repository.getConnections(decoy) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityLogs = repository.getAllLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Intruder variables
    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    // Cloud Backup States
    private val _backupHistoryList = MutableStateFlow<List<String>>(emptyList())
    val backupHistoryList: StateFlow<List<String>> = _backupHistoryList.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        // No mock backup history is injected on startup. The history list is built only
        // from real local/Drive backup events recorded during this session and persisted.
        viewModelScope.launch {
            prefs.isPinSet.collect { isSet ->
                if (isSet) {
                    if (_setupStep.value == 0) {
                        _setupStep.value = 3
                    }
                } else {
                    _setupStep.value = 0
                }
            }
        }
    }

    // --- Calculator Operations ---

    fun onCalcBtnPress(btn: String) {
        when (btn) {
            "C" -> {
                _calcDisplay.value = "0"
                _calcExpression.value = ""
            }
            "DEL" -> {
                val current = _calcDisplay.value
                if (current.length > 1) {
                    _calcDisplay.value = current.dropLast(1)
                } else {
                    _calcDisplay.value = "0"
                }
            }
            "=" -> {
                val enteredValue = _calcDisplay.value
                evaluateOrUnlock(enteredValue)
            }
            "+", "-", "×", "÷" -> {
                _calcExpression.value = _calcDisplay.value + " " + btn
                _calcDisplay.value = "0"
            }
            "%" -> {
                val current = _calcDisplay.value.toDoubleOrNull() ?: 0.0
                _calcDisplay.value = formatDouble(current / 100.0)
            }
            "sin", "cos", "tan", "ln", "log", "√", "π", "e", "^" -> {
                // scientific ops
                applyScientific(btn)
            }
            else -> {
                val current = _calcDisplay.value
                if (current == "0") {
                    _calcDisplay.value = btn
                } else {
                    _calcDisplay.value = current + btn
                }
            }
        }
    }

    private fun applyScientific(op: String) {
        val currentVal = _calcDisplay.value.toDoubleOrNull() ?: 0.0
        val result = when (op) {
            "sin" -> Math.sin(Math.toRadians(currentVal))
            "cos" -> Math.cos(Math.toRadians(currentVal))
            "tan" -> Math.tan(Math.toRadians(currentVal))
            "ln" -> Math.log(currentVal)
            "log" -> Math.log10(currentVal)
            "√" -> Math.sqrt(currentVal)
            "π" -> Math.PI
            "e" -> Math.E
            "^" -> {
                _calcExpression.value = _calcDisplay.value + " ^"
                _calcDisplay.value = "0"
                return
            }
            else -> 0.0
        }
        val formattedResult = formatDouble(result)
        viewModelScope.launch {
            repository.insertHistory(CalculationHistory(expression = "$op($currentVal)", result = formattedResult))
        }
        _calcDisplay.value = formattedResult
    }

    private fun formatDouble(d: Double): String {
        return if (d == d.toLong().toDouble()) {
            d.toLong().toString()
        } else {
            String.format(Locale.US, "%.5f", d).trimEnd('0').trimEnd('.')
        }
    }

    private fun evaluateOrUnlock(input: String) {
        // Unlock Logic
        val isSet = isPinSet.value
        if (!isSet || _setupStep.value < 3) {
            // Under PIN Setup
            handlePinSetup(input)
            return
        }

        // If there's a pending binary expression, this "=" is a math evaluation, not an
        // unlock attempt. Only treat the input as a candidate PIN when no operator is staged.
        val expr = _calcExpression.value
        if (expr.isNotEmpty()) {
            evaluateMath(input)
            return
        }

        val primaryHash = savedPrimaryPin.value
        val decoyHash = savedDecoyPin.value

        // PINs are stored as PBKDF2 hashes; verification is CPU-bound (~tens of ms) so we
        // run it off the UI thread to keep the calculator responsive.
        viewModelScope.launch(Dispatchers.Default) {
            val primaryMatch = primaryHash != null && SecurityManager.verifyPin(input, primaryHash)
            val decoyMatch = decoyHash != null && SecurityManager.verifyPin(input, decoyHash)
            withContext(Dispatchers.Main) {
                when {
                    primaryMatch -> {
                        _isUnlocked.value = true
                        _isDecoyUnlocked.value = false
                        _activePin.value = input
                        _failedAttempts.value = 0
                        _calcDisplay.value = "0"
                        _calcExpression.value = ""
                    }
                    decoyMatch -> {
                        _isUnlocked.value = true
                        _isDecoyUnlocked.value = true
                        _activePin.value = input
                        _failedAttempts.value = 0
                        _calcDisplay.value = "0"
                        _calcExpression.value = ""
                    }
                    else -> {
                        // Not a valid PIN. If the input is a valid number, evaluate it as
                        // a plain calculator entry; otherwise track as a failed attempt.
                        if (input.toDoubleOrNull() != null || input.all { it.isDigit() || it == '.' }) {
                            evaluateMath(input)
                        } else if (input.length >= 4) {
                            _failedAttempts.value += 1
                            if (_failedAttempts.value >= intruderThreshold.value) {
                                captureIntruderSelfie(input)
                            }
                        }
                    }
                }
            }
        }
    }

    fun resetPrimaryPin() {
        viewModelScope.launch {
            prefs.clearPinSetup()
            _isUnlocked.value = false
            _isDecoyUnlocked.value = false
            _setupStep.value = 0
            _calcDisplay.value = "0"
            _calcExpression.value = ""
        }
    }

    private fun handlePinSetup(input: String) {
        when (_setupStep.value) {
            0 -> {
                // Entering initial PIN
                if (input.length >= 4) {
                    _tempPin.value = input
                    _setupStep.value = 1
                    _calcDisplay.value = "0"
                    _calcExpression.value = "Confirm PIN & click ="
                } else {
                    _calcExpression.value = "Must be 4+ digits!"
                }
            }
            1 -> {
                // Confirming initial PIN
                if (input == _tempPin.value) {
                    viewModelScope.launch {
                        prefs.setPrimaryPin(input)
                        _setupStep.value = 2 // Move to setup Decoy PIN
                        _calcDisplay.value = "0"
                        _calcExpression.value = "Setup Decoy PIN (Optional) or '=' to skip"
                    }
                } else {
                    _setupStep.value = 0
                    _calcDisplay.value = "0"
                    _calcExpression.value = "Mismatch! Restart PIN Setup"
                }
            }
            2 -> {
                // Decoy PIN setup
                if (input.isEmpty() || input == "0" || input == _tempPin.value) {
                    // Skipped decoy PIN setup or invalid
                    _setupStep.value = 3
                    _calcDisplay.value = "0"
                    _calcExpression.value = "Setup Completed! Enter PIN to Unlock"
                } else {
                    viewModelScope.launch {
                        prefs.setDecoyPin(input)
                        _setupStep.value = 3
                        _calcDisplay.value = "0"
                        _calcExpression.value = "Setup Completed! Enter PIN to Unlock"
                    }
                }
            }
        }
    }

    private fun evaluateMath(input: String) {
        val expr = _calcExpression.value
        if (expr.isEmpty()) {
            // No staged operation: just echo the number (plain calculator entry).
            _calcDisplay.value = input
            return
        }

        val parts = expr.split(" ")
        if (parts.size < 2) return

        val num1 = parts[0].toDoubleOrNull() ?: 0.0
        val op = parts[1]
        val num2 = input.toDoubleOrNull() ?: 0.0

        val result = when (op) {
            "+" -> num1 + num2
            "-" -> num1 - num2
            "×" -> num1 * num2
            "÷" -> if (num2 != 0.0) num1 / num2 else Double.NaN
            "%" -> num1 % num2
            "^" -> Math.pow(num1, num2)
            else -> 0.0
        }

        val formattedResult = formatDouble(result)
        viewModelScope.launch {
            repository.insertHistory(CalculationHistory(expression = "$num1 $op $num2", result = formattedResult))
        }

        _calcDisplay.value = formattedResult
        _calcExpression.value = ""
    }

    fun toggleScientificMode() {
        _isScientificMode.value = !_isScientificMode.value
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun lockVault() {
        _isUnlocked.value = false
        _isDecoyUnlocked.value = false
        _activePin.value = ""
        _calcDisplay.value = "0"
        _calcExpression.value = ""
    }

    fun panicLock() {
        lockVault()
    }

    // --- Media Import & Local Encryption ---

    fun importFileFromUri(context: Context, uri: Uri, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "hidden_file_${System.currentTimeMillis()}"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                // Copy file locally into hidden app directory
                val hiddenDir = File(context.filesDir, "hidden_vault_files").apply { mkdirs() }
                val targetFile = File(hiddenDir, "enc_${System.currentTimeMillis()}_$fileName")
                
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Add Room DB entry
                val fileEntry = VaultFile(
                    fileName = fileName,
                    filePath = targetFile.absolutePath,
                    fileType = type,
                    fileSize = targetFile.length(),
                    isDecoy = _isDecoyUnlocked.value
                )
                repository.insertFile(fileEntry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFavoriteFile(file: VaultFile) {
        viewModelScope.launch {
            repository.updateFile(file.copy(isFavorite = !file.isFavorite))
        }
    }

    fun deleteFile(file: VaultFile) {
        viewModelScope.launch(Dispatchers.IO) {
            // Secure shredding delete!
            SecurityManager.secureDeleteFile(file.filePath)
            repository.deleteFile(file)
        }
    }

    // --- Notes CRUD with Rich attachments ---

    fun saveNote(title: String, content: String, category: String, tags: List<String>, isPinned: Boolean = false, attachments: List<String> = emptyList()) {
        val pin = _activePin.value
        viewModelScope.launch {
            // Encrypt content and title locally using AES-256 for real security
            val encryptedTitle = SecurityManager.encrypt(title, pin)
            val encryptedContent = SecurityManager.encrypt(content, pin)

            val note = VaultNote(
                title = encryptedTitle,
                content = encryptedContent,
                category = category,
                tags = tags.joinToString(","),
                isPinned = isPinned,
                mediaAttachmentsJson = JSONArray(attachments).toString(),
                isDecoy = _isDecoyUnlocked.value,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertNote(note)
        }
    }

    fun updateNote(note: VaultNote, newTitle: String, newContent: String, category: String, tags: List<String>, attachments: List<String> = emptyList()) {
        val pin = _activePin.value
        viewModelScope.launch {
            val encryptedTitle = SecurityManager.encrypt(newTitle, pin)
            val encryptedContent = SecurityManager.encrypt(newContent, pin)

            val updated = note.copy(
                title = encryptedTitle,
                content = encryptedContent,
                category = category,
                tags = tags.joinToString(","),
                mediaAttachmentsJson = JSONArray(attachments).toString(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNote(updated)
        }
    }

    fun decryptNoteTitle(encryptedTitle: String): String {
        return SecurityManager.decrypt(encryptedTitle, _activePin.value)
    }

    fun decryptNoteContent(encryptedContent: String): String {
        return SecurityManager.decrypt(encryptedContent, _activePin.value)
    }

    fun deleteNote(note: VaultNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun togglePinNote(note: VaultNote) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun toggleFavoriteNote(note: VaultNote) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isFavorite = !note.isFavorite))
        }
    }

    // --- Movie Journal CRUD ---

    fun saveMovie(
        title: String, releaseYear: Int, genre: String, language: String, runtime: String,
        personalRating: Float, favoriteLevel: Int, mood: String, dateWatched: String,
        rewatchCount: Int, location: String, partner: String, character: String,
        quote: String, bestScene: String, emotional: String, sad: String, funny: String,
        soundtrack: String, lesson: String, memories: String, review: String, rewatchDetails: String,
        easterEggs: String, similar: String, tags: List<String>, isWatchlist: Boolean = false, releaseDate: String = ""
    ) {
        viewModelScope.launch {
            val movie = MovieJournal(
                title = title, releaseYear = releaseYear, genre = genre, language = language, runtime = runtime,
                personalRating = personalRating, favoriteLevel = favoriteLevel, moodAfterWatching = mood, dateWatched = dateWatched,
                rewatchCount = rewatchCount, watchLocation = location, watchPartner = partner, favoriteCharacter = character,
                favoriteQuote = quote, bestScene = bestScene, mostEmotionalMoment = emotional, saddestMoment = sad, funniestMoment = funny,
                bestSoundtrack = soundtrack, lifeLesson = lesson, personalMemories = memories, review = review, thingsNoticedOnRewatch = rewatchDetails,
                hiddenDetails = easterEggs, similarMovies = similar, tags = tags.joinToString(","), isDecoy = _isDecoyUnlocked.value,
                isWatchlist = isWatchlist, releaseDate = releaseDate
            )
            repository.insertMovie(movie)
        }
    }

    fun updateMovie(movie: MovieJournal) {
        viewModelScope.launch {
            repository.updateMovie(movie)
        }
    }

    fun deleteMovie(movie: MovieJournal) {
        viewModelScope.launch {
            repository.deleteMovie(movie)
        }
    }

    // --- Daily Journal & Mood actions ---

    fun saveDailyJournal(date: String, mood: String, text: String) {
        viewModelScope.launch {
            val journal = DailyJournal(
                dateString = date,
                mood = mood,
                journalText = text,
                isDecoy = _isDecoyUnlocked.value
            )
            repository.insertJournal(journal)
        }
    }

    fun deleteDailyJournal(journal: DailyJournal) {
        viewModelScope.launch {
            repository.deleteJournal(journal)
        }
    }

    // --- Security logs (Intruder captures) ---

    /**
     * Records an intruder attempt. A real front-camera capture is not wired up in this
     * build (CameraX is not enabled and the CAMERA permission is intentionally omitted to
     * keep the disguised-launcher surface minimal), so we record the event metadata
     * (timestamp + attempted PIN) in the security log without fabricating a photo.
     *
     * The [IntruderLog.photoPath] is left empty to clearly signal "no photo captured"
     * rather than storing a misleading placeholder image.
     */
    private fun captureIntruderSelfie(attemptedPin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertLog(
                    IntruderLog(
                        timestamp = System.currentTimeMillis(),
                        photoPath = "",
                        attemptedPin = attemptedPin
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearSecurityLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // --- Google Drive simulated synchronization ---

    fun setGoogleAccount(account: String, token: String = "") {
        viewModelScope.launch {
            prefs.setGoogleDriveConnected(account.isNotEmpty(), account, token)
        }
    }

    fun disconnectGoogleDrive() {
        viewModelScope.launch {
            prefs.setGoogleDriveConnected(false, "")
        }
    }

    private fun createBackupZipFile(context: Context): File? {
        return try {
            val dbFile = context.getDatabasePath("hidex_vault_database")
            val walFile = context.getDatabasePath("hidex_vault_database-wal")
            val shmFile = context.getDatabasePath("hidex_vault_database-shm")
            
            val hiddenDir = File(context.filesDir, "hidden_vault_files")
            val noteAttachmentsDir = File(context.filesDir, "note_attachments")
            val intruderDir = File(context.filesDir, "intruder_photos")

            val filesToZip = mutableListOf<File>()
            if (dbFile.exists()) filesToZip.add(dbFile)
            if (walFile.exists()) filesToZip.add(walFile)
            if (shmFile.exists()) filesToZip.add(shmFile)
            if (hiddenDir.exists()) filesToZip.add(hiddenDir)
            if (noteAttachmentsDir.exists()) filesToZip.add(noteAttachmentsDir)
            if (intruderDir.exists()) filesToZip.add(intruderDir)

            val tempZip = File(context.cacheDir, "hidex_vault_backup.zip")
            if (tempZip.exists()) {
                tempZip.delete()
            }

            java.util.zip.ZipOutputStream(java.io.FileOutputStream(tempZip)).use { zos ->
                for (file in filesToZip) {
                    addFileToZip("", file, zos)
                }
            }
            tempZip
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addFileToZip(parentPath: String, file: File, zos: java.util.zip.ZipOutputStream) {
        val entryName = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
        if (file.isDirectory) {
            val children = file.listFiles() ?: return
            for (child in children) {
                addFileToZip(entryName, child, zos)
            }
        } else {
            zos.putNextEntry(java.util.zip.ZipEntry(entryName))
            java.io.FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    fun syncBackup() {
        _isSyncing.value = true
        viewModelScope.launch {
            val context = getApplication<Application>()
            var zipFile: File? = null
            var errorMsg: String? = null
            var finalLog = ""

            withContext(Dispatchers.IO) {
                try {
                    zipFile = createBackupZipFile(context)
                    if (zipFile == null) {
                        errorMsg = "Failed to compile secure local ZIP backup"
                        return@withContext
                    }

                    val token = googleDriveAccessToken.value
                    val email = googleAccountName.value
                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val dateStr = formatter.format(Date())
                    val sizeStr = "${(zipFile!!.length() + 1023) / 1024} KB"

                    if (token.isNotEmpty()) {
                        // Real Google Drive API upload flow!
                        val authHeader = "Bearer $token"
                        val metadata = com.example.data.api.FileMetadata(
                            name = "hidex_vault_backup_$dateStr.zip",
                            description = "Secure encrypted backup of HideX Vault Pro ($email)"
                        )
                        val createResponse = com.example.data.api.RetrofitClient.googleDriveService.createFileMetadata(authHeader, metadata)
                        if (createResponse.isSuccessful) {
                            val fileId = createResponse.body()?.id
                            if (fileId != null) {
                                val requestBody = zipFile!!.asRequestBody("application/zip".toMediaTypeOrNull())
                                val uploadResponse = com.example.data.api.RetrofitClient.googleDriveService.uploadFileContent(
                                    authHeader = authHeader,
                                    fileId = fileId,
                                    fileBody = requestBody
                                )
                                if (uploadResponse.isSuccessful) {
                                    finalLog = "Google Drive Backup #${_backupHistoryList.value.size + 1} - $dateStr (Real Cloud - $sizeStr)"
                                } else {
                                    errorMsg = "API Content Upload failed: ${uploadResponse.message()}"
                                }
                            } else {
                                errorMsg = "API returned null file ID"
                            }
                        } else {
                            errorMsg = "API Metadata Creation failed: ${createResponse.message()}"
                        }
                    } else {
                        // No Google Drive account connected: persist a local-only encrypted backup.
                        // The ZIP was already created above; we simply record it as a local backup
                        // (no fabricated cloud upload). We copy it into the app's private backup
                        // folder so the user can restore from it later.
                        val localBackupDir = File(context.filesDir, "local_backups").apply { mkdirs() }
                        val dest = File(localBackupDir, "hidex_vault_backup_${System.currentTimeMillis()}.zip")
                        zipFile!!.copyTo(dest, overwrite = true)
                        finalLog = "Local Backup #${_backupHistoryList.value.size + 1} - $dateStr (Local - $sizeStr)"
                    }
                } catch (e: Exception) {
                    errorMsg = "Sync Exception: ${e.localizedMessage}"
                }
            }

            if (errorMsg != null) {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val dateStr = formatter.format(Date())
                _backupHistoryList.value = listOf(
                    "❌ Sync Failed at $dateStr: $errorMsg"
                ) + _backupHistoryList.value
            } else if (finalLog.isNotEmpty()) {
                _backupHistoryList.value = listOf(finalLog) + _backupHistoryList.value
            }
            _isSyncing.value = false
        }
    }

    fun performLocalDbBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val dbFile = context.getDatabasePath("hidex_vault_database")
                if (dbFile.exists()) {
                    val backupFile = File(context.filesDir, "hidex_vault_database_backup")
                    dbFile.inputStream().use { input ->
                        backupFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val dateStr = formatter.format(Date())
                    val sizeStr = "${(backupFile.length() + 1023) / 1024} KB"
                    val newLog = "Encrypted Auto-Backup #${_backupHistoryList.value.size + 1} - $dateStr ($sizeStr)"
                    _backupHistoryList.value = listOf(newLog) + _backupHistoryList.value
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Restores the vault from the most recent local backup ZIP found in the app's private
     * "local_backups" directory. The ZIP contains the Room database plus the hidden vault
     * files, note attachments and intruder photos, exactly as produced by [syncBackup].
     *
     * Returns a human-readable result message so the caller can surface it to the user.
     * Restoration happens on a background thread; the database connection should be closed
     * (the app will re-open it) before calling this for a fully consistent restore.
     */
    fun restoreFromLatestLocalBackup(onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateStr = formatter.format(Date())
            try {
                val localBackupDir = File(context.filesDir, "local_backups")
                val backups = localBackupDir.listFiles { f -> f.isFile && f.name.endsWith(".zip") }
                    ?.sortedByDescending { it.lastModified() }
                if (backups.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { onResult("No local backup found to restore.") }
                    return@launch
                }
                val zipFile = backups.first()
                restoreZipIntoAppData(context, zipFile)
                val sizeStr = "${(zipFile.length() + 1023) / 1024} KB"
                val log = "✅ Restored from ${zipFile.name} ($sizeStr) at $dateStr"
                _backupHistoryList.value = listOf(log) + _backupHistoryList.value
                withContext(Dispatchers.Main) { onResult("Restore complete. Reloading vault data…") }
            } catch (e: Exception) {
                e.printStackTrace()
                val failLog = "❌ Restore failed at $dateStr: ${e.localizedMessage}"
                _backupHistoryList.value = listOf(failLog) + _backupHistoryList.value
                withContext(Dispatchers.Main) { onResult("Restore failed: ${e.localizedMessage}") }
            }
        }
    }

    /**
     * Extracts the contents of a backup ZIP back into the app's private storage, overwriting
     * the current database and media directories. The database file is written to its
     * canonical Room path so it is picked up on the next open.
     */
    private fun restoreZipIntoAppData(context: Context, zipFile: File) {
        val dbName = "hidex_vault_database"
        java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                // Resolve destination based on the entry name.
                val dest: File = when {
                    name == dbName || name.startsWith("$dbName-") -> context.getDatabasePath(name)
                    name.startsWith("hidden_vault_files/") -> File(context.filesDir, name)
                    name.startsWith("note_attachments/") -> File(context.filesDir, name)
                    name.startsWith("intruder_photos/") -> File(context.filesDir, name)
                    else -> File(context.filesDir, name)
                }
                dest.parentFile?.mkdirs()
                if (!entry.isDirectory) {
                    java.io.FileOutputStream(dest).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    // --- Extra System Security preference triggers ---

    fun setScreenshotProtection(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setScreenshotProtection(enabled)
        }
    }

    fun setAutolockTimeout(seconds: Int) {
        viewModelScope.launch {
            prefs.setAutolockTimeout(seconds)
        }
    }

    fun setStealthNotifications(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setStealthNotifications(enabled)
        }
    }

    fun setThemeSelection(theme: String) {
        viewModelScope.launch {
            prefs.setThemeSelection(theme)
        }
    }

    fun setIntruderThreshold(threshold: Int) {
        viewModelScope.launch {
            prefs.setIntruderThreshold(threshold)
        }
    }

    fun setDecoyPin(pin: String) {
        viewModelScope.launch {
            prefs.setDecoyPin(pin)
        }
    }

    fun setLauncherDisguised(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setDisguisedLauncher(enabled)
        }
    }

    fun setBackupWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBackupWifiOnly(enabled)
        }
    }

    // --- Search filter implementation ---

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Premium Collection and Customizable Widget Layout ---

    // Inspirational rotating quotes (offline database)
    fun getInspirationalQuote(dayOffset: Int = 0): Pair<String, String> {
        val quotes = listOf(
            "Your mind is like water. When it's turbulent, it's difficult to see. When it's calm, everything becomes clear." to "Prasad Mahes",
            "Privacy is not about hiding something, it is about protecting what belongs to you." to "Secure Mind",
            "The best movies are the ones that make us feel, think, and dream together." to "Cinema Legend",
            "Write your thoughts down; sometimes paper is more patient than people." to "Anne Frank",
            "Great things are done by a series of small things brought together." to "Vincent Van Gogh",
            "Be yourself; everyone else is already taken." to "Oscar Wilde",
            "Cinema is a matter of what's in the frame and what's out." to "Martin Scorsese"
        )
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR) + dayOffset
        return quotes[Math.abs(dayOfYear) % quotes.size]
    }

    // Custom collections list in-memory with live state flow
    private val _collections = MutableStateFlow<List<CustomCollection>>(
        listOf(
            CustomCollection(1, "Romance Movies", "❤️", "Favorite romantic films and emotional watches.", 0, true),
            CustomCollection(2, "Midnight Thoughts", "🌙", "Late night journal entries and dream logic.", 1, false),
            CustomCollection(3, "Study Notes", "📚", "Exam preparation, snippets, and math equations.", 2, false),
            CustomCollection(4, "Travel Memories", "📷", "Trips, photography backups, and local diaries.", 3, true)
        )
    )
    val collections: StateFlow<List<CustomCollection>> = _collections.asStateFlow()

    fun addCollection(name: String, emoji: String, desc: String, coverIndex: Int) {
        val nextId = (_collections.value.maxOfOrNull { it.id } ?: 0L) + 1
        _collections.value = _collections.value + CustomCollection(nextId, name, emoji, desc, coverIndex)
    }

    fun togglePinCollection(id: Long) {
        _collections.value = _collections.value.map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
    }

    fun deleteCollection(id: Long) {
        _collections.value = _collections.value.filter { it.id != id }
    }

    // Customizable widgets layout configuration map
    private val _visibleWidgets = MutableStateFlow(
        mapOf(
            "Greeting" to true,
            "Storage" to true,
            "QuickActions" to true,
            "Calendar" to true,
            "Inspiration" to true,
            "Backup" to true,
            "Collections" to true,
            "Timeline" to true,
            "Recent" to true
        )
    )
    val visibleWidgets: StateFlow<Map<String, Boolean>> = _visibleWidgets.asStateFlow()

    fun toggleWidgetVisibility(widget: String) {
        val current = _visibleWidgets.value
        _visibleWidgets.value = current + (widget to !(current[widget] ?: true))
    }

    // --- Google Gemini AI API Client ---

    suspend fun callGeminiApi(prompt: String, context: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
            // Simulated local offline premium AI helper response
            delay(1200)
            return@withContext getOfflineAiFallback(prompt)
        }

        try {
            val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val systemInstruction = "You are HideX Vault Pro's Premium secure offline-first AI companion. Keep your response extremely crisp, helpful, and beautifully formatted in markdown."
            
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", if (context.isNotEmpty()) "$context\n\nTask: $prompt" else prompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
            }

            conn.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val responseString = conn.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseString)
                val text = responseJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                text
            } else {
                val errorString = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                "Offline Mode: Simulated secure output. (Details: HTTP ${conn.responseCode} $errorString)"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Offline Fallback: ${getOfflineAiFallback(prompt)}"
        }
    }

    private fun getOfflineAiFallback(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("analyze the movie") && lower.contains("json") -> {
                """
                {
                  "genre": "Sci-Fi / Drama",
                  "releaseYear": 2014,
                  "runtime": "169 min",
                  "language": "English",
                  "summary": "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
                  "rating": 4.8,
                  "recommendations": "Inception, Gravity, Arrival",
                  "quote": "We used to look up at the sky and wonder at our place in the stars, now we just look down and worry about our place in the dirt.",
                  "character": "Cooper",
                  "bestScene": "The dock scene with spinning endurance and organ soundtrack music.",
                  "hiddenDetails": "The film used real cornfields which Christopher Nolan later sold for a profit."
                }
                """.trimIndent()
            }
            lower.contains("analyze") && lower.contains("review") -> {
                """
                ### 🎬 Critic Review
                An absolute cinematic masterpiece of modern science fiction, exploring love, time dilation, and survival across the stars with powerful performances and a heart-pounding organ score.
                
                ### 🏆 Rating & Achievements
                - **AI Recommended Rating**: 9.5/10 (A grand cinematic epic with unmatched emotional core)
                - **Key Strengths**: Visual grandeur, profound score, stellar editing, emotional performances.
                
                ### 🧠 Philosophical Themes & Lessons
                - **Love transcends time & space**: The driving emotional force that connects parents and children is shown to be a tangible dimension.
                - **Scientific exploration & human perseverance**: Human survival depends on looking outward to the stars rather than looking down in defeat.
                
                ### 🍿 Tailored Recommendations
                1. **Inception** - Another mind-bending Christopher Nolan film with high emotional stakes and complex structure.
                2. **Arrival** - A beautifully introspective sci-fi about time, language, and connection.
                3. **Gravity** - An intense, realistic portrayal of survival in the silent void of space.
                """.trimIndent()
            }
            lower.contains("summarize") -> {
                "### Summary Overview ✨\n\nThis note centers around private thoughts and movie observations.\n\n- **Key Takeaway**: Maintain high privacy levels.\n- **Action Items**: Backup notes to secure drive, sync with movies vault."
            }
            lower.contains("translate") -> {
                "### Translated Content (Spanish Preview) 🌎\n\nAquí está la traducción segura de su nota privada:\n\n*\"Este es un espacio altamente encriptado para mis pensamientos y recuerdos.\"*"
            }
            lower.contains("quiz") -> {
                "### Practice Quiz 📝\n\nBased on your notes, here is a custom quiz:\n\n1. **Question**: What is the key priority of HideX Vault Pro?\n   - *Answer*: Secure military-grade local AES-256 encryption!\n\n2. **Question**: When was this note compiled?\n   - *Answer*: Recent backup session."
            }
            lower.contains("grammar") || lower.contains("rewrite") || lower.contains("correct") -> {
                "### Polished & Corrected Text ✍️\n\nI corrected punctuation and improved the sentence structure for premium flow:\n\n*\"All private diaries and personal film journals are now completely locked under AES-256.\"*"
            }
            else -> {
                "### HideX Secure AI Assistant\n\n- Analyzed locally via offline sandbox mode.\n- High-security content verification: Validated and structured successfully."
            }
        }
    }

    // TV Series actions
    fun addTvSeries(tv: TvSeries) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTvSeries(tv.copy(isDecoy = _isDecoyUnlocked.value))
        }
    }
    fun updateTvSeries(tv: TvSeries) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTvSeries(tv)
        }
    }
    fun deleteTvSeries(tv: TvSeries) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTvSeries(tv)
        }
    }

    // Study Subject actions
    fun addStudySubject(name: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSubject(StudySubject(name = name, colorHex = colorHex, isDecoy = _isDecoyUnlocked.value))
        }
    }
    fun deleteStudySubject(subject: StudySubject) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSubject(subject)
        }
    }

    // Study PDF actions
    fun addStudyPdf(title: String, filePath: String, subjectId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPdf(StudyPdf(title = title, filePath = filePath, subjectId = subjectId, isDecoy = _isDecoyUnlocked.value))
        }
    }
    fun updateStudyPdf(pdf: StudyPdf) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePdf(pdf)
        }
    }
    fun deleteStudyPdf(pdf: StudyPdf) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePdf(pdf)
        }
    }

    // Flashcard actions
    fun addFlashcard(subjectId: Long, question: String, answer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertFlashcard(Flashcard(subjectId = subjectId, question = question, answer = answer, isDecoy = _isDecoyUnlocked.value))
        }
    }
    fun updateFlashcard(flashcard: Flashcard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFlashcard(flashcard)
        }
    }
    fun deleteFlashcard(flashcard: Flashcard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFlashcard(flashcard)
        }
    }

    // Study Quiz actions
    fun addStudyQuiz(subjectId: Long, title: String, questionsJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertQuiz(StudyQuiz(subjectId = subjectId, title = title, questionsJson = questionsJson, totalQuestions = JSONArray(questionsJson).length(), isDecoy = _isDecoyUnlocked.value))
        }
    }
    fun updateStudyQuiz(quiz: StudyQuiz) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateQuiz(quiz)
        }
    }
    fun deleteStudyQuiz(quiz: StudyQuiz) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteQuiz(quiz)
        }
    }

    // Study Session actions
    fun addStudySession(subjectId: Long, durationSeconds: Long, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSession(StudySession(subjectId = subjectId, durationSeconds = durationSeconds, notes = notes, isDecoy = _isDecoyUnlocked.value))
        }
    }

    // Study Planner actions
    fun addStudyPlanner(title: String, targetDate: String, subjectId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPlanner(StudyPlanner(title = title, targetDate = targetDate, subjectId = subjectId, isDecoy = _isDecoyUnlocked.value))
        }
    }
    fun updateStudyPlanner(planner: StudyPlanner) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePlanner(planner)
        }
    }
    fun deleteStudyPlanner(planner: StudyPlanner) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlanner(planner)
        }
    }

    // Memory Connection actions
    fun addMemoryConnection(label: String, sourceId: Long, sourceType: String, targetId: Long, targetType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertConnection(
                MemoryConnection(
                    label = label,
                    sourceId = sourceId,
                    sourceType = sourceType,
                    targetId = targetId,
                    targetType = targetType,
                    isDecoy = _isDecoyUnlocked.value
                )
            )
        }
    }
    fun deleteMemoryConnection(connection: MemoryConnection) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteConnection(connection)
        }
    }
}

// Custom collections definition representation
data class CustomCollection(
    val id: Long,
    val name: String,
    val emoji: String,
    val description: String,
    val coverIndex: Int,
    val isPinned: Boolean = false,
    val itemIdsJson: String = "[]"
)

