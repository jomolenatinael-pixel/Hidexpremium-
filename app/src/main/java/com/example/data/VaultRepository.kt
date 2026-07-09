package com.example.data

import kotlinx.coroutines.flow.Flow

class VaultRepository(private val db: VaultDatabase) {

    private val fileDao = db.fileDao()
    private val movieDao = db.movieDao()
    private val noteDao = db.noteDao()
    private val folderDao = db.folderDao()
    private val intruderDao = db.intruderDao()
    private val historyDao = db.historyDao()
    private val journalDao = db.journalDao()

    // File actions
    fun getFiles(isDecoy: Boolean) = fileDao.getFiles(isDecoy)
    fun getFilesByType(type: String, isDecoy: Boolean) = fileDao.getFilesByType(type, isDecoy)
    fun getFavorites(isDecoy: Boolean) = fileDao.getFavorites(isDecoy)
    suspend fun insertFile(file: VaultFile) = fileDao.insertFile(file)
    suspend fun updateFile(file: VaultFile) = fileDao.updateFile(file)
    suspend fun deleteFile(file: VaultFile) = fileDao.deleteFile(file)
    suspend fun deleteFileById(id: Long) = fileDao.deleteFileById(id)

    // Movie actions
    fun getMovies(isDecoy: Boolean, isWatchlist: Boolean) = movieDao.getMovies(isDecoy, isWatchlist)
    fun getMovieById(id: Long) = movieDao.getMovieById(id)
    suspend fun insertMovie(movie: MovieJournal) = movieDao.insertMovie(movie)
    suspend fun updateMovie(movie: MovieJournal) = movieDao.updateMovie(movie)
    suspend fun deleteMovie(movie: MovieJournal) = movieDao.deleteMovie(movie)

    // Note actions
    fun getNotes(isDecoy: Boolean, isArchived: Boolean) = noteDao.getNotes(isDecoy, isArchived)
    fun getNoteById(id: Long) = noteDao.getNoteById(id)
    fun getNotesInFolder(folderId: Long, isDecoy: Boolean) = noteDao.getNotesInFolder(folderId, isDecoy)
    fun getRootNotes(isDecoy: Boolean) = noteDao.getRootNotes(isDecoy)
    suspend fun insertNote(note: VaultNote) = noteDao.insertNote(note)
    suspend fun updateNote(note: VaultNote) = noteDao.updateNote(note)
    suspend fun deleteNote(note: VaultNote) = noteDao.deleteNote(note)

    // Folder actions
    fun getSubFolders(parentId: Long, isDecoy: Boolean) = folderDao.getSubFolders(parentId, isDecoy)
    fun getRootFolders(isDecoy: Boolean) = folderDao.getRootFolders(isDecoy)
    suspend fun insertFolder(folder: Folder) = folderDao.insertFolder(folder)
    suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)

    // Intruder actions
    fun getAllLogs() = intruderDao.getAllLogs()
    suspend fun insertLog(log: IntruderLog) = intruderDao.insertLog(log)
    suspend fun clearLogs() = intruderDao.clearLogs()

    // Calculator actions
    fun getHistory() = historyDao.getHistory()
    suspend fun insertHistory(history: CalculationHistory) = historyDao.insertHistory(history)
    suspend fun clearHistory() = historyDao.clearHistory()

    // Journal actions
    fun getJournals(isDecoy: Boolean) = journalDao.getJournals(isDecoy)
    suspend fun insertJournal(journal: DailyJournal) = journalDao.insertJournal(journal)
    suspend fun deleteJournal(journal: DailyJournal) = journalDao.deleteJournal(journal)
}
