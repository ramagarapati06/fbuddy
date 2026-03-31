package com.example.fbuddy.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.db.TransactionEntity
import com.example.fbuddy.data.model.TransactionType
import com.example.fbuddy.data.repository.TransactionRepository
import com.example.fbuddy.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val todayTotal: Double = 0.0,
    val todayCount: Int = 0,
    val yesterdayTotal: Double = 0.0,
    val sevenDayAvg: Double = 0.0,
    val monthTotal: Double = 0.0,
    val sevenDayBars: List<Double> = List(7) { 0.0 },
    val recentTransactions: List<TransactionEntity> = emptyList(),
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TransactionRepository(FBuddyDatabase.getInstance(app))

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            val todayStart     = DateUtils.startOfDayMillis(now)
            val yesterdayStart = DateUtils.startOfDaysAgoMillis(1, now)
            val last7Start     = DateUtils.startOfDaysAgoMillis(7, now)
            val monthStart     = DateUtils.startOfMonthMillis(now)

            combine(
                repo.getTransactionsInRange(todayStart, now),
                repo.getTransactionsInRange(yesterdayStart, todayStart - 1),
                repo.getTransactionsInRange(last7Start, now),
                repo.getTransactionsInRange(monthStart, now),
                repo.getPagedTransactions(10, 0),
            ) { today, yesterday, last7, month, recent ->

                val todayDebits     = today.filter { it.type == TransactionType.DEBIT }
                val yesterdayDebits = yesterday.filter { it.type == TransactionType.DEBIT }
                val last7Debits     = last7.filter { it.type == TransactionType.DEBIT }
                val monthDebits     = month.filter { it.type == TransactionType.DEBIT }

                // Build 7 bars — one per day, oldest first
                val bars = (6 downTo 0).map { daysAgo ->
                    val dayStart = DateUtils.startOfDaysAgoMillis(daysAgo.toLong(), now)
                    val dayEnd   = if (daysAgo == 0) now
                    else DateUtils.startOfDaysAgoMillis((daysAgo - 1).toLong(), now) - 1
                    last7.filter {
                        it.type == TransactionType.DEBIT &&
                                it.timestamp in dayStart..dayEnd
                    }.sumOf { it.amount }
                }

                HomeUiState(
                    todayTotal          = todayDebits.sumOf { it.amount },
                    todayCount          = todayDebits.size,
                    yesterdayTotal      = yesterdayDebits.sumOf { it.amount },
                    sevenDayAvg         = if (last7Debits.isEmpty()) 0.0
                    else last7Debits.sumOf { it.amount } / 7.0,
                    monthTotal          = monthDebits.sumOf { it.amount },
                    sevenDayBars        = bars,
                    recentTransactions  = recent.take(5),
                )
            }.collect { _uiState.value = it }
        }
    }
}


