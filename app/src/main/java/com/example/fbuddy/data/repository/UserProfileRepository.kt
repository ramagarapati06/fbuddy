package com.example.fbuddy.data.repository

import android.content.Context
import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.db.UserProfileEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(context: Context) {

    private val dao  = FBuddyDatabase.getInstance(context).userProfileDao()
    private val gson = Gson()

    // ── Read ──────────────────────────────────────────────────────────────
    suspend fun getProfile(): UserProfileEntity? = dao.getProfile()

    fun observeProfile(): Flow<UserProfileEntity?> = dao.observeProfile()

    suspend fun isOnboardingComplete(): Boolean =
        dao.isOnboardingComplete() == true

    // ── Write ─────────────────────────────────────────────────────────────
    suspend fun saveProfile(profile: UserProfileEntity) = dao.upsertProfile(profile)

    // ── Helpers ───────────────────────────────────────────────────────────
    fun decodeCategoryBudgets(json: String): Map<String, Int> {
        return try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun encodeCategoryBudgets(map: Map<String, Int>): String =
        gson.toJson(map)

    // ── Computed helpers used by chatbot + analytics ──────────────────────
    suspend fun getDisposableIncome(): Int {
        val p = getProfile() ?: return 0
        val fixed = p.rent + p.emiLoans + p.subscriptions + p.utilities
        return (p.monthlySalary - fixed - p.savingsGoal).coerceAtLeast(0)
    }

    suspend fun getDailyBudget(): Int {
        val p = getProfile() ?: return 0
        return if (p.monthlyBudget > 0) p.monthlyBudget / 30
               else getDisposableIncome() / 30
    }
}
