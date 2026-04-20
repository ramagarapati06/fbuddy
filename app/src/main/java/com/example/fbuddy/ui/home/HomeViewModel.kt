package com.example.fbuddy.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.db.TransactionEntity
import com.example.fbuddy.data.model.TransactionType
import com.example.fbuddy.data.repository.TransactionRepository
import com.example.fbuddy.data.repository.UserProfileRepository
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
    val userName: String = "",
    val monthlyBudget: Int = 25000,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val txnRepo     = TransactionRepository(FBuddyDatabase.getInstance(app))
    private val profileRepo = UserProfileRepository(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            val todayStart     = DateUtils.startOfDayMillis(now)
            val yesterdayStart = DateUtils.startOfDaysAgoMillis(1, now)
            val last7Start     = DateUtils.startOfDaysAgoMillis(7, now)
            val monthStart     = DateUtils.startOfMonthMillis(now)

            // Step 1: combine the 5 transaction flows (max allowed by Kotlin combine)
            val txnFlow: Flow<HomeUiState> = combine(
                txnRepo.getTransactionsInRange(todayStart, now),
                txnRepo.getTransactionsInRange(yesterdayStart, todayStart - 1),
                txnRepo.getTransactionsInRange(last7Start, now),
                txnRepo.getTransactionsInRange(monthStart, now),
                txnRepo.getPagedTransactions(10, 0),
            ) { today, yesterday, last7, month, recent ->

                val todayList     = today.toList()
                val yesterdayList = yesterday.toList()
                val last7List     = last7.toList()
                val monthList     = month.toList()
                val recentList    = recent.toList()

                val todayDebits     = todayList.filter     { it.type == TransactionType.DEBIT }
                val yesterdayDebits = yesterdayList.filter { it.type == TransactionType.DEBIT }
                val last7Debits     = last7List.filter     { it.type == TransactionType.DEBIT }
                val monthDebits     = monthList.filter     { it.type == TransactionType.DEBIT }

                // Build 7 bars — index 0 = 6 days ago, index 6 = today
                val bars: List<Double> = (6 downTo 0).map { daysAgo ->
                    val dayStart = DateUtils.startOfDaysAgoMillis(daysAgo.toLong(), now)
                    val dayEnd   = if (daysAgo == 0) now
                                   else DateUtils.startOfDaysAgoMillis((daysAgo - 1).toLong(), now) - 1
                    last7List.filter { txn ->
                        txn.type == TransactionType.DEBIT &&
                        txn.timestamp in dayStart..dayEnd
                    }.sumOf { txn -> txn.amount }
                }

                HomeUiState(
                    todayTotal         = todayDebits.sumOf { it.amount },
                    todayCount         = todayDebits.size,
                    yesterdayTotal     = yesterdayDebits.sumOf { it.amount },
                    sevenDayAvg        = if (last7Debits.isEmpty()) 0.0
                                         else last7Debits.sumOf { it.amount } / 7.0,
                    monthTotal         = monthDebits.sumOf { it.amount },
                    sevenDayBars       = bars,
                    recentTransactions = recentList.take(5),
                )
            }

            // Step 2: combine the transaction state with the profile flow
            txnFlow.combine(profileRepo.observeProfile()) { state, profile ->
                state.copy(
                    userName      = profile?.fullName?.ifBlank { "there" } ?: "there",
                    monthlyBudget = if ((profile?.monthlyBudget ?: 0) > 0)
                                        profile!!.monthlyBudget
                                    else 25000,
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun refresh() {
        // Flows are already reactive — this just triggers a recomposition
        _uiState.value = _uiState.value.copy()
    }
}
