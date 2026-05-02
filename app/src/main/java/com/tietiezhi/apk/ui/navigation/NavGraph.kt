package com.tietiezhi.apk.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tietiezhi.apk.ui.chat.ChatScreen
import com.tietiezhi.apk.ui.chatlist.ChatListScreen
import com.tietiezhi.apk.ui.settings.SettingsScreen

object Routes {
    const val CHAT_LIST = "chat_list"
    const val CHAT = "chat/{chatId}"
    const val SETTINGS = "settings"
    fun chat(chatId: String) = "chat/$chatId"
}

@Composable
fun NavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.CHAT_LIST) {
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onChatClick = { nav.navigate(Routes.chat(it)) },
                onSettingsClick = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) {
            ChatScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
