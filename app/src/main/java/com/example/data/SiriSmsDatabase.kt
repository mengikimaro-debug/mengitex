package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SmsMessageEntity::class,
        MessageTemplateEntity::class,
        SavedContactEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SiriSmsDatabase : RoomDatabase() {
    abstract fun dao(): SiriSmsDao

    companion object {
        @Volatile
        private var INSTANCE: SiriSmsDatabase? = null

        fun getInstance(context: Context): SiriSmsDatabase {
            val appCtx = context.applicationContext
            com.example.security.VaultCryptoManager.registerContextOnly(appCtx)
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    appCtx,
                    SiriSmsDatabase::class.java,
                    "sirisms_encrypted_vault.db"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .addCallback(object : Callback() {
                        override fun onDestructiveMigration(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onDestructiveMigration(db)
                            runCatching {
                                appCtx.getSharedPreferences("mengitex_db_meta", Context.MODE_PRIVATE)
                                    .edit().putBoolean("db_indexes_v1_ready", false).apply()
                            }
                        }

                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onOpen(db)
                            runCatching {
                                // Fast SQLite WAL tuning
                                db.execSQL("PRAGMA synchronous = NORMAL")
                                db.execSQL("PRAGMA temp_store = MEMORY")
                                db.execSQL("PRAGMA cache_size = -16384")
                                db.execSQL("PRAGMA mmap_size = 67108864")
                                db.execSQL("PRAGMA wal_autocheckpoint = 2000")

                                val dbMeta = appCtx.getSharedPreferences("mengitex_db_meta", Context.MODE_PRIVATE)
                                if (!dbMeta.getBoolean("db_indexes_v1_ready", false)) {
                                    db.execSQL(
                                        "CREATE INDEX IF NOT EXISTS idx_sms_active_timestamp ON sms_messages(isDeleted, status, timestamp DESC)"
                                    )
                                    db.execSQL(
                                        "CREATE INDEX IF NOT EXISTS idx_sms_phone ON sms_messages(phoneNumber)"
                                    )
                                    db.execSQL(
                                        "CREATE INDEX IF NOT EXISTS idx_sms_deleted ON sms_messages(isDeleted, deletedAt DESC)"
                                    )
                                    dbMeta.edit().putBoolean("db_indexes_v1_ready", true).apply()
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
