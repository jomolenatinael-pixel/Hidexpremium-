package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_files")
data class VaultFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileType: String, // PHOTO, VIDEO, DOC, AUDIO
    val albumName: String = "Default",
    val fileSize: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isDecoy: Boolean = false
)

@Entity(tableName = "movie_journals")
data class MovieJournal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val posterUrl: String = "",
    val bannerUrl: String = "",
    val releaseYear: Int = 0,
    val genre: String = "",
    val language: String = "",
    val runtime: String = "",
    val personalRating: Float = 0f,
    val favoriteLevel: Int = 0, // Red Hearts ❤️ count (e.g., 0 to 5)
    val moodAfterWatching: String = "",
    val dateWatched: String = "",
    val rewatchCount: Int = 0,
    val watchLocation: String = "",
    val watchPartner: String = "",
    val favoriteCharacter: String = "",
    val favoriteQuote: String = "",
    val bestScene: String = "",
    val mostEmotionalMoment: String = "",
    val saddestMoment: String = "",
    val funniestMoment: String = "",
    val bestSoundtrack: String = "",
    val lifeLesson: String = "",
    val personalMemories: String = "",
    val review: String = "",
    val thingsNoticedOnRewatch: String = "",
    val hiddenDetails: String = "",
    val similarMovies: String = "",
    val tags: String = "",
    val screenshotsJson: String = "[]",
    val trailerLink: String = "",
    val wouldRecommend: Boolean = true,
    val isDecoy: Boolean = false,
    val isWatchlist: Boolean = false,
    val releaseDate: String = "" // For upcoming movie countdowns (e.g., YYYY-MM-DD)
)

@Entity(tableName = "vault_notes")
data class VaultNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String, // Markdown or rich structure
    val folderId: Long? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val tags: String = "",
    val category: String = "General",
    val mediaAttachmentsJson: String = "[]", // JSON array of attachment item paths
    val updatedAt: Long = System.currentTimeMillis(),
    val isDecoy: Boolean = false
)

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val isDecoy: Boolean = false
)

@Entity(tableName = "intruder_logs")
data class IntruderLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val photoPath: String = "", // Simulated photo path
    val attemptedPin: String = ""
)

@Entity(tableName = "calculation_history")
data class CalculationHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_journals")
data class DailyJournal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // YYYY-MM-DD
    val mood: String, // happy, sad, energetic, calm, etc.
    val journalText: String,
    val isDecoy: Boolean = false
)

@Entity(tableName = "tv_series")
data class TvSeries(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val posterUrl: String = "",
    val currentSeason: Int = 1,
    val currentEpisode: Int = 1,
    val totalSeasons: Int = 1,
    val isFinished: Boolean = false,
    val favoriteEpisode: String = "",
    val favoriteCharacter: String = "",
    val bestSeason: String = "",
    val personalReview: String = "",
    val watchHistoryJson: String = "[]", // JSON representation of watch history list
    val nextSeasonReleaseDate: String = "", // Countdown until next season (YYYY-MM-DD)
    val bannerUrl: String = "",
    val rating: Float = 0f,
    val moodAfterWatching: String = "",
    val tags: String = "",
    val isDecoy: Boolean = false
)

@Entity(tableName = "study_subjects")
data class StudySubject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#4A00E0",
    val isDecoy: Boolean = false
)

@Entity(tableName = "study_pdfs")
data class StudyPdf(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val subjectId: Long,
    val bookmarksJson: String = "[]", // List of integers (page numbers)
    val highlightsJson: String = "[]", // List of highlighting text/page records
    val isDecoy: Boolean = false
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val question: String,
    val answer: String,
    val isFavorite: Boolean = false,
    val reviewCount: Int = 0,
    val nextReviewTime: Long = System.currentTimeMillis(),
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val isDecoy: Boolean = false
)

@Entity(tableName = "study_quizzes")
data class StudyQuiz(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val questionsJson: String, // list of questions/options/answers
    val lastScore: Int = -1,
    val totalQuestions: Int = 0,
    val isDecoy: Boolean = false
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isDecoy: Boolean = false
)

@Entity(tableName = "study_planners")
data class StudyPlanner(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetDate: String, // YYYY-MM-DD
    val subjectId: Long,
    val isCompleted: Boolean = false,
    val isDecoy: Boolean = false
)

@Entity(tableName = "memory_connections")
data class MemoryConnection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String, // Custom description of memory link, e.g. "Watched during summer break"
    val sourceId: Long,
    val sourceType: String, // MOVIE, TV_SERIES, NOTE, PHOTO, VIDEO, JOURNAL, STUDY_PDF
    val targetId: Long,
    val targetType: String, // MOVIE, TV_SERIES, NOTE, PHOTO, VIDEO, JOURNAL, STUDY_PDF
    val timestamp: Long = System.currentTimeMillis(),
    val isDecoy: Boolean = false
)
