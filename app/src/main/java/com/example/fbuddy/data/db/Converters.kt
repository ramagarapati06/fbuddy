package com.example.fbuddy.data.db

import androidx.room.TypeConverter
import com.example.fbuddy.data.model.Category
import com.example.fbuddy.data.model.TransactionSource
import com.example.fbuddy.data.model.TransactionType

/**
 * Room type converters for enums.
 */
class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? =
        value?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun fromCategory(value: Category?): String? = value?.name

    @TypeConverter
    fun toCategory(value: String?): Category? =
        value?.let { Category.valueOf(it) }

    @TypeConverter
    fun fromSource(value: TransactionSource?): String? = value?.name

    @TypeConverter
    fun toSource(value: String?): TransactionSource? =
        value?.let { TransactionSource.valueOf(it) }
}

