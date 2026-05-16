package com.autoagents.app.ui.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoagents.app.App
import com.autoagents.app.data.db.ArticleEntity
import com.autoagents.app.data.llm.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class YoutubeUiState(
    val isProcessing: Boolean = false,
    val message: String? = null,
    val videoId: String? = null,
    val summary: String? = null,
    val savedArticleId: Long? = null
)

class YoutubeViewModel : ViewModel() {

    private val app = App.get()
    private val articleDao = app.database.articleDao()

    private val _ui = MutableStateFlow(YoutubeUiState())
    val ui: StateFlow<YoutubeUiState> = _ui

    fun run(url: String) {
        if (_ui.value.isProcessing) return
        val raw = url.trim()
        if (raw.isEmpty()) return
        viewModelScope.launch {
            _ui.value = YoutubeUiState(isProcessing = true, message = "유튜브 자막을 가져오는 중…")
            val tx = app.youtubeClient.fetchTranscript(raw)
            if (!tx.ok) {
                _ui.value = YoutubeUiState(
                    isProcessing = false,
                    message = "자막 수집 실패: ${tx.errorMessage}",
                    videoId = tx.videoId.takeIf { it.isNotBlank() }
                )
                return@launch
            }
            _ui.value = _ui.value.copy(
                videoId = tx.videoId,
                message = "LLM으로 요약 생성 중…"
            )
            val cfg = app.settings.current()
            val system = ChatMessage(
                "system",
                "You are a Korean tech video summarizer. Read the auto transcript carefully. " +
                        "Produce a well-structured Markdown summary in Korean with: " +
                        "## 핵심 요약 (3-5 bullets), ## 본문 정리 (3-5 짧은 단락), ## 핵심 키워드 (쉼표로 구분된 영문 태그)." +
                        " Topics of interest: ${cfg.keywords}."
            )
            val user = ChatMessage(
                "user",
                "Video URL: https://www.youtube.com/watch?v=${tx.videoId}\n\nTranscript:\n${tx.transcript.take(12000)}"
            )
            val resp = app.llmClient.chat(
                listOf(system, user),
                modelOverride = cfg.ytModel.takeIf { it.isNotBlank() },
                maxTokens = 2000
            )
            if (!resp.ok) {
                _ui.value = _ui.value.copy(
                    isProcessing = false,
                    message = "요약 실패: ${resp.errorMessage}"
                )
                return@launch
            }
            val summary = resp.content!!.trim()
            val article = ArticleEntity(
                title = "[YouTube] ${tx.videoId}",
                summary = summary.lineSequence().filter { it.isNotBlank() }.take(2).joinToString(" "),
                content = summary,
                source = "YouTube",
                sourceUrl = "https://www.youtube.com/watch?v=${tx.videoId}",
                imageUrl = "https://img.youtube.com/vi/${tx.videoId}/hqdefault.jpg",
                tags = null,
                isYoutube = true,
                videoId = tx.videoId,
                publishedAt = System.currentTimeMillis()
            )
            val newId = articleDao.insert(article)
            _ui.value = YoutubeUiState(
                isProcessing = false,
                message = "요약이 저장되었습니다.",
                videoId = tx.videoId,
                summary = summary,
                savedArticleId = newId
            )
        }
    }

    fun clear() {
        _ui.value = YoutubeUiState()
    }
}
