package com.example.fbuddy.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.fbuddy.data.model.Category

/**
 * Stores user overrides mapping merchants to categories.
 */
@Entity(
    tableName = "user_categories",
    indices = [Index(value = ["normalizedMerchant"], unique = true)]
)
data class UserCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val merchant: String,
    val normalizedMerchant: String,
    val category: Category
)

