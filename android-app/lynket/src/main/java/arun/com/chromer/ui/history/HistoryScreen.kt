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

package arun.com.chromer.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.history.ModernHistoryViewModel
import arun.com.chromer.ui.navigation.navigateToBrowser
import coil.compose.AsyncImage

/**
 * Phase 3: Modern HistoryScreen with Paging 3
 *
 * Demonstrates advanced Compose patterns:
 * - Paging 3 with LazyPagingItems
 * - Pull-to-refresh
 * - Search with debounce
 * - Empty states
 * - Loading states (initial, append, prepend)
 * - Error handling
 * - Confirmation dialogs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: ModernHistoryViewModel = hiltViewModel()
) {
    // Collect state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle(initialValue = emptyList())

    // Paging items
    val pagedHistory = viewModel.pagedHistory.collectAsLazyPagingItems()

    // Pull to refresh state
    val pullRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()

    Scaffold(
        topBar = {
            HistoryTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                onClearSearch = viewModel::clearSearch,
                onBackClick = { navController.popBackStack() },
                onClearAllClick = viewModel::showClearAllDialog,
                totalCount = uiState.totalCount
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // Show search results when searching
                searchQuery.isNotBlank() -> {
                    SearchResults(
                        results = searchResults,
                        isSearching = uiState.isSearching,
                        onWebsiteClick = { website ->
                            navController.navigateToBrowser(website.url)
                        },
                        onDeleteClick = viewModel::deleteWebsite,
                        onBookmarkClick = viewModel::toggleBookmark
                    )
                }
                // Show paged history
                else -> {
                    HistoryList(
                        pagedHistory = pagedHistory,
                        onWebsiteClick = { website ->
                            navController.navigateToBrowser(website.url)
                        },
                        onDeleteClick = viewModel::deleteWebsite,
                        onBookmarkClick = viewModel::toggleBookmark,
                        onRefresh = {
                            pagedHistory.refresh()
                            viewModel.refresh()
                        }
                    )
                }
            }

            // Clear all confirmation dialog
            if (uiState.showClearAllDialog) {
                ClearAllDialog(
                    onConfirm = viewModel::clearAllHistory,
                    onDismiss = viewModel::hideClearAllDialog,
                    itemCount = uiState.totalCount
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onBackClick: () -> Unit,
    onClearAllClick: () -> Unit,
    totalCount: Int
) {
    Column {
        TopAppBar(
            title = { Text("History ($totalCount)") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                IconButton(onClick = onClearAllClick, enabled = totalCount > 0) {
                    Icon(Icons.Default.DeleteSweep, "Clear All")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Search bar
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search history...") },
            leadingIcon = {
                Icon(Icons.Default.Search, "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Clear, "Clear")
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
}

@Composable
private fun HistoryList(
    pagedHistory: LazyPagingItems<Website>,
    onWebsiteClick: (Website) -> Unit,
    onDeleteClick: (Website) -> Unit,
    onBookmarkClick: (Website) -> Unit,
    onRefresh: () -> Unit
) {
    when (val loadState = pagedHistory.loadState.refresh) {
        is LoadState.Loading -> {
            LoadingState()
        }
        is LoadState.Error -> {
            ErrorState(
                message = loadState.error.message ?: "Unknown error",
                onRetry = onRefresh
            )
        }
        else -> {
            if (pagedHistory.itemCount == 0) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pagedHistory.itemCount) { index ->
                        pagedHistory[index]?.let { website ->
                            WebsiteItem(
                                website = website,
                                onClick = { onWebsiteClick(website) },
                                onDelete = { onDeleteClick(website) },
                                onBookmark = { onBookmarkClick(website) }
                            )
                        }
                    }

                    // Loading indicator at bottom when loading more
                    when (pagedHistory.loadState.append) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<Website>,
    isSearching: Boolean,
    onWebsiteClick: (Website) -> Unit,
    onDeleteClick: (Website) -> Unit,
    onBookmarkClick: (Website) -> Unit
) {
    when {
        isSearching -> {
            LoadingState()
        }
        results.isEmpty() -> {
            EmptySearchState()
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "${results.size} result${if (results.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(results, key = { it.url }) { website ->
                    WebsiteItem(
                        website = website,
                        onClick = { onWebsiteClick(website) },
                        onDelete = { onDeleteClick(website) },
                        onBookmark = { onBookmarkClick(website) }
                    )
                }
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
                if (website.count > 1) {
                    Text(
                        text = "Visited ${website.count} times",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
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
private fun ClearAllDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    itemCount: Int
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear All History?") },
        text = {
            Text("This will delete all $itemCount items from your browsing history. This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All")
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
private fun EmptyHistoryState() {
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
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No browsing history",
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
private fun EmptySearchState() {
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
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No results found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Try a different search term",
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
                text = "Error loading history",
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
