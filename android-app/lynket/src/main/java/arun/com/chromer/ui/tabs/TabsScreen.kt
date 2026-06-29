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

package arun.com.chromer.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import arun.com.chromer.tabs.TabsManager.Tab
import arun.com.chromer.tabs.ui.ModernTabsViewModel
import arun.com.chromer.tabs.ui.ModernTabsViewModel.TabsUiState
import arun.com.chromer.ui.navigation.navigateToBrowser
import coil.compose.AsyncImage

/**
 * Phase 3: Modern TabsScreen
 *
 * Shows active browser tabs with:
 * - List of open tabs
 * - Close individual tabs
 * - Close all tabs
 * - Refresh tabs
 * - Navigate to tab
 *
 * Demonstrates integration with legacy TabsManager
 * while using modern Compose UI and ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(
    navController: NavController,
    viewModel: ModernTabsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TabsTopBar(
                onBackClick = { navController.popBackStack() },
                onCloseAllClick = viewModel::showCloseAllDialog,
                onRefreshClick = viewModel::refresh,
                tabCount = when (val state = uiState) {
                    is TabsUiState.Success -> state.tabs.size
                    else -> 0
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is TabsUiState.Loading -> {
                    LoadingState()
                }
                is TabsUiState.Success -> {
                    if (state.tabs.isEmpty()) {
                        EmptyTabsState()
                    } else {
                        TabsList(
                            tabs = state.tabs,
                            onTabClick = { tab ->
                                // Navigate to the tab's URL
                                navController.navigateToBrowser(tab.url)
                            }
                        )
                    }

                    // Close all dialog
                    if (state.showCloseAllDialog) {
                        CloseAllDialog(
                            onConfirm = viewModel::closeAllTabs,
                            onDismiss = viewModel::hideCloseAllDialog,
                            tabCount = state.tabs.size
                        )
                    }
                }
                is TabsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = viewModel::loadTabs
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabsTopBar(
    onBackClick: () -> Unit,
    onCloseAllClick: () -> Unit,
    onRefreshClick: () -> Unit,
    tabCount: Int
) {
    TopAppBar(
        title = { Text("Active Tabs ($tabCount)") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            IconButton(onClick = onRefreshClick) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
            IconButton(onClick = onCloseAllClick, enabled = tabCount > 0) {
                Icon(Icons.Default.Close, "Close All")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun TabsList(
    tabs: List<Tab>,
    onTabClick: (Tab) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tabs, key = { it.url }) { tab ->
            TabItem(
                tab = tab,
                onClick = { onTabClick(tab) }
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favicon
            AsyncImage(
                model = tab.website?.faviconUrl,
                contentDescription = "Favicon",
                modifier = Modifier.size(48.dp)
            )

            // Title and URL
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tab.website?.safeLabel() ?: tab.url,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Text(
                    text = tab.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                // Tab type indicator
                Spacer(modifier = Modifier.height(4.dp))
                TabTypeBadge(tab = tab)
            }

            // Arrow indicator
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TabTypeBadge(tab: Tab) {
    val (label, color) = when (tab.type) {
        arun.com.chromer.tabs.CUSTOM_TAB -> "Custom Tab" to MaterialTheme.colorScheme.primary
        arun.com.chromer.tabs.WEB_VIEW -> "WebView" to MaterialTheme.colorScheme.secondary
        arun.com.chromer.tabs.ARTICLE -> "Article" to MaterialTheme.colorScheme.tertiary
        else -> "Other" to MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CloseAllDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    tabCount: Int
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Close All Tabs?") },
        text = {
            Text("This will close all $tabCount active tab${if (tabCount != 1) "s" else ""}.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Close All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmptyTabsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Tab,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No active tabs",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Open a link to create a tab",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Error loading tabs",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
