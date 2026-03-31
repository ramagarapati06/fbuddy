package com.example.fbuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: UserCategoryEntity): Long

    @Update
    suspend fun update(override: UserCategoryEntity)

    @Query("SELECT * FROM user_categories")
    fun getAll(): Flow<List<UserCategoryEntity>>

    @Query("SELECT * FROM user_categories WHERE normalizedMerchant = :normalized LIMIT 1")
    suspend fun findByNormalizedMerchant(normalized: String): UserCategoryEntity?

    @Query("DELETE FROM user_categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}

