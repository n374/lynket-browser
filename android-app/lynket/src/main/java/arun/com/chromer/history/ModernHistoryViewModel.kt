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

package arun.com.chromer.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import arun.com.chromer.data.history.ModernHistoryRepository
import arun.com.chromer.data.preferences.UserPreferencesRepository
import arun.com.chromer.data.website.model.Website
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Phase 3: Modern HistoryViewModel with Paging 3
 *
 * Demonstrates advanced patterns:
 * - Paging 3 with Compose integration
 * - Search with debounce
 * - Multiple UI state streams
 * - Bulk operations (delete all)
 *
 * Features:
 * - Infinite scroll pagination
 * - Real-time search filtering
 * - Delete individual items
 * - Clear all history
 * - Export history (future)
 */
@HiltViewModel
class ModernHistoryViewModel @Inject constructor(
    private val historyRepository: ModernHistoryRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /**
     * UI State for history screen
     */
    data class HistoryUiState(
        val searchQuery: String = "",
        val isSearching: Boolean = false,
        val totalCount: Int = 0,
        val bookmarkCount: Int = 0,
        val showClearAllDialog: Boolean = false
    )

    /**
     * Search query flow
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * UI state flow
     */
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    /**
     * Paginated history flow.
     * Automatically handles loading more items as user scrolls.
     * Cached in viewModelScope to survive configuration changes.
     */
    val pagedHistory: Flow<PagingData<Website>> = historyRepository
        .getPagedHistory()
        .cachedIn(viewModelScope)

    /**
     * Search results flow.
     * Debounces search input to avoid excessive queries.
     * Only activates when search query is not empty.
     */
    val searchResults: Flow<List<Website>> = _searchQuery
        .debounce(300) // Wait 300ms after user stops typing
        .filter { it.isNotBlank() }
        .distinctUntilChanged()
        .onEach { _uiState.update { state -> state.copy(isSearching = true) } }
        .flatMapLatest { query ->
            historyRepository.searchFlow(query)
        }
        .onEach { _uiState.update { state -> state.copy(isSearching = false) } }
        .catch { exception ->
            Timber.e(exception, "Error searching history")
            emit(emptyList())
            _uiState.update { state -> state.copy(isSearching = false) }
        }

    /**
     * Bookmarks flow for quick access
     */
    val bookmarks: StateFlow<List<Website>> = historyRepository.getBookmarks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadCounts()
    }

    /**
     * Load total counts for display
     */
    private fun loadCounts() {
        viewModelScope.launch {
            try {
                val total = historyRepository.getCount()
                val bookmarkCount = historyRepository.getBookmarkCount()
                _uiState.update { state ->
                    state.copy(
                        totalCount = total,
                        bookmarkCount = bookmarkCount
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading counts")
            }
        }
    }

    // ========== Actions ==========

    /**
     * Update search query
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Clear search
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.update { it.copy(searchQuery = "") }
    }

    /**
     * Delete a single website from history
     */
    fun deleteWebsite(website: Website) {
        viewModelScope.launch {
            try {
                val success = historyRepository.delete(website)
                if (success) {
                    Timber.d("Deleted: ${website.url}")
                    loadCounts() // Refresh counts
                } else {
                    Timber.e("Failed to delete: ${website.url}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting website")
            }
        }
    }

    /**
     * Toggle bookmark status
     */
    fun toggleBookmark(website: Website) {
        viewModelScope.launch {
            try {
                historyRepository.toggleBookmark(website.url)
                Timber.d("Toggled bookmark: ${website.url}")
                loadCounts()
            } catch (e: Exception) {
                Timber.e(e, "Error toggling bookmark")
            }
        }
    }

    /**
     * Show clear all confirmation dialog
     */
    fun showClearAllDialog() {
        _uiState.update { it.copy(showClearAllDialog = true) }
    }

    /**
     * Hide clear all dialog
     */
    fun hideClearAllDialog() {
        _uiState.update { it.copy(showClearAllDialog = false) }
    }

    /**
     * Clear all history (after confirmation)
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                val count = historyRepository.deleteAll()
                Timber.d("Cleared all history: $count items")
                _uiState.update { state ->
                    state.copy(
                        showClearAllDialog = false,
                        totalCount = 0,
                        bookmarkCount = 0
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error clearing history")
            }
        }
    }

    /**
     * Delete old history (older than X days)
     */
    fun deleteOldHistory(days: Int = 30) {
        viewModelScope.launch {
            try {
                val deleted = historyRepository.deleteOlderThan(days)
                Timber.d("Deleted $deleted items older than $days days")
                loadCounts()
            } catch (e: Exception) {
                Timber.e(e, "Error deleting old history")
            }
        }
    }

    /**
     * Refresh history (force reload)
     */
    fun refresh() {
        loadCounts()
        // Paging data automatically refreshes when database changes
    }
}
