package com.example.fbuddy.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.db.TransactionEntity
import com.example.fbuddy.data.repository.TransactionRepository
import com.example.fbuddy.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val groupedTransactions: Map<String, List<TransactionEntity>> = emptyMap(),
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val isLoading: Boolean = true,
)

class TransactionsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TransactionRepository(FBuddyDatabase.getInstance(app))

    private val _search = MutableStateFlow("")
    private val _filter = MutableStateFlow("All")

    val uiState: StateFlow<TransactionsUiState> = combine(
        repo.getPagedTransactions(500, 0),
        _search,
        _filter
    ) { all, query, filter ->
        val filtered = all
            .filter { txn ->
                val matchSearch = query.isBlank() ||
                        txn.merchant?.contains(query, ignoreCase = true) == true ||
                        txn.rawText?.contains(query, ignoreCase = true) == true ||
                        txn.amount.toString().contains(query)
                val matchFilter = when (filter) {
                    "SMS"      -> txn.source == com.example.fbuddy.data.model.TransactionSource.SMS
                    "Receipt"  -> txn.source == com.example.fbuddy.data.model.TransactionSource.RECEIPT
                    "Food"     -> txn.category == com.example.fbuddy.data.model.Category.FOOD_DINING
                    "Travel"   -> txn.category == com.example.fbuddy.data.model.Category.TRAVEL_TRANSPORT
                    "Shopping" -> txn.category == com.example.fbuddy.data.model.Category.SHOPPING
                    "Health"   -> txn.category == com.example.fbuddy.data.model.Category.HEALTH_MEDICAL
                    else       -> true
                }
                matchSearch && matchFilter
            }
            .sortedByDescending { it.timestamp }

        val grouped = filtered.groupBy { DateUtils.friendlyDateLabel(it.timestamp) }

        TransactionsUiState(
            groupedTransactions = grouped,
            searchQuery         = query,
            selectedFilter      = filter,
            isLoading           = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())

    fun onSearchQuery(q: String) { _search.value = q }
    fun onFilter(f: String) { _filter.value = f }
    fun delete(id: Long) { viewModelScope.launch { repo.deleteById(id) } }
}
