/*
 *  Lynket
 *
 *  Copyright (C) 2025 Arunkumar
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  (at your option) any later version.
 */

package arun.com.chromer.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.home.ModernHomeViewModel
import arun.com.chromer.home.ModernHomeViewModel.HomeUiState
import arun.com.chromer.ui.navigation.navigateToBrowser
import arun.com.chromer.ui.navigation.navigateToHistory
import arun.com.chromer.ui.navigation.navigateToSettings
import coil.compose.AsyncImage

/**
 * Phase 3.2: Modern HomeScreen using Jetpack Compose
 *
 * Demonstrates the complete modern stack:
 * - Hilt ViewModel injection (@HiltViewModel)
 * - StateFlow collection (collectAsStateWithLifecycle)
 * - Material3 components
 * - Navigation Compose
 * - Coil for image loading
 *
 * This screen shows:
 * - Search bar for URL/search input
 * - Browser provider info
 * - Recent browsing history
 * - Quick actions (history, settings)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: ModernHomeViewModel = hiltViewModel()
) {
    // Collect UI state with lifecycle awareness
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize default provider on first launch
    LaunchedEffect(Unit) {
        viewModel.initializeDefaultProvider()
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                onHistoryClick = { navController.navigateToHistory() },
                onSettingsClick = { navController.navigateToSettings() }
            )
        },
        floatingActionButton = {
            // Could add Web Heads toggle FAB here
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    LoadingState()
                }
                is HomeUiState.Success -> {
                    HomeContent(
                        recentWebsites = state.recentWebsites,
                        providerInfo = state.providerInfo,
                        onWebsiteClick = { website ->
                            navController.navigateToBrowser(website.url)
                        },
                        onWebsiteDelete = viewModel::deleteWebsite,
                        onWebsiteBookmark = viewModel::toggleBookmark,
                        onSearchSubmit = { query ->
                            navController.navigateToBrowser(query)
                        }
                    )
                }
                is HomeUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.refreshRecents() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Lynket") },
        actions = {
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Default.History, contentDescription = "History")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    recentWebsites: List<Website>,
    providerInfo: arun.com.chromer.home.epoxycontroller.model.CustomTabProviderInfo,
    onWebsiteClick: (Website) -> Unit,
    onWebsiteDelete: (Website) -> Unit,
    onWebsiteBookmark: (Website) -> Unit,
    onSearchSubmit: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search bar
        item {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { onSearchSubmit(searchQuery) }
            )
        }

        // Provider info card
        item {
            ProviderInfoCard(providerInfo = providerInfo)
        }

        // Recent history section
        if (recentWebsites.isNotEmpty()) {
            item {
                Text(
                    text = "Recent History",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(recentWebsites, key = { it.url }) { website ->
                WebsiteItem(
                    website = website,
                    onClick = { onWebsiteClick(website) },
                    onDelete = { onWebsiteDelete(website) },
                    onBookmark = { onWebsiteBookmark(website) }
                )
            }
        } else {
            item {
                EmptyHistoryState()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Enter URL or search...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.large
    )
}

@Composable
private fun ProviderInfoCard(
    providerInfo: arun.com.chromer.home.epoxycontroller.model.CustomTabProviderInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Provider icon using Coil
            AsyncImage(
                model = providerInfo.iconUri,
                contentDescription = "Browser icon",
                modifier = Modifier.size(48.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Browser Provider",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                // Note: providerDescription is a StringResource, would need proper resolution
                Text(
                    text = "Custom Tab Browser", // Simplified for now
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun WebsiteItem(
    website: Website,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onBookmark: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favicon
            AsyncImage(
                model = website.faviconUrl,
                contentDescription = "Favicon",
                modifier = Modifier.size(40.dp)
            )

            // Title and URL
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = website.safeLabel(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = website.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Actions
            IconButton(onClick = onBookmark) {
                Icon(
                    imageVector = if (website.bookmarked)
                        Icons.Default.Bookmark
                    else
                        Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (website.bookmarked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No recent history",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Your browsing history will appear here",
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
                text = "Error loading data",
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
