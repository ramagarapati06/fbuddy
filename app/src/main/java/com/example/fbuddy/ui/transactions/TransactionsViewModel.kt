package com.example.fbuddy.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.db.TransactionEntity
import com.example.fbuddy.data.model.Category
import com.example.fbuddy.data.model.TransactionSource
import com.example.fbuddy.data.repository.TransactionRepository
import com.example.fbuddy.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingCategoryPick(
    val transactionId: Long,
    val merchantName: String?,
    val amount: Double,
)

data class TransactionsUiState(
    val groupedTransactions: Map<String, List<TransactionEntity>> = emptyMap(),
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val isLoading: Boolean = true,
    // Banner for uncategorized transactions
    val pendingCategoryPick: PendingCategoryPick? = null,
)

class TransactionsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TransactionRepository(FBuddyDatabase.getInstance(app))
    private val dao  = FBuddyDatabase.getInstance(app).transactionDao()

    private val _search = MutableStateFlow("")
    private val _filter = MutableStateFlow("All")

    // Track which "Other" transactions we've already shown a banner for
    private val shownBannerIds = mutableSetOf<Long>()

    val uiState: StateFlow<TransactionsUiState> = combine(
        repo.getPagedTransactions(500, 0),
        _search,
        _filter
    ) { all, query, filter ->

        val allList = all.toList()

        // Filter based on search + category filter
        val filtered = allList
            .filter { txn ->
                val matchSearch = query.isBlank() ||
                        txn.merchant?.contains(query, ignoreCase = true) == true ||
                        txn.rawText?.contains(query, ignoreCase = true) == true ||
                        txn.amount.toString().contains(query)

                val matchFilter = when (filter) {
                    "SMS"         -> txn.source == TransactionSource.SMS
                    "Receipt"     -> txn.source == TransactionSource.RECEIPT
                    "Food"        -> txn.category == Category.FOOD_DINING
                    "Travel"      -> txn.category == Category.TRAVEL_TRANSPORT
                    "Shopping"    -> txn.category == Category.SHOPPING
                    "Health"      -> txn.category == Category.HEALTH_MEDICAL
                    "Groceries"   -> txn.category == Category.GROCERIES
                    "Utilities"   -> txn.category == Category.UTILITIES_BILLS
                    else          -> true
                }
                matchSearch && matchFilter
            }
            .sortedByDescending { it.timestamp }

        val grouped = filtered.groupBy { DateUtils.friendlyDateLabel(it.timestamp) }

        // Find the most recent "Other" transaction we haven't shown a banner for yet
        val pendingPick = allList
            .filter { it.category == Category.OTHER && it.id !in shownBannerIds }
            .maxByOrNull { it.timestamp }
            ?.let { txn ->
                shownBannerIds.add(txn.id)
                PendingCategoryPick(
                    transactionId = txn.id,
                    merchantName  = txn.merchant,
                    amount        = txn.amount,
                )
            }

        TransactionsUiState(
            groupedTransactions  = grouped,
            searchQuery          = query,
            selectedFilter       = filter,
            isLoading            = false,
            pendingCategoryPick  = pendingPick,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())

    fun onSearchQuery(q: String) { _search.value = q }
    fun onFilter(f: String) { _filter.value = f }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteById(id) }
    }

    /** Called when user taps a category chip in the banner */
    fun updateCategory(transactionId: Long, category: Category) {
        viewModelScope.launch {
            val txn = dao.getById(transactionId) ?: return@launch
            dao.upsert(txn.copy(category = category))
        }
    }

    /** Called when user dismisses the banner without picking */
    fun dismissBanner(transactionId: Long) {
        shownBannerIds.add(transactionId)
    }
}
