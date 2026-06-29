/*
 *  Lynket
 *
 *  Copyright (C) 2025 Arunkumar
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package arun.com.chromer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import arun.com.chromer.ui.about.AboutScreen
import arun.com.chromer.ui.article.ArticleScreen
import arun.com.chromer.ui.browser.BrowserScreen
import arun.com.chromer.ui.home.HomeScreen
import arun.com.chromer.ui.history.HistoryScreen
import arun.com.chromer.ui.providerselection.ProviderSelectionScreen
import arun.com.chromer.ui.settings.SettingsScreen
import arun.com.chromer.ui.tabs.TabsScreen

/**
 * Phase 3.1: Navigation setup for Lynket Browser
 *
 * Defines all app screens and navigation routes using Jetpack Navigation Compose.
 * Type-safe navigation with sealed class hierarchy.
 */
sealed class Screen(val route: String) {
    /**
     * Home screen - main launcher
     */
    data object Home : Screen("home")

    /**
     * History screen - browsing history list
     */
    data object History : Screen("history")

    /**
     * Tabs screen - active tabs list
     */
    data object Tabs : Screen("tabs")

    /**
     * Settings root screen
     */
    data object Settings : Screen("settings")

    /**
     * Browser screen with URL parameter
     */
    data object Browser : Screen("browser/{url}") {
        fun createRoute(url: String) = "browser/$url"
    }

    /**
     * Per-app settings with package name parameter
     */
    data object PerAppSettings : Screen("per_app_settings/{package}") {
        fun createRoute(packageName: String) = "per_app_settings/$packageName"
    }

    /**
     * Provider selection screen
     */
    data object ProviderSelection : Screen("provider_selection")

    /**
     * Article mode viewer
     */
    data object Article : Screen("article/{url}") {
        fun createRoute(url: String) = "article/$url"
    }

    /**
     * Web Heads management
     */
    data object WebHeads : Screen("webheads")

    /**
     * About screen
     */
    data object About : Screen("about")
}

/**
 * Main navigation graph for Lynket Browser.
 *
 * @param modifier Modifier for the NavHost
 * @param navController NavHostController for navigation actions
 * @param startDestination Initial destination (defaults to Home)
 */
@Composable
fun ChromerNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        // History Screen
        composable(Screen.History.route) {
            HistoryScreen(navController = navController)
        }

        // Tabs Screen
        composable(Screen.Tabs.route) {
            TabsScreen(navController = navController)
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        // Browser Screen with URL argument
        composable(
            route = Screen.Browser.route,
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            BrowserScreen(url = url, navController = navController)
        }

        // Per-App Settings with package argument
        composable(
            route = Screen.PerAppSettings.route,
            arguments = listOf(
                navArgument("package") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("package") ?: ""
            // PerAppSettingsScreen(packageName = packageName, navController = navController)
            PlaceholderScreen(title = "Per-App: $packageName")
        }

        // Provider Selection Screen
        composable(Screen.ProviderSelection.route) {
            ProviderSelectionScreen(navController = navController)
        }

        // Article Screen with URL argument
        composable(
            route = Screen.Article.route,
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            ArticleScreen(url = url, navController = navController)
        }

        // Web Heads Management
        composable(Screen.WebHeads.route) {
            // WebHeadsScreen(navController = navController)
            PlaceholderScreen(title = "Web Heads")
        }

        // About Screen
        composable(Screen.About.route) {
            AboutScreen(navController = navController)
        }
    }
}

/**
 * Placeholder screen for development.
 * Will be replaced with actual screen implementations.
 */
@Composable
private fun PlaceholderScreen(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
    }
}

/**
 * Navigation extensions for type-safe navigation
 */
fun NavHostController.navigateToHome() {
    navigate(Screen.Home.route) {
        popUpTo(Screen.Home.route) { inclusive = true }
    }
}

fun NavHostController.navigateToHistory() {
    navigate(Screen.History.route)
}

fun NavHostController.navigateToTabs() {
    navigate(Screen.Tabs.route)
}

fun NavHostController.navigateToSettings() {
    navigate(Screen.Settings.route)
}

fun NavHostController.navigateToBrowser(url: String) {
    navigate(Screen.Browser.createRoute(url))
}

fun NavHostController.navigateToPerAppSettings(packageName: String) {
    navigate(Screen.PerAppSettings.createRoute(packageName))
}

fun NavHostController.navigateToProviderSelection() {
    navigate(Screen.ProviderSelection.route)
}

fun NavHostController.navigateToArticle(url: String) {
    navigate(Screen.Article.createRoute(url))
}

fun NavHostController.navigateToWebHeads() {
    navigate(Screen.WebHeads.route)
}

fun NavHostController.navigateToAbout() {
    navigate(Screen.About.route)
}

// Missing import helper
private fun Modifier.fillMaxSize(): Modifier = this.then(
    androidx.compose.foundation.layout.fillMaxSize()
)
