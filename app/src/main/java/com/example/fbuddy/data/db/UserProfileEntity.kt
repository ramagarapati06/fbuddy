package com.example.fbuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "",
    val city: String = "",
    val monthlySalary: Int = 0,
    val salaryFrequency: String = "MONTHLY",
    val rent: Int = 0,
    val emiLoans: Int = 0,
    val subscriptions: Int = 0,
    val utilities: Int = 0,
    val savingsGoal: Int = 0,
    val savingsPurpose: String = "EMERGENCY_FUND",
    val monthlyBudget: Int = 0,
    val categoryBudgetsJson: String = "{}",
    val currencySymbol: String = "₹",
    val enableSmsTracking: Boolean = true,
    val enableOcrScanning: Boolean = true,
    val notifyOnOverspend: Boolean = true,
    val onboardingComplete: Boolean = false
)