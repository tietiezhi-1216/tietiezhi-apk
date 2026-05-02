package com.tietiezhi.apk.ui.navigation

import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.tietiezhi.apk.ui.chat.ChatScreen
import com.tietiezhi.apk.ui.features.FeaturesScreen
import com.tietiezhi.apk.ui.settings.SettingsScreen
import com.tietiezhi.apk.ui.terminal.TerminalScreen
import com.tietiezhi.apk.ui.workspace.WorkspaceScreen
import com.tietiezhi.apk.ui.workspace.WorkspaceEditorScreen
import kotlinx.coroutines.launch

object Routes {
    const val CHAT = "chat"
    const val FEATURES = "features"
    const val MCP = "mcp"
    const val AGENTS = "agents"
    const val HOOKS = "hooks"
    const val CRON = "cron"
    const val WORKSPACE = "workspace"
    const val WORKSPACE_EDITOR = "workspace/editor/{path}"
    const val TERMINAL = "terminal"
    const val SETTINGS = "settings"
    
    fun workspaceEditor(path: String) = "workspace/editor/${java.net.URLEncoder.encode(path, "UTF-8")}"
}

@Composable
fun NavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    AppDrawer(
        drawerState = drawerState,
        onRouteSelected = { route ->
            if (route != currentRoute) {
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        onClose = {
            scope.launch { drawerState.close() }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.CHAT,
            modifier = modifier
        ) {
            composable(Routes.CHAT) {
                ChatScreen(
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    }
                )
            }
            
            composable(Routes.FEATURES) {
                FeaturesScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Routes.MCP) {
                FeaturesScreen(
                    onBack = { navController.popBackStack() },
                    initialTab = 1
                )
            }
            
            composable(Routes.AGENTS) {
                FeaturesScreen(
                    onBack = { navController.popBackStack() },
                    initialTab = 2
                )
            }
            
            composable(Routes.HOOKS) {
                FeaturesScreen(
                    onBack = { navController.popBackStack() },
                    initialTab = 3
                )
            }
            
            composable(Routes.CRON) {
                FeaturesScreen(
                    onBack = { navController.popBackStack() },
                    initialTab = 4
                )
            }
            
            composable(Routes.WORKSPACE) {
                WorkspaceScreen(
                    onFileClick = { path -> 
                        navController.navigate(Routes.workspaceEditor(path))
                    },
                    onBack = { navController.popBackStack() }
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
                TerminalScreen(onBack = { navController.popBackStack() })
            }
            
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
