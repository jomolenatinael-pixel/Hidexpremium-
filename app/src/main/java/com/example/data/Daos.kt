package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultFileDao {
    @Query("SELECT * FROM vault_files WHERE isDecoy = :isDecoy ORDER BY timestamp DESC")
    fun getFiles(isDecoy: Boolean): Flow<List<VaultFile>>

    @Query("SELECT * FROM vault_files WHERE fileType = :type AND isDecoy = :isDecoy ORDER BY timestamp DESC")
    fun getFilesByType(type: String, isDecoy: Boolean): Flow<List<VaultFile>>

    @Query("SELECT * FROM vault_files WHERE isFavorite = 1 AND isDecoy = :isDecoy ORDER BY timestamp DESC")
    fun getFavorites(isDecoy: Boolean): Flow<List<VaultFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VaultFile): Long

    @Update
    suspend fun updateFile(file: VaultFile)

    @Delete
    suspend fun deleteFile(file: VaultFile)

    @Query("DELETE FROM vault_files WHERE id = :id")
    suspend fun deleteFileById(id: Long)
}

@Dao
interface MovieDao {
    @Query("SELECT * FROM movie_journals WHERE isDecoy = :isDecoy AND isWatchlist = :isWatchlist ORDER BY id DESC")
    fun getMovies(isDecoy: Boolean, isWatchlist: Boolean): Flow<List<MovieJournal>>

    @Query("SELECT * FROM movie_journals WHERE id = :id")
    fun getMovieById(id: Long): Flow<MovieJournal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieJournal): Long

    @Update
    suspend fun updateMovie(movie: MovieJournal)

    @Delete
    suspend fun deleteMovie(movie: MovieJournal)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM vault_notes WHERE isDecoy = :isDecoy AND isArchived = :isArchived ORDER BY updatedAt DESC")
    fun getNotes(isDecoy: Boolean, isArchived: Boolean): Flow<List<VaultNote>>

    @Query("SELECT * FROM vault_notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<VaultNote?>

    @Query("SELECT * FROM vault_notes WHERE folderId = :folderId AND isDecoy = :isDecoy ORDER BY updatedAt DESC")
    fun getNotesInFolder(folderId: Long, isDecoy: Boolean): Flow<List<VaultNote>>

    @Query("SELECT * FROM vault_notes WHERE folderId IS NULL AND isDecoy = :isDecoy ORDER BY updatedAt DESC")
    fun getRootNotes(isDecoy: Boolean): Flow<List<VaultNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: VaultNote): Long

    @Update
    suspend fun updateNote(note: VaultNote)

    @Delete
    suspend fun deleteNote(note: VaultNote)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE parentId = :parentId AND isDecoy = :isDecoy ORDER BY name ASC")
    fun getSubFolders(parentId: Long, isDecoy: Boolean): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentId IS NULL AND isDecoy = :isDecoy ORDER BY name ASC")
    fun getRootFolders(isDecoy: Boolean): Flow<List<Folder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Delete
    suspend fun deleteFolder(folder: Folder)
}

@Dao
interface IntruderDao {
    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<IntruderLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: IntruderLog): Long

    @Query("DELETE FROM intruder_logs")
    suspend fun clearLogs()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<CalculationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CalculationHistory): Long

    @Query("DELETE FROM calculation_history")
    suspend fun clearHistory()
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM daily_journals WHERE isDecoy = :isDecoy ORDER BY dateString DESC")
    fun getJournals(isDecoy: Boolean): Flow<List<DailyJournal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: DailyJournal): Long

    @Delete
    suspend fun deleteJournal(journal: DailyJournal)
}
