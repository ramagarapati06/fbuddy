package com.example.fbuddy.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fbuddy.data.db.FBuddyDatabase
import com.example.fbuddy.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val role: Role,
    val text: String
) {
    enum class Role { USER, BOT }
}

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(ChatMessage.Role.BOT, "Ask me things like “How much did I spend today?”")
    ),
    val isThinking: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(FBuddyDatabase.getInstance(application))
    private val engine = ChatEngine(repository)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun send(message: String) {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + ChatMessage(ChatMessage.Role.USER, trimmed),
            isThinking = true
        )

        viewModelScope.launch {
            val reply = runCatching { engine.answer(trimmed) }
                .getOrElse { "Sorry—something went wrong reading your data." }

            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + ChatMessage(ChatMessage.Role.BOT, reply),
                isThinking = false
            )
        }
    }
}

