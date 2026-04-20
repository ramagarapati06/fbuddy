package com.example.fbuddy.ui.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.db.TransactionEntity
import com.example.fbuddy.data.model.Category
import com.example.fbuddy.data.model.TransactionSource
import com.example.fbuddy.data.model.TransactionType
import com.example.fbuddy.data.repository.TransactionRepository
import com.example.fbuddy.notifications.TransactionNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanUiState(
    val extractedText: String = "",
    val parsed: ParsedReceipt? = null,
    // Editable fields — kept separate so edits don't mutate ParsedReceipt
    val editedMerchant: String = "",
    val editedAmount: String = "",
    val editedCategory: Category = Category.OTHER,
    val isSaving: Boolean = false,
    val lastSavedId: Long? = null,
    val error: String? = null,
    // Controls the unknown-merchant category picker dialog
    val showCategoryPicker: Boolean = false
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(FBuddyDatabase.getInstance(application))
    private val notificationManager = TransactionNotificationManager(application)

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    // ── Called by OCR result ──────────────────────────────────────────────────

    fun setExtractedText(text: String) {
        val parsed = if (text.isBlank()) null else ReceiptParser.parse(text)
        val isUnknownMerchant = parsed?.merchant == null || parsed.merchant.isBlank()

        _uiState.value = _uiState.value.copy(
            extractedText      = text,
            parsed             = parsed,
            editedMerchant     = parsed?.merchant ?: "",
            editedAmount       = parsed?.totalAmount?.let { "%.2f".format(it) } ?: "",
            editedCategory     = parsed?.category ?: Category.OTHER,
            error              = null,
            lastSavedId        = null,
            // Show category picker immediately if merchant is unknown
            showCategoryPicker = isUnknownMerchant && parsed != null
        )
    }

    // ── Edit handlers (called from UI Edit clicks) ────────────────────────────

    fun onMerchantEdited(value: String) {
        _uiState.value = _uiState.value.copy(editedMerchant = value)
    }

    fun onAmountEdited(value: String) {
        _uiState.value = _uiState.value.copy(editedAmount = value)
    }

    fun onCategorySelected(category: Category) {
        _uiState.value = _uiState.value.copy(
            editedCategory     = category,
            showCategoryPicker = false
        )
    }

    fun dismissCategoryPicker() {
        _uiState.value = _uiState.value.copy(showCategoryPicker = false)
    }

    fun openCategoryPicker() {
        _uiState.value = _uiState.value.copy(showCategoryPicker = true)
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    fun saveAsTransaction() {
        val state  = _uiState.value
        val parsed = state.parsed ?: return
        val amount = state.editedAmount.toDoubleOrNull()
            ?: parsed.totalAmount
            ?: return

        val merchant = state.editedMerchant.ifBlank { parsed.merchant }
        val category = state.editedCategory

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val entity = TransactionEntity(
                    amount    = amount,
                    type      = TransactionType.DEBIT,
                    merchant  = merchant,
                    category  = category,
                    timestamp = System.currentTimeMillis(),
                    source    = TransactionSource.RECEIPT,
                    rawText   = parsed.rawText
                )
                val id = repository.upsert(entity)
                _uiState.value = _uiState.value.copy(isSaving = false, lastSavedId = id)

                // Fire notification after successful save
                notificationManager.showTransactionNotification(
                    merchant = merchant ?: "Unknown Merchant",
                    amount   = amount,
                    category = category,
                    source   = "Receipt"
                )
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error    = t.message ?: "Failed to save receipt"
                )
            }
        }
    }
}
