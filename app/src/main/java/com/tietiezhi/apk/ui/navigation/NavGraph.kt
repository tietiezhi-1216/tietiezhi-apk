package com.tietiezhi.apk.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.tietiezhi.apk.ui.chat.ChatScreen
import com.tietiezhi.apk.ui.chatlist.ChatListScreen
import com.tietiezhi.apk.ui.features.FeaturesScreen
import com.tietiezhi.apk.ui.settings.SettingsScreen
import com.tietiezhi.apk.ui.terminal.TerminalScreen
import com.tietiezhi.apk.ui.workspace.WorkspaceScreen
import com.tietiezhi.apk.ui.workspace.WorkspaceEditorScreen

object Routes {
    const val CHAT_LIST = "chat_list"
    const val CHAT = "chat/{chatId}"
    const val FEATURES = "features"
    const val WORKSPACE = "workspace"
    const val WORKSPACE_EDITOR = "workspace/editor/{path}"
    const val TERMINAL = "terminal"
    const val SETTINGS = "settings"
    
    fun chat(chatId: String) = "chat/$chatId"
    fun workspaceEditor(path: String) = "workspace/editor/${java.net.URLEncoder.encode(path, "UTF-8")}"
}

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    data object Chat : BottomNavItem(Routes.CHAT_LIST, Icons.Default.Chat, "对话")
    data object Features : BottomNavItem(Routes.FEATURES, Icons.Default.Bolt, "功能")
    data object Workspace : BottomNavItem(Routes.WORKSPACE, Icons.Default.Folder, "工作区")
    data object Terminal : BottomNavItem(Routes.TERMINAL, Icons.Default.Terminal, "终端")
    data object Settings : BottomNavItem(Routes.SETTINGS, Icons.Default.Settings, "设置")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val bottomNavItems = listOf(
        BottomNavItem.Chat,
        BottomNavItem.Features,
        BottomNavItem.Workspace,
        BottomNavItem.Terminal,
        BottomNavItem.Settings
    )
    
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CHAT_LIST,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.CHAT_LIST) {
                ChatListScreen(
                    onChatClick = { navController.navigate(Routes.chat(it)) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) }
                )
            }
            
            composable(
                route = Routes.CHAT,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) {
                ChatScreen(onBack = { navController.popBackStack() })
            }
            
            composable(Routes.FEATURES) {
                FeaturesScreen()
            }
            
            composable(Routes.WORKSPACE) {
                WorkspaceScreen(
                    onFileClick = { path -> 
                        navController.navigate(Routes.workspaceEditor(path))
                    }
                )
            }
            
            composable(
                route = Routes.WORKSPACE_EDITOR,
                arguments = listOf(navArgument("path") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
                val path = java.net.URLDecoder.decode(encodedPath, "UTF-8")
                WorkspaceEditorScreen(
                    filePath = path,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Routes.TERMINAL) {
                TerminalScreen()
            }
            
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
