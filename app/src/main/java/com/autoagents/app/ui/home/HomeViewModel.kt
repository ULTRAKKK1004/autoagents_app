package com.autoagents.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoagents.app.App
import com.autoagents.app.data.db.ArticleEntity
import com.autoagents.app.data.llm.ChatMessage
import com.autoagents.app.data.rss.RssClient
import com.autoagents.app.data.rss.RssItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class HomeUiState(
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val refreshedCount: Int = 0
)

class HomeViewModel : ViewModel() {

    private val app = App.get()
    private val articleDao = app.database.articleDao()

    val articles: StateFlow<List<ArticleEntity>> =
        articleDao.observeRecent(60)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun refresh() {
        if (_ui.value.isLoading) return
        viewModelScope.launch {
            _ui.value = HomeUiState(isLoading = true, statusMessage = "RSS 피드 수집 중…")
            val items = app.rssClient.fetchAll(RssClient.DEFAULT_SOURCES, perSource = 3)
            if (items.isEmpty()) {
                _ui.value = HomeUiState(isLoading = false, statusMessage = "RSS에서 새 글을 가져오지 못했습니다.")
                return@launch
            }
            _ui.value = _ui.value.copy(statusMessage = "${items.size}건 수신, LLM으로 요약 중…")

            var added = 0
            val cfg = app.settings.current()
            for ((index, item) in items.withIndex()) {
                if (articleDao.existsBySourceUrl(item.link) > 0) continue
                val article = synthesize(item, cfg.keywords) ?: continue
                articleDao.insert(article)
                added++
                _ui.value = _ui.value.copy(statusMessage = "처리 ${index + 1}/${items.size} (저장 $added)")
            }
            _ui.value = HomeUiState(
                isLoading = false,
                statusMessage = if (added == 0) "새로운 기사가 없습니다." else "${added}개 기사 추가됨.",
                refreshedCount = added
            )
        }
    }

    private suspend fun synthesize(item: RssItem, keywords: String): ArticleEntity? {
        val sysPrompt = """
            You are a Korean tech blog writer. Your interests: ${keywords}.
            Given a news item (title + description + url), produce a JSON object with these exact keys:
            title_ko (Korean title), summary_ko (2-3 sentences Korean), content_ko (Markdown article body in Korean, 4-6 short paragraphs), tags (comma-separated 3-6 keywords in English).
            Return ONLY the JSON object, no markdown fences, no commentary.
        """.trimIndent()
        val userText = buildString {
            appendLine("Title: ${item.title}")
            appendLine("Source: ${item.source}")
            appendLine("URL: ${item.link}")
            appendLine()
            appendLine("Description:")
            appendLine(item.description.take(2000))
        }
        val resp = app.llmClient.chat(
            messages = listOf(
                ChatMessage("system", sysPrompt),
                ChatMessage("user", userText)
            ),
            maxTokens = 1500
        )
        if (!resp.ok) return null
        val raw = resp.content!!.trim()
        val cleaned = stripFences(raw)
        val parsed = runCatching {
            val obj = json.parseToJsonElement(cleaned).jsonObject
            val titleKo = obj["title_ko"]?.jsonPrimitive?.content?.trim().orEmpty().ifBlank { item.title }
            val summaryKo = obj["summary_ko"]?.jsonPrimitive?.content?.trim().orEmpty()
            val contentKo = obj["content_ko"]?.jsonPrimitive?.content?.trim().orEmpty()
            val tags = obj["tags"]?.jsonPrimitive?.content?.trim()
            Triple(titleKo, summaryKo, contentKo) to tags
        }.getOrNull() ?: return null
        val (triple, tags) = parsed
        val (titleKo, summaryKo, contentKo) = triple
        if (contentKo.isBlank()) return null
        return ArticleEntity(
            title = titleKo,
            summary = summaryKo,
            content = contentKo,
            source = item.source,
            sourceUrl = item.link,
            imageUrl = item.imageUrl,
            tags = tags,
            isYoutube = false,
            videoId = null,
            publishedAt = System.currentTimeMillis()
        )
    }

    private fun stripFences(text: String): String {
        var t = text.trim()
        if (t.startsWith("```")) {
            val firstNewline = t.indexOf('\n')
            if (firstNewline > 0) t = t.substring(firstNewline + 1)
        }
        if (t.endsWith("```")) {
            t = t.substring(0, t.length - 3)
        }
        val start = t.indexOf('{')
        val end = t.lastIndexOf('}')
        return if (start in 0..<end) t.substring(start, end + 1) else t
    }
}
