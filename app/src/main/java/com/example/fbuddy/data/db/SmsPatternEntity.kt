package com.example.fbuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Custom or built-in SMS regex patterns that can be toggled.
 */
@Entity(tableName = "sms_patterns")
data class SmsPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val bankName: String?,
    val pattern: String,
    val description: String? = null,
    val isActive: Boolean = true
)

