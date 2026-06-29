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

package arun.com.chromer.data.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import arun.com.chromer.data.database.entity.WebsiteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Phase 1.3: Room DAO for Website/History operations
 *
 * Replaces legacy HistorySqlDiskStore with modern Room DAO using:
 * - Kotlin Coroutines (suspend functions) for async operations
 * - Flow for reactive data streams
 * - PagingSource for efficient list pagination
 *
 * All database operations are now type-safe, null-safe, and properly async.
 */
@Dao
interface WebsiteDao {

    /**
     * Get all history items as a Flow, sorted by most recent first.
     * Flow emits updates whenever the database changes.
     */
    @Query("SELECT * FROM history ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<WebsiteEntity>>

    /**
     * Get recent history items (limit 8) as a Flow.
     * Emits updates in real-time as history changes.
     */
    @Query("SELECT * FROM history ORDER BY created_at DESC LIMIT 8")
    fun getRecentsFlow(): Flow<List<WebsiteEntity>>

    /**
     * Get a paginated list of history items for RecyclerView.
     * Efficiently handles large datasets with automatic loading.
     */
    @Query("SELECT * FROM history ORDER BY created_at DESC")
    fun getAllPaged(): PagingSource<Int, WebsiteEntity>

    /**
     * Get a website by its URL.
     * Returns null if not found.
     */
    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): WebsiteEntity?

    /**
     * Get a website by its URL as a Flow.
     * Emits updates whenever this website changes.
     */
    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    fun getByUrlFlow(url: String): Flow<WebsiteEntity?>

    /**
     * Search history by URL or title.
     * Case-insensitive search, returns up to 5 results.
     */
    @Query("""
        SELECT * FROM history
        WHERE url LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
        ORDER BY created_at DESC
        LIMIT 5
    """)
    suspend fun search(query: String): List<WebsiteEntity>

    /**
     * Search history by URL or title as a Flow.
     */
    @Query("""
        SELECT * FROM history
        WHERE url LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
        ORDER BY created_at DESC
        LIMIT 5
    """)
    fun searchFlow(query: String): Flow<List<WebsiteEntity>>

    /**
     * Get all bookmarked websites.
     */
    @Query("SELECT * FROM history WHERE bookmarked = 1 ORDER BY created_at DESC")
    fun getBookmarksFlow(): Flow<List<WebsiteEntity>>

    /**
     * Check if a URL exists in the database.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM history WHERE url = :url LIMIT 1)")
    suspend fun exists(url: String): Boolean

    /**
     * Get the visit count for a URL.
     */
    @Query("SELECT visit_count FROM history WHERE url = :url LIMIT 1")
    suspend fun getVisitCount(url: String): Int?

    /**
     * Insert a website. If URL already exists, replace it.
     * Returns the row ID of the inserted item.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(website: WebsiteEntity): Long

    /**
     * Insert multiple websites.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(websites: List<WebsiteEntity>)

    /**
     * Update a website.
     */
    @Update
    suspend fun update(website: WebsiteEntity)

    /**
     * Delete a website.
     */
    @Delete
    suspend fun delete(website: WebsiteEntity)

    /**
     * Delete a website by URL.
     */
    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    /**
     * Delete all history (nuclear option).
     */
    @Query("DELETE FROM history")
    suspend fun deleteAll()

    /**
     * Delete history older than a certain timestamp.
     * Useful for cleanup jobs.
     */
    @Query("DELETE FROM history WHERE created_at < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Increment visit count for a URL.
     * Updates the created_at timestamp to mark it as recently visited.
     */
    @Query("""
        UPDATE history
        SET visit_count = visit_count + 1,
            created_at = :timestamp
        WHERE url = :url
    """)
    suspend fun incrementVisitCount(url: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Toggle bookmark status for a URL.
     */
    @Query("UPDATE history SET bookmarked = NOT bookmarked WHERE url = :url")
    suspend fun toggleBookmark(url: String)

    /**
     * Set bookmark status for a URL.
     */
    @Query("UPDATE history SET bookmarked = :bookmarked WHERE url = :url")
    suspend fun setBookmarked(url: String, bookmarked: Boolean)

    /**
     * Get the total count of history items.
     */
    @Query("SELECT COUNT(*) FROM history")
    suspend fun getCount(): Int

    /**
     * Get the total count of bookmarked items.
     */
    @Query("SELECT COUNT(*) FROM history WHERE bookmarked = 1")
    suspend fun getBookmarkCount(): Int
}

/**
 * Helper function to upsert (insert or update) a website visit.
 * If the URL exists, increments visit count. Otherwise, inserts new entry.
 *
 * Usage:
 * ```
 * dao.upsertVisit(website.url) { websiteEntity }
 * ```
 */
suspend fun WebsiteDao.upsertVisit(
    url: String,
    entityProvider: () -> WebsiteEntity
) {
    val existing = getByUrl(url)
    if (existing != null) {
        // URL exists - increment visit count and update timestamp
        incrementVisitCount(url)
        // Optionally update other fields if they've changed
        val updated = entityProvider().copy(
            id = existing.id,
            visitCount = existing.visitCount + 1,
            createdAt = System.currentTimeMillis()
        )
        update(updated)
    } else {
        // New URL - insert it
        insert(entityProvider())
    }
}
