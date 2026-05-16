package com.autoagents.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoagents.app.App
import com.autoagents.app.data.db.InsightEntity
import com.autoagents.app.data.llm.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InsightsUiState(
    val isGenerating: Boolean = false,
    val error: String? = null
)

class InsightsViewModel : ViewModel() {
    private val app = App.get()
    private val dao = app.database.insightDao()
    private val articleDao = app.database.articleDao()

    val insights: StateFlow<List<InsightEntity>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _ui = MutableStateFlow(InsightsUiState())
    val ui: StateFlow<InsightsUiState> = _ui

    fun generate() {
        if (_ui.value.isGenerating) return
        viewModelScope.launch {
            _ui.value = InsightsUiState(isGenerating = true)
            val cfg = app.settings.current()
            val recentSnapshot = articleDao.observeRecent(20).first()
            val summary = recentSnapshot.joinToString("\n---\n") { a ->
                "Title: ${a.title}\nSummary: ${a.summary}"
            }.ifBlank { "(아직 저장된 기사가 없습니다.)" }

            val system = ChatMessage(
                "system",
                "You are a strategic Korean tech analyst. " +
                        "Based on the recent articles, write a SINGLE Markdown brief in Korean with sections: " +
                        "## 오늘의 흐름, ## 주목할 포인트 3가지, ## 다음 주에 살펴볼 키워드. " +
                        "Topics of interest: ${cfg.keywords}."
            )
            val user = ChatMessage("user", "최근 기사 요약 모음:\n$summary")
            val resp = app.llmClient.chat(listOf(system, user), maxTokens = 1800)
            if (!resp.ok) {
                _ui.value = InsightsUiState(isGenerating = false, error = resp.errorMessage)
                return@launch
            }
            dao.insert(InsightEntity(content = resp.content!!, createdAt = System.currentTimeMillis()))
            _ui.value = InsightsUiState(isGenerating = false)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}
