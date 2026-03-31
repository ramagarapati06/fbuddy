package com.example.fbuddy.ui.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.db.TransactionEntity
import com.example.fbuddy.data.model.TransactionSource
import com.example.fbuddy.data.model.TransactionType
import com.example.fbuddy.data.repository.TransactionRepository
import com.example.fbuddy.utils.Categorizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanUiState(
    val extractedText: String = "",
    val parsed: ParsedReceipt? = null,
    val isSaving: Boolean = false,
    val lastSavedId: Long? = null,
    val error: String? = null
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository =
        TransactionRepository(FBuddyDatabase.getInstance(application))

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun setExtractedText(text: String) {
        val parsed = if (text.isBlank()) null else ReceiptParser.parse(text)
        _uiState.value = _uiState.value.copy(
            extractedText = text,
            parsed = parsed,
            error = null,
            lastSavedId = null
        )
    }

    fun saveAsTransaction() {
        val parsed = _uiState.value.parsed ?: return
        val amount = parsed.totalAmount ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            try {
                val category = Categorizer.categorize(parsed.merchant, parsed.rawText)
                val entity = TransactionEntity(
                    amount = amount,
                    type = TransactionType.DEBIT,
                    merchant = parsed.merchant,
                    category = category,
                    timestamp = System.currentTimeMillis(),
                    source = TransactionSource.RECEIPT,
                    rawText = parsed.rawText
                )
                val id = repository.upsert(entity)
                _uiState.value = _uiState.value.copy(isSaving = false, lastSavedId = id)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = t.message ?: "Failed to save receipt"
                )
            }
        }
    }
}

