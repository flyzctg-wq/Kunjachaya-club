package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        FinancialRecordEntity::class,
        AnnouncementEntity::class,
        ComplaintEntity::class,
        ActivityEntity::class,
        ActivityLogEntity::class,
        EventEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun financialDao(): FinancialDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun activityDao(): ActivityDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // NOTE (DevOps): fallbackToDestructiveMigration() is fine while iterating on
                // the schema pre-launch, because it silently deletes and recreates the local
                // DB on any version bump. Once this app holds real resident data in the field,
                // replace this with explicit Migration(x, y) objects — otherwise a future schema
                // change will wipe every resident's local cache (Firestore remains the source
                // of truth, so data isn't permanently lost, but it forces a full re-sync and
                // will look like data loss to users on a bad connection).
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kunjachhaya_club_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
