package com.autoagents.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoagents.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("autoagents_settings")

data class LlmSettings(
    val endpoint: String,
    val token: String,
    val model: String,
    val ytModel: String,
    val ytSummaryApi: String,
    val keywords: String,
    val language: String
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ENDPOINT = stringPreferencesKey("llm_endpoint")
        val TOKEN = stringPreferencesKey("llm_token")
        val MODEL = stringPreferencesKey("llm_model")
        val YT_MODEL = stringPreferencesKey("yt_llm_model")
        val YT_API = stringPreferencesKey("yt_summary_api")
        val KEYWORDS = stringPreferencesKey("interest_keywords")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settings: Flow<LlmSettings> = context.dataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun current(): LlmSettings = context.dataStore.data.first().toSettings()

    suspend fun update(transform: (LlmSettings) -> LlmSettings) {
        context.dataStore.edit { prefs ->
            val cur = prefs.toSettings()
            val next = transform(cur)
            prefs[Keys.ENDPOINT] = next.endpoint
            prefs[Keys.TOKEN] = next.token
            prefs[Keys.MODEL] = next.model
            prefs[Keys.YT_MODEL] = next.ytModel
            prefs[Keys.YT_API] = next.ytSummaryApi
            prefs[Keys.KEYWORDS] = next.keywords
            prefs[Keys.LANGUAGE] = next.language
        }
    }

    suspend fun resetDefaults() {
        update { defaultSettings() }
    }

    private fun Preferences.toSettings(): LlmSettings {
        val d = defaultSettings()
        return LlmSettings(
            endpoint = this[Keys.ENDPOINT]?.takeIf { it.isNotBlank() } ?: d.endpoint,
            token = this[Keys.TOKEN]?.takeIf { it.isNotBlank() } ?: d.token,
            model = this[Keys.MODEL]?.takeIf { it.isNotBlank() } ?: d.model,
            ytModel = this[Keys.YT_MODEL]?.takeIf { it.isNotBlank() } ?: d.ytModel,
            ytSummaryApi = this[Keys.YT_API]?.takeIf { it.isNotBlank() } ?: d.ytSummaryApi,
            keywords = this[Keys.KEYWORDS]?.takeIf { it.isNotBlank() } ?: d.keywords,
            language = this[Keys.LANGUAGE]?.takeIf { it.isNotBlank() } ?: d.language
        )
    }

    companion object {
        fun defaultSettings() = LlmSettings(
            endpoint = BuildConfig.DEFAULT_LLM_ENDPOINT,
            token = BuildConfig.DEFAULT_LLM_TOKEN,
            model = BuildConfig.DEFAULT_LLM_MODEL,
            ytModel = BuildConfig.DEFAULT_YT_LLM_MODEL,
            ytSummaryApi = BuildConfig.DEFAULT_YT_SUMMARY_API,
            keywords = "AI, Machine Learning, Mobile, Robotics, deep learning, Gemini, chatgpt, Claude, deepseek",
            language = "ko"
        )
    }
}
