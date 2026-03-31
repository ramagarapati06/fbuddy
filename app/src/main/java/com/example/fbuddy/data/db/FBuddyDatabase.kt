package com.example.fbuddy.data.db

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        TransactionEntity::class,
        SmsPatternEntity::class,
        UserCategoryEntity::class,
        UserProfileEntity::class       // ← ADD THIS LINE
    ],
    version = 2,                       // ← BUMP version from 1 to 2
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FBuddyDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun smsPatternDao(): SmsPatternDao
    abstract fun userCategoryDao(): UserCategoryDao
    abstract fun userProfileDao(): UserProfileDao  // ← ADD THIS LINE

    companion object {
        @Volatile
        private var INSTANCE: FBuddyDatabase? = null

        fun getInstance(context: Context): FBuddyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FBuddyDatabase::class.java,
                    "fbuddy.db"
                )
                .fallbackToDestructiveMigration() // ← ADD THIS — handles version bump
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
