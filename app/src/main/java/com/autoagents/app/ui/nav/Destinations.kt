package com.autoagents.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import com.autoagents.app.R

enum class TopDestination(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int
) {
    Home("home", Icons.Filled.Article, R.string.tab_home),
    Chat("chat", Icons.Filled.Chat, R.string.tab_chat),
    YouTube("youtube", Icons.Filled.PlayCircle, R.string.tab_youtube),
    Memo("memo", Icons.Filled.Notes, R.string.tab_memo),
    Insights("insights", Icons.Filled.Lightbulb, R.string.tab_insights),
    Settings("settings", Icons.Filled.Settings, R.string.tab_settings);

    companion object {
        val Tabs = listOf(Home, Chat, YouTube, Memo, Insights, Settings)
    }
}
