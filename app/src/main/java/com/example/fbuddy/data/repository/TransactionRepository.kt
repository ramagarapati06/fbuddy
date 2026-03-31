package com.example.fbuddy.data.repository

import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.db.TransactionEntity
import com.example.fbuddy.data.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository exposing transaction data to the rest of the app.
 *
 * This wires together Room DAOs and will later incorporate SMS/OCR sources.
 */
class TransactionRepository(
    private val db: FBuddyDatabase
) {

    private val transactionDao = db.transactionDao()

    fun getPagedTransactions(limit: Int, offset: Int): Flow<List<Transaction>> {
        return transactionDao.getPagedTransactions(limit, offset)
    }

    fun getTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsInRange(startMillis, endMillis)
    }

    suspend fun upsert(transaction: TransactionEntity): Long {
        return transactionDao.upsert(transaction)
    }

    suspend fun upsertAll(transactions: List<TransactionEntity>): List<Long> {
        return transactionDao.upsertAll(transactions)
    }

    suspend fun deleteById(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun getById(id: Long): Transaction? {
        return transactionDao.getById(id)
    }

    suspend fun clearAll() {
        transactionDao.clearAll()
    }
}

