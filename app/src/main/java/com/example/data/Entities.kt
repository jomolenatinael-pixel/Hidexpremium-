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
