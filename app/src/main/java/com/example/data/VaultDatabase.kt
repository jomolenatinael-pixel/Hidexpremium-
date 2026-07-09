package com.example.data

import android.content.Context
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
        DailyJournal::class
    ],
    version = 1,
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

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "hidex_vault_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
