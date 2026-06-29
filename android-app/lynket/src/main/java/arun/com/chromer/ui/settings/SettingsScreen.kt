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

package arun.com.chromer.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import arun.com.chromer.settings.ModernSettingsViewModel

/**
 * Phase 3: Modern SettingsScreen
 *
 * Demonstrates DataStore + Compose preferences:
 * - Real-time preference updates
 * - Material3 components
 * - Organized into categories
 * - Instant UI reactivity (no manual refresh needed!)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: ModernSettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Web Heads Section
            item {
                PreferenceCategory("Web Heads")
            }
            item {
                SwitchPreference(
                    title = "Enable Web Heads",
                    summary = "Show floating bubble heads for links",
                    checked = preferences.webHeadsEnabled,
                    onCheckedChange = viewModel::setWebHeadsEnabled
                )
            }
            item {
                SwitchPreference(
                    title = "Show Favicons",
                    summary = "Display website icons in web heads",
                    checked = preferences.webHeadsFavicons,
                    onCheckedChange = viewModel::setWebHeadsFavicons,
                    enabled = preferences.webHeadsEnabled
                )
            }
            item {
                SwitchPreference(
                    title = "Close on Open",
                    summary = "Close web head when opening link",
                    checked = preferences.webHeadsCloseOnOpen,
                    onCheckedChange = viewModel::setWebHeadsCloseOnOpen,
                    enabled = preferences.webHeadsEnabled
                )
            }

            // Browser Features Section
            item {
                PreferenceCategory("Browser Features")
            }
            item {
                SwitchPreference(
                    title = "Incognito Mode",
                    summary = "Browse privately without saving history",
                    checked = preferences.incognitoMode,
                    onCheckedChange = viewModel::setIncognitoMode
                )
            }
            item {
                SwitchPreference(
                    title = "AMP Mode",
                    summary = "Load AMP versions of pages when available",
                    checked = preferences.ampMode,
                    onCheckedChange = viewModel::setAmpMode
                )
            }
            item {
                SwitchPreference(
                    title = "Article Mode",
                    summary = "Extract and display article content",
                    checked = preferences.articleMode,
                    onCheckedChange = viewModel::setArticleMode
                )
            }
            item {
                SwitchPreference(
                    title = "Use WebView",
                    summary = "Use system WebView instead of custom tabs",
                    checked = preferences.useWebView,
                    onCheckedChange = viewModel::setUseWebView
                )
            }

            // Appearance Section
            item {
                PreferenceCategory("Appearance")
            }
            item {
                SwitchPreference(
                    title = "Dynamic Toolbar",
                    summary = "Match toolbar color to website theme",
                    checked = preferences.dynamicToolbar,
                    onCheckedChange = viewModel::setDynamicToolbar
                )
            }
            item {
                SwitchPreference(
                    title = "Bottom Bar",
                    summary = "Show action bar at bottom",
                    checked = preferences.bottomBarEnabled,
                    onCheckedChange = viewModel::setBottomBar
                )
            }

            // Performance Section
            item {
                PreferenceCategory("Performance")
            }
            item {
                SwitchPreference(
                    title = "Warm Up",
                    summary = "Pre-initialize browser for faster loading",
                    checked = preferences.warmUp,
                    onCheckedChange = viewModel::setWarmUp
                )
            }
            item {
                SwitchPreference(
                    title = "Pre-fetch Links",
                    summary = "Load pages in background",
                    checked = preferences.preFetch,
                    onCheckedChange = viewModel::setPreFetch
                )
            }
            item {
                SwitchPreference(
                    title = "Aggressive Loading",
                    summary = "Use more resources for faster loading",
                    checked = preferences.aggressiveLoading,
                    onCheckedChange = viewModel::setAggressiveLoading
                )
            }

            // Advanced Section
            item {
                PreferenceCategory("Advanced")
            }
            item {
                SwitchPreference(
                    title = "Per-App Settings",
                    summary = "Configure behavior for specific apps",
                    checked = preferences.perAppSettings,
                    onCheckedChange = viewModel::setPerAppSettings
                )
            }
            item {
                SwitchPreference(
                    title = "Merge Tabs and Apps",
                    summary = "Combine tabs with recent apps",
                    checked = preferences.mergeTabsAndApps,
                    onCheckedChange = viewModel::setMergeTabs
                )
            }

            // About Section
            item {
                PreferenceCategory("About")
            }
            item {
                Preference(
                    title = "Version",
                    summary = "2.1.3 (Modern)",
                    onClick = { /* Show about */ }
                )
            }
        }
    }
}

@Composable
private fun PreferenceCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SwitchPreference(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun Preference(
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
