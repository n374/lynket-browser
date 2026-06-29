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

package arun.com.chromer.data.history

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import arun.com.chromer.data.database.dao.WebsiteDao
import arun.com.chromer.data.database.dao.upsertVisit
import arun.com.chromer.data.database.entity.WebsiteEntity
import arun.com.chromer.data.database.entity.toEntity
import arun.com.chromer.data.database.entity.toWebsite
import arun.com.chromer.data.preferences.UserPreferencesRepository
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.di.hilt.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 1.5: Modern history repository using Kotlin Flows and Room
 *
 * Replaces DefaultHistoryRepository (RxJava + SQLite) with modern implementation:
 * - Kotlin Coroutines for async operations
 * - Flow for reactive data streams
 * - Room for type-safe database access
 * - PagingData for efficient pagination
 *
 * This repository will gradually replace the legacy HistoryRepository as
 * ViewModels are migrated to use Hilt and Flows.
 */
@Singleton
class ModernHistoryRepository @Inject constructor(
    private val websiteDao: WebsiteDao,
    private val preferencesRepository: UserPreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * Get all history as a Flow.
     * Emits updates whenever history changes.
     */
    fun getAllHistory(): Flow<List<Website>> {
        return websiteDao.getAllFlow()
            .map { entities -> entities.map { it.toWebsite() } }
            .flowOn(ioDispatcher)
    }

    /**
     * Get recent history (last 8 items) as a Flow.
     * Updates in real-time as history changes.
     */
    fun getRecents(): Flow<List<Website>> {
        return websiteDao.getRecentsFlow()
            .map { entities -> entities.map { it.toWebsite() } }
            .onEach { websites ->
                Timber.d("Recent history updated: ${websites.size} items")
            }
            .flowOn(ioDispatcher)
    }

    /**
     * Get paginated history for RecyclerView.
     * Efficiently handles large datasets with Paging 3.
     */
    fun getPagedHistory(): Flow<PagingData<Website>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { websiteDao.getAllPaged() }
        ).flow
            .map { pagingData -> pagingData.map { it.toWebsite() } }
            .flowOn(ioDispatcher)
    }

    /**
     * Get a website by URL.
     * Returns null if not found.
     */
    suspend fun getByUrl(url: String): Website? = withContext(ioDispatcher) {
        websiteDao.getByUrl(url)?.toWebsite()
    }

    /**
     * Get a website by URL as a Flow.
     * Emits updates whenever this website changes.
     */
    fun getByUrlFlow(url: String): Flow<Website?> {
        return websiteDao.getByUrlFlow(url)
            .map { it?.toWebsite() }
            .flowOn(ioDispatcher)
    }

    /**
     * Search history by URL or title.
     * Returns up to 5 results.
     */
    suspend fun search(query: String): List<Website> = withContext(ioDispatcher) {
        websiteDao.search(query).map { it.toWebsite() }
    }

    /**
     * Search history as a Flow.
     */
    fun searchFlow(query: String): Flow<List<Website>> {
        return websiteDao.searchFlow(query)
            .map { entities -> entities.map { it.toWebsite() } }
            .flowOn(ioDispatcher)
    }

    /**
     * Get all bookmarked websites.
     */
    fun getBookmarks(): Flow<List<Website>> {
        return websiteDao.getBookmarksFlow()
            .map { entities -> entities.map { it.toWebsite() } }
            .flowOn(ioDispatcher)
    }

    /**
     * Insert or update a website visit.
     *
     * Behavior:
     * - If incognito mode is enabled, returns immediately without saving
     * - If URL exists, increments visit count and updates timestamp
     * - If URL is new, inserts as new entry
     *
     * @param website The website to record
     * @return The updated or inserted website
     */
    suspend fun recordVisit(website: Website): Website? = withContext(ioDispatcher) {
        // Check if incognito mode is enabled
        val prefs = preferencesRepository.userPreferencesFlow.first()
        if (prefs.incognitoMode) {
            Timber.d("Incognito mode enabled, not recording visit: ${website.url}")
            return@withContext website
        }

        try {
            // Use upsert helper to insert or update
            websiteDao.upsertVisit(website.url) {
                website.toEntity()
            }

            // Fetch the updated website
            val updated = websiteDao.getByUrl(website.url)?.toWebsite()

            if (updated != null) {
                Timber.d("Recorded visit for: ${website.url} (count: ${updated.count})")
            } else {
                Timber.e("Failed to record visit for: ${website.url}")
            }

            updated
        } catch (e: Exception) {
            Timber.e(e, "Error recording visit for: ${website.url}")
            null
        }
    }

    /**
     * Insert a website (used when migrating from legacy code).
     * Prefer recordVisit() for new code.
     */
    suspend fun insert(website: Website): Website? = recordVisit(website)

    /**
     * Update a website.
     */
    suspend fun update(website: Website): Website? = withContext(ioDispatcher) {
        try {
            val entity = website.toEntity()
            websiteDao.update(entity)
            Timber.d("Updated website: ${website.url}")
            website
        } catch (e: Exception) {
            Timber.e(e, "Error updating website: ${website.url}")
            null
        }
    }

    /**
     * Delete a website from history.
     */
    suspend fun delete(website: Website): Boolean = withContext(ioDispatcher) {
        try {
            websiteDao.deleteByUrl(website.url)
            Timber.d("Deleted website: ${website.url}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error deleting website: ${website.url}")
            false
        }
    }

    /**
     * Delete all history (nuclear option).
     */
    suspend fun deleteAll(): Int = withContext(ioDispatcher) {
        try {
            val count = websiteDao.getCount()
            websiteDao.deleteAll()
            Timber.d("Deleted all history: $count items")
            count
        } catch (e: Exception) {
            Timber.e(e, "Error deleting all history")
            0
        }
    }

    /**
     * Check if a URL exists in history.
     */
    suspend fun exists(url: String): Boolean = withContext(ioDispatcher) {
        websiteDao.exists(url)
    }

    /**
     * Check if a website exists in history.
     */
    suspend fun exists(website: Website): Boolean = exists(website.url)

    /**
     * Toggle bookmark status for a website.
     */
    suspend fun toggleBookmark(url: String) = withContext(ioDispatcher) {
        try {
            websiteDao.toggleBookmark(url)
            Timber.d("Toggled bookmark for: $url")
        } catch (e: Exception) {
            Timber.e(e, "Error toggling bookmark for: $url")
        }
    }

    /**
     * Set bookmark status for a website.
     */
    suspend fun setBookmarked(url: String, bookmarked: Boolean) = withContext(ioDispatcher) {
        try {
            websiteDao.setBookmarked(url, bookmarked)
            Timber.d("Set bookmark for: $url to $bookmarked")
        } catch (e: Exception) {
            Timber.e(e, "Error setting bookmark for: $url")
        }
    }

    /**
     * Delete history older than specified days.
     * Useful for cleanup jobs.
     */
    suspend fun deleteOlderThan(days: Int): Int = withContext(ioDispatcher) {
        try {
            val timestamp = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
            val countBefore = websiteDao.getCount()
            websiteDao.deleteOlderThan(timestamp)
            val countAfter = websiteDao.getCount()
            val deleted = countBefore - countAfter
            Timber.d("Deleted $deleted items older than $days days")
            deleted
        } catch (e: Exception) {
            Timber.e(e, "Error deleting old history")
            0
        }
    }

    /**
     * Get total history count.
     */
    suspend fun getCount(): Int = withContext(ioDispatcher) {
        websiteDao.getCount()
    }

    /**
     * Get total bookmark count.
     */
    suspend fun getBookmarkCount(): Int = withContext(ioDispatcher) {
        websiteDao.getBookmarkCount()
    }
}
