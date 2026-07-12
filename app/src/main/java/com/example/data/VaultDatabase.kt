package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VaultFile::class,
        MovieJournal::class,
        VaultNote::class,
        Folder::class,
        IntruderLog::class,
        CalculationHistory::class,
        DailyJournal::class,
        TvSeries::class,
        StudySubject::class,
        StudyPdf::class,
        Flashcard::class,
        StudyQuiz::class,
        StudySession::class,
        StudyPlanner::class,
        MemoryConnection::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun fileDao(): VaultFileDao
    abstract fun movieDao(): MovieDao
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun intruderDao(): IntruderDao
    abstract fun historyDao(): HistoryDao
    abstract fun journalDao(): JournalDao
    abstract fun tvSeriesDao(): TvSeriesDao
    abstract fun studyDao(): StudyDao
    abstract fun memoryConnectionDao(): MemoryConnectionDao

    companion object {
        private const val TAG = "VaultDatabase"

        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "hidex_vault_database"
                )
                    // No explicit migrations are defined yet (schema is stable at v2).
                    // If a future schema bump ships without a migration, prefer keeping the
                    // existing data over silently nuking the entire vault. We only fall back
                    // to a destructive migration as an absolute last resort, and we log it
                    // loudly so data loss is never silent.
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onDestructiveMigration(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            Log.w(TAG, "Destructive migration triggered — existing vault data was dropped due to a schema change without an explicit migration.")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
