package com.example.fbuddy.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.fbuddy.data.model.Category
import com.example.fbuddy.data.model.TransactionSource
import com.example.fbuddy.data.model.TransactionType

/**
 * Persistent representation of a financial transaction.
 *
 * This is the single source of truth for all transactions (SMS + receipts).
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index("timestamp"),
        Index("category"),
        Index("source"),
        Index("merchant"),
        Index("smsMessageId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amount: Double,
    val type: TransactionType,
    val merchant: String?,
    val category: Category,
    /**
     * UTC epoch millis for when the transaction occurred.
     */
    val timestamp: Long,
    val source: TransactionSource,
    /**
     * Raw SMS body or OCR text backing this transaction.
     */
    val rawText: String?,
    val notes: String? = null,
    val bankName: String? = null,
    val accountLast4: String? = null,
    val currencyCode: String? = "INR",
    val referenceId: String? = null,
    /**
     * SMS provider message ID used to avoid double ingestion.
     */
    val smsMessageId: Long? = null
)

