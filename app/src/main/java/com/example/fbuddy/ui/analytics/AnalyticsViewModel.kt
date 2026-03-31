package com.example.fbuddy.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.model.Category
import com.example.fbuddy.data.model.TransactionType
import com.example.fbuddy.data.repository.TransactionRepository
import com.example.fbuddy.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class MonthlyBar(val label: String, val total: Double)

data class AnalyticsUiState(
    val categoryTotalsMonth: Map<Category, Double> = emptyMap(),
    val dailyTotals30Days: List<Double> = emptyList(),
    val monthlyTotals6Months: List<MonthlyBar> = emptyList(),
    val topMerchants: List<Pair<String, Double>> = emptyList(),
    val thisMonthTotal: Double = 0.0,
    val lastMonthTotal: Double = 0.0,
    val isLoading: Boolean = true
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(FBuddyDatabase.getInstance(application))

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sixMonthsAgo = now - (180L * 24 * 60 * 60 * 1000)

            repository.getTransactionsInRange(sixMonthsAgo, now).collectLatest { all ->
                val debits = all.filter { it.type == TransactionType.DEBIT }

                // This month range
                val thisMonthStart = startOfCurrentMonth(now)
                val lastMonthStart = startOfLastMonth(now)

                val thisMonth = debits.filter { it.timestamp >= thisMonthStart }
                val lastMonth = debits.filter { it.timestamp in lastMonthStart until thisMonthStart }

                // Category pie chart (this month)
                val categoryTotals = thisMonth
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                // Daily bar chart (last 30 days)
                val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                val last30 = debits.filter { it.timestamp >= thirtyDaysAgo }
                val dailyTotals = (29 downTo 0).map { daysAgo ->
                    val start = DateUtils.startOfDaysAgoMillis(daysAgo.toLong(), now)
                    val end = if (daysAgo == 0) now
                    else DateUtils.startOfDaysAgoMillis((daysAgo - 1).toLong(), now)
                    last30.filter { it.timestamp in start until end }.sumOf { it.amount }
                }

                // Monthly trend (last 6 months)
                val monthlyBars = (5 downTo 0).map { monthsAgo ->
                    val start = startOfMonthAgo(now, monthsAgo)
                    val end = if (monthsAgo == 0) now else startOfMonthAgo(now, monthsAgo - 1)
                    val total = debits.filter { it.timestamp in start until end }.sumOf { it.amount }
                    val label = monthLabelAgo(now, monthsAgo)
                    MonthlyBar(label, total)
                }

                // Top 5 merchants (last 30 days)
                val topMerchants = last30
                    .filter { !it.merchant.isNullOrBlank() }
                    .groupBy { it.merchant!!.trim() }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                    .entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .map { it.key to it.value }

                _uiState.value = AnalyticsUiState(
                    categoryTotalsMonth = categoryTotals,
                    dailyTotals30Days = dailyTotals,
                    monthlyTotals6Months = monthlyBars,
                    topMerchants = topMerchants,
                    thisMonthTotal = thisMonth.sumOf { it.amount },
                    lastMonthTotal = lastMonth.sumOf { it.amount },
                    isLoading = false
                )
            }
        }
    }

    private fun startOfCurrentMonth(now: Long): Long {
        return Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun startOfLastMonth(now: Long): Long {
        return Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .withDayOfMonth(1)
            .minusMonths(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun startOfMonthAgo(now: Long, monthsAgo: Int): Long {
        return Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .withDayOfMonth(1)
            .minusMonths(monthsAgo.toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun monthLabelAgo(now: Long, monthsAgo: Int): String {
        val date = Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .minusMonths(monthsAgo.toLong())
        return date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    }
}
