package com.example.fbuddy.ui.onboarding

// ── Data collected across all 6 onboarding steps ─────────────────────────────
data class OnboardingData(
    // Step 1
    val fullName: String = "",
    val city: String = "",
    // Step 2
    val monthlySalary: String = "",
    val salaryFrequency: SalaryFrequency = SalaryFrequency.MONTHLY,
    // Step 3
    val rent: String = "",
    val emiLoans: String = "",
    val subscriptions: String = "",
    val utilities: String = "",
    // Step 4
    val savingsGoal: String = "",
    val savingsPurpose: SavingsPurpose = SavingsPurpose.EMERGENCY_FUND,
    // Step 5
    val monthlyBudget: String = "",
    val categoryBudgets: Map<SpendCategory, String> = emptyMap(),
    // Step 6
    val currencySymbol: String = "₹",
    val enableSmsTracking: Boolean = true,
    val enableOcrScanning: Boolean = true,
    val notifyOnOverspend: Boolean = true,
)

enum class SalaryFrequency(val label: String) {
    WEEKLY("Weekly"),
    BIWEEKLY("Every 2 weeks"),
    MONTHLY("Monthly"),
    VARIABLE("Variable / Freelance")
}

enum class SavingsPurpose(val label: String, val emoji: String) {
    EMERGENCY_FUND("Emergency Fund",   "🛡️"),
    VACATION(      "Vacation / Travel","✈️"),
    GADGET(        "Phone / Gadget",   "📱"),
    VEHICLE(       "Vehicle",          "🚗"),
    EDUCATION(     "Education",        "📚"),
    INVESTMENT(    "Investment",       "📈"),
    OTHER(         "Other",            "🎯"),
}

enum class SpendCategory(val label: String, val emoji: String) {
    FOOD(         "Food & Dining",  "🍕"),
    GROCERY(      "Groceries",      "🛒"),
    TRAVEL(       "Travel",         "🚗"),
    SHOPPING(     "Shopping",       "🛍️"),
    HEALTH(       "Health",         "💊"),
    UTILITIES(    "Utilities",      "⚡"),
    ENTERTAINMENT("Entertainment",  "🎬"),
}

// ── Feasibility check logic ───────────────────────────────────────────────────
sealed class FeasibilityResult {
    data class OK(val message: String, val dailyAllowance: Int) : FeasibilityResult()
    data class Warn(val message: String, val suggestion: String) : FeasibilityResult()
    data class Bad(val message: String, val suggestion: String) : FeasibilityResult()
}

fun checkFeasibility(data: OnboardingData): FeasibilityResult? {
    val salary = data.monthlySalary.toIntOrNull() ?: return null
    val rent   = data.rent.toIntOrNull() ?: 0
    val emi    = data.emiLoans.toIntOrNull() ?: 0
    val subs   = data.subscriptions.toIntOrNull() ?: 0
    val utils  = data.utilities.toIntOrNull() ?: 0
    val save   = data.savingsGoal.toIntOrNull() ?: return null
    if (salary == 0 || save == 0) return null

    val totalFixed = rent + emi + subs + utils
    val savePct    = (save.toFloat() / salary * 100).toInt()
    val disposable = salary - totalFixed - save
    val daily      = disposable / 30

    return when {
        totalFixed + save > salary ->
            FeasibilityResult.Bad(
                message    = "Fixed costs + savings exceed your salary by ₹${fmt(totalFixed + save - salary)}.",
                suggestion = "Try reducing savings to ₹${fmt((salary - totalFixed) / 5)} (20%)."
            )
        savePct > 60 ->
            FeasibilityResult.Bad(
                message    = "Saving ${savePct}% (₹${fmt(save)}) leaves almost nothing for daily expenses.",
                suggestion = "A healthy target is 20–30%. Try ₹${fmt(salary / 4)}."
            )
        savePct > 40 || disposable < 10000 ->
            FeasibilityResult.Warn(
                message    = "Ambitious! You'll have ₹${fmt(disposable)}/mo for daily expenses.",
                suggestion = "That's ₹${fmt(daily)}/day — doable but tight."
            )
        else ->
            FeasibilityResult.OK(
                message        = "Great! You'll have ₹${fmt(disposable)}/mo (₹${fmt(daily)}/day) for expenses.",
                dailyAllowance = daily
            )
    }
}

private fun fmt(n: Int): String =
    "%,d".format(n)