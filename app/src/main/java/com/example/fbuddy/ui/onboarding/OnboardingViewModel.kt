package com.example.fbuddy.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fbuddy.data.db.UserProfileEntity
import com.example.fbuddy.data.repository.UserProfileRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = UserProfileRepository(app)
    private val gson = Gson()

    private val _data = MutableStateFlow(OnboardingData())
    val data: StateFlow<OnboardingData> = _data.asStateFlow()

    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step.asStateFlow()

    private val _isDone = MutableStateFlow(false)
    val isDone: StateFlow<Boolean> = _isDone.asStateFlow()

    val totalSteps = 6

    fun updateData(updated: OnboardingData) {
        _data.value = updated
    }

    fun nextStep(updated: OnboardingData) {
        _data.value = updated
        _step.value = (_step.value + 1).coerceAtMost(totalSteps - 1)
    }

    fun prevStep() {
        _step.value = (_step.value - 1).coerceAtLeast(0)
    }

    fun complete(final: OnboardingData) {
        _data.value = final
        viewModelScope.launch {
            repo.saveProfile(final.toEntity())
            _isDone.value = true
        }
    }

    // ── Convert OnboardingData → Room entity ─────────────────────────────
    private fun OnboardingData.toEntity(): UserProfileEntity {
        val catMap = categoryBudgets
            .mapKeys { it.key.name }
            .mapValues { it.value.toIntOrNull() ?: 0 }

        return UserProfileEntity(
            id                  = 1,
            fullName            = fullName,
            city                = city,
            monthlySalary       = monthlySalary.toIntOrNull() ?: 0,
            salaryFrequency     = salaryFrequency.name,
            rent                = rent.toIntOrNull() ?: 0,
            emiLoans            = emiLoans.toIntOrNull() ?: 0,
            subscriptions       = subscriptions.toIntOrNull() ?: 0,
            utilities           = utilities.toIntOrNull() ?: 0,
            savingsGoal         = savingsGoal.toIntOrNull() ?: 0,
            savingsPurpose      = savingsPurpose.name,
            monthlyBudget       = monthlyBudget.toIntOrNull() ?: 0,
            categoryBudgetsJson = gson.toJson(catMap),
            currencySymbol      = currencySymbol,
            enableSmsTracking   = enableSmsTracking,
            enableOcrScanning   = enableOcrScanning,
            notifyOnOverspend   = notifyOnOverspend,
            onboardingComplete  = true
        )
    }
}
