package com.autoagents.app.data.youtube

import com.autoagents.app.data.prefs.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class YoutubeTranscript(
    val videoId: String,
    val transcript: String,
    val errorMessage: String? = null
) {
    val ok: Boolean get() = transcript.isNotBlank() && errorMessage == null
}

class YoutubeClient(private val settings: SettingsRepository) {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun extractVideoId(url: String): String? {
        if (url.isBlank()) return null
        val patterns = listOf(
            Regex("[?&]v=([A-Za-z0-9_-]{6,})"),
            Regex("youtu\\.be/([A-Za-z0-9_-]{6,})"),
            Regex("/shorts/([A-Za-z0-9_-]{6,})"),
            Regex("/embed/([A-Za-z0-9_-]{6,})")
        )
        for (p in patterns) {
            p.find(url)?.groupValues?.getOrNull(1)?.let { return it }
        }
        // Fallback: if it looks like a bare video id
        if (Regex("^[A-Za-z0-9_-]{11}$").matches(url.trim())) return url.trim()
        return null
    }

    suspend fun fetchTranscript(url: String): YoutubeTranscript = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(url)
            ?: return@withContext YoutubeTranscript("", "", "URL에서 video id를 찾지 못했습니다.")
        val cfg = settings.current()
        val api = cfg.ytSummaryApi.trimEnd('/')
        if (api.isBlank() || !api.startsWith("http")) {
            return@withContext YoutubeTranscript(videoId, "", "YT API endpoint가 잘못되었습니다.")
        }
        val payload = buildJsonObject {
            put("video_url", JsonPrimitive("https://www.youtube.com/watch?v=$videoId"))
            put("lang", JsonPrimitive("ko"))
        }
        val body = payload.toString().toRequestBody(JSON_MT)
        val req = Request.Builder()
            .url(api)
            .post(body)
            .header("Content-Type", "application/json")
            .build()
        try {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext YoutubeTranscript(videoId, "", "HTTP ${resp.code}: ${raw.take(300)}")
                }
                val transcript = extractTranscript(raw)
                if (transcript.isNullOrBlank()) {
                    YoutubeTranscript(videoId, "", "응답에서 자막을 찾지 못했습니다.")
                } else {
                    YoutubeTranscript(videoId, transcript)
                }
            }
        } catch (t: Throwable) {
            YoutubeTranscript(videoId, "", t.message ?: t.javaClass.simpleName)
        }
    }

    private fun extractTranscript(raw: String): String? {
        val element = runCatching { json.parseToJsonElement(raw) }.getOrNull() ?: return raw
        val obj = runCatching { element.jsonObject }.getOrNull() ?: return raw
        listOf("transcript", "text", "subtitles", "result", "content").forEach { key ->
            obj[key]?.let { e ->
                runCatching { e.jsonPrimitive.content }.getOrNull()?.let { if (it.isNotBlank()) return it }
            }
        }
        obj["segments"]?.let { e ->
            runCatching {
                e.jsonArray.joinToString(" ") { seg ->
                    seg.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
                }
            }.getOrNull()?.let { if (it.isNotBlank()) return it }
        }
        return raw.take(20000)
    }

    companion object {
        private val JSON_MT = "application/json; charset=utf-8".toMediaType()
    }
}
