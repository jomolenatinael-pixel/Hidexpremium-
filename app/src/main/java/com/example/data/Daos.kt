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

@Dao
interface TvSeriesDao {
    @Query("SELECT * FROM tv_series WHERE isDecoy = :isDecoy ORDER BY id DESC")
    fun getTvSeries(isDecoy: Boolean): Flow<List<TvSeries>>

    @Query("SELECT * FROM tv_series WHERE id = :id")
    fun getTvSeriesById(id: Long): Flow<TvSeries?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTvSeries(tvSeries: TvSeries): Long

    @Update
    suspend fun updateTvSeries(tvSeries: TvSeries)

    @Delete
    suspend fun deleteTvSeries(tvSeries: TvSeries)
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_subjects WHERE isDecoy = :isDecoy ORDER BY name ASC")
    fun getSubjects(isDecoy: Boolean): Flow<List<StudySubject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: StudySubject): Long

    @Delete
    suspend fun deleteSubject(subject: StudySubject)

    @Query("SELECT * FROM study_pdfs WHERE subjectId = :subjectId AND isDecoy = :isDecoy ORDER BY title ASC")
    fun getPdfsForSubject(subjectId: Long, isDecoy: Boolean): Flow<List<StudyPdf>>

    @Query("SELECT * FROM study_pdfs WHERE isDecoy = :isDecoy ORDER BY id DESC")
    fun getAllPdfs(isDecoy: Boolean): Flow<List<StudyPdf>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: StudyPdf): Long

    @Update
    suspend fun updatePdf(pdf: StudyPdf)

    @Delete
    suspend fun deletePdf(pdf: StudyPdf)

    @Query("SELECT * FROM flashcards WHERE subjectId = :subjectId AND isDecoy = :isDecoy")
    fun getFlashcardsForSubject(subjectId: Long, isDecoy: Boolean): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE isDecoy = :isDecoy")
    fun getAllFlashcards(isDecoy: Boolean): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard): Long

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)

    @Query("SELECT * FROM study_quizzes WHERE subjectId = :subjectId AND isDecoy = :isDecoy")
    fun getQuizzesForSubject(subjectId: Long, isDecoy: Boolean): Flow<List<StudyQuiz>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: StudyQuiz): Long

    @Update
    suspend fun updateQuiz(quiz: StudyQuiz)

    @Delete
    suspend fun deleteQuiz(quiz: StudyQuiz)

    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId AND isDecoy = :isDecoy ORDER BY timestamp DESC")
    fun getSessionsForSubject(subjectId: Long, isDecoy: Boolean): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE isDecoy = :isDecoy ORDER BY timestamp DESC")
    fun getAllSessions(isDecoy: Boolean): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Query("SELECT * FROM study_planners WHERE isDecoy = :isDecoy ORDER BY targetDate ASC")
    fun getPlanners(isDecoy: Boolean): Flow<List<StudyPlanner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanner(planner: StudyPlanner): Long

    @Update
    suspend fun updatePlanner(planner: StudyPlanner)

    @Delete
    suspend fun deletePlanner(planner: StudyPlanner)
}

@Dao
interface MemoryConnectionDao {
    @Query("SELECT * FROM memory_connections WHERE isDecoy = :isDecoy ORDER BY timestamp DESC")
    fun getConnections(isDecoy: Boolean): Flow<List<MemoryConnection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: MemoryConnection): Long

    @Delete
    suspend fun deleteConnection(connection: MemoryConnection)
}

