package com.autoagents.app

import android.app.Application
import com.autoagents.app.data.db.AppDatabase
import com.autoagents.app.data.llm.LlmClient
import com.autoagents.app.data.prefs.SettingsRepository
import com.autoagents.app.data.rss.RssClient
import com.autoagents.app.data.youtube.YoutubeClient

class App : Application() {

    val settings: SettingsRepository by lazy { SettingsRepository(applicationContext) }
    val database: AppDatabase by lazy { AppDatabase.build(applicationContext) }
    val llmClient: LlmClient by lazy { LlmClient(settings) }
    val rssClient: RssClient by lazy { RssClient() }
    val youtubeClient: YoutubeClient by lazy { YoutubeClient(settings) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        private var instance: App? = null
        fun get(): App = instance ?: error("App not initialized")
    }
}
