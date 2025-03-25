package com.aayush.bluechat.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aayush.bluechat.screens.HomeScreen
import com.aayush.bluechat.screens.SettingsScreen
import com.aayush.bluechat.screens.ChatScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object Chat : Screen("chat/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    darkTheme: Boolean,
    onThemeUpdated: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                navigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                navigateToChat = { deviceAddress ->
                    navController.navigate(Screen.Chat.createRoute(deviceAddress))
                },
                darkTheme = darkTheme,
                onThemeUpdated = onThemeUpdated
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                darkTheme = darkTheme,
                onThemeUpdated = onThemeUpdated
            )
        }
        
        composable(
            route = Screen.Chat.route
        ) { backStackEntry ->
            val deviceAddress = backStackEntry.arguments?.getString("deviceAddress")
            if (deviceAddress != null) {
                ChatScreen(
                    deviceAddress = deviceAddress,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    darkTheme = darkTheme,
                    onThemeUpdated = onThemeUpdated
                )
            }
        }
    }
} 