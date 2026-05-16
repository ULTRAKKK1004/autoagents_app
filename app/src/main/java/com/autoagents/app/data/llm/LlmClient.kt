package com.autoagents.app.data.llm

import com.autoagents.app.data.prefs.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class LlmResponse(
    val content: String?,
    val errorMessage: String? = null
) {
    val ok: Boolean get() = content != null
}

class LlmClient(private val settings: SettingsRepository) {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun chat(
        messages: List<ChatMessage>,
        modelOverride: String? = null,
        temperature: Double = 0.7,
        maxTokens: Int = 2048
    ): LlmResponse = withContext(Dispatchers.IO) {
        val cfg = settings.current()
        val endpoint = cfg.endpoint.trimEnd('/')
        val model = modelOverride?.takeIf { it.isNotBlank() } ?: cfg.model
        if (endpoint.isBlank() || !endpoint.startsWith("http")) {
            return@withContext LlmResponse(null, "Invalid endpoint: $endpoint")
        }
        val url = "$endpoint/chat/completions"

        val payload = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("messages", buildJsonArray {
                messages.forEach { m ->
                    add(buildJsonObject {
                        put("role", JsonPrimitive(m.role))
                        put("content", JsonPrimitive(m.content))
                    })
                }
            })
            put("temperature", JsonPrimitive(temperature))
            put("max_tokens", JsonPrimitive(maxTokens))
            put("stream", JsonPrimitive(false))
        }
        val body = payload.toString().toRequestBody(JSON_MT)
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${cfg.token}")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        try {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext LlmResponse(null, "HTTP ${resp.code}: ${raw.take(400)}")
                }
                val parsed = runCatching {
                    val root = json.parseToJsonElement(raw).jsonObject
                    val choices = root["choices"]?.jsonArray ?: return@runCatching null
                    val first = choices.firstOrNull()?.jsonObject ?: return@runCatching null
                    val message = first["message"]?.jsonObject
                    val text = message?.get("content")?.jsonPrimitive?.contentOrNull
                    text
                }.getOrNull()
                if (parsed.isNullOrBlank()) {
                    return@withContext LlmResponse(null, "Empty content in response")
                }
                LlmResponse(parsed)
            }
        } catch (t: Throwable) {
            LlmResponse(null, t.message ?: t.javaClass.simpleName)
        }
    }

    suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        val cfg = settings.current()
        val endpoint = cfg.endpoint.trimEnd('/')
        if (endpoint.isBlank()) return@withContext emptyList()
        val req = Request.Builder()
            .url("$endpoint/models")
            .header("Authorization", "Bearer ${cfg.token}")
            .get()
            .build()
        try {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val raw = resp.body?.string().orEmpty()
                val root = json.parseToJsonElement(raw).jsonObject
                val data = root["data"]?.jsonArray ?: return@withContext emptyList()
                data.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun pingConnection(): String = withContext(Dispatchers.IO) {
        val msgs = listOf(ChatMessage("user", "Reply only with 'OK'."))
        val r = chat(msgs, temperature = 0.0, maxTokens = 16)
        if (r.ok) "OK (${r.content?.take(40)?.trim()})" else "FAIL: ${r.errorMessage}"
    }

    companion object {
        private val JSON_MT = "application/json; charset=utf-8".toMediaType()
    }
}

private val JsonPrimitive.contentOrNull: String?
    get() = try { content } catch (_: Throwable) { null }
