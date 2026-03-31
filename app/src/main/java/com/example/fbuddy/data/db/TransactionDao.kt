package com.example.fbuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query(
        """
        SELECT * FROM transactions
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getPagedTransactions(limit: Int, offset: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE timestamp BETWEEN :startMillis AND :endMillis
        ORDER BY timestamp DESC
        """
    )
    fun getTransactionsInRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE smsMessageId = :smsId
        LIMIT 1
        """
    )
    suspend fun findBySmsMessageId(smsId: Long): TransactionEntity?

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

