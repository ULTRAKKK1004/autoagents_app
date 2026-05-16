package com.autoagents.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.autoagents.app.ui.chat.ChatScreen
import com.autoagents.app.ui.home.ArticleDetailScreen
import com.autoagents.app.ui.home.HomeScreen
import com.autoagents.app.ui.insights.InsightsScreen
import com.autoagents.app.ui.memo.MemoEditScreen
import com.autoagents.app.ui.memo.MemoListScreen
import com.autoagents.app.ui.settings.SettingsScreen
import com.autoagents.app.ui.youtube.YoutubeScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    initialYoutubeUrl: String? = null
) {
    NavHost(
        navController = navController,
        startDestination = TopDestination.Home.route,
        modifier = modifier
    ) {
        composable(TopDestination.Home.route) {
            HomeScreen(onOpenArticle = { id ->
                navController.navigate("article/$id")
            })
        }
        composable(
            route = "article/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("articleId") ?: 0L
            ArticleDetailScreen(articleId = id, onBack = { navController.popBackStack() })
        }
        composable(TopDestination.Chat.route) {
            ChatScreen()
        }
        composable(TopDestination.YouTube.route) {
            YoutubeScreen(initialUrl = initialYoutubeUrl)
        }
        composable(TopDestination.Memo.route) {
            MemoListScreen(onEdit = { id ->
                navController.navigate("memo/edit?id=$id")
            }, onNew = {
                navController.navigate("memo/edit?id=0")
            })
        }
        composable(
            route = "memo/edit?id={id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            MemoEditScreen(memoId = id, onDone = { navController.popBackStack() })
        }
        composable(TopDestination.Insights.route) {
            InsightsScreen()
        }
        composable(TopDestination.Settings.route) {
            SettingsScreen()
        }
    }
}
