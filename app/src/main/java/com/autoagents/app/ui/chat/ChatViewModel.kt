package com.autoagents.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoagents.app.App
import com.autoagents.app.data.db.ChatMessageEntity
import com.autoagents.app.data.llm.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val isSending: Boolean = false,
    val error: String? = null
)

class ChatViewModel : ViewModel() {

    private val app = App.get()
    private val dao = app.database.chatDao()

    val messages: StateFlow<List<ChatMessageEntity>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _ui = MutableStateFlow(ChatUiState())
    val ui: StateFlow<ChatUiState> = _ui

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _ui.value.isSending) return
        viewModelScope.launch {
            _ui.value = ChatUiState(isSending = true)
            val now = System.currentTimeMillis()
            dao.insert(ChatMessageEntity(role = "user", content = trimmed, createdAt = now))
            val cfg = app.settings.current()
            val system = ChatMessage(
                "system",
                "You are a helpful Korean AI research assistant. " +
                        "Topics of interest: ${cfg.keywords}. " +
                        "Reply in the user's language (Korean by default). " +
                        "Use Markdown for headings, bullet points, and emphasis when helpful."
            )
            val history = messages.value.takeLast(10).map { ChatMessage(it.role, it.content) }
            val userMsg = ChatMessage("user", trimmed)
            val resp = app.llmClient.chat(listOf(system) + history + userMsg)
            if (resp.ok) {
                dao.insert(
                    ChatMessageEntity(
                        role = "assistant",
                        content = resp.content!!,
                        createdAt = System.currentTimeMillis()
                    )
                )
                _ui.value = ChatUiState(isSending = false)
            } else {
                _ui.value = ChatUiState(isSending = false, error = resp.errorMessage)
            }
        }
    }

    fun clear() {
        viewModelScope.launch { dao.clear() }
    }
}
