package com.example.fbuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsPatternDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pattern: SmsPatternEntity): Long

    @Query("SELECT * FROM sms_patterns WHERE isActive = 1")
    fun getActivePatterns(): Flow<List<SmsPatternEntity>>

    @Query("SELECT * FROM sms_patterns")
    fun getAllPatterns(): Flow<List<SmsPatternEntity>>

    @Update
    suspend fun update(pattern: SmsPatternEntity)

    @Query("DELETE FROM sms_patterns WHERE id = :id")
    suspend fun deleteById(id: Long)
}

