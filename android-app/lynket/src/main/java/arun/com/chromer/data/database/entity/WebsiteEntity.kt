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

package arun.com.chromer.data.database.entity

import android.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.shared.Constants

/**
 * Phase 1.3: Room entity for Website/History data
 *
 * Replaces the legacy SQLite HistoryTable schema with a modern Room entity.
 * This entity represents both browsing history and website metadata.
 *
 * @property id Auto-generated primary key
 * @property url The website URL (indexed for fast lookups)
 * @property title The page title
 * @property faviconUrl URL of the website's favicon
 * @property canonicalUrl The canonical URL of the page
 * @property themeColor The website's theme color as a hex string
 * @property ampUrl AMP version URL if available
 * @property bookmarked Whether the user has bookmarked this site
 * @property createdAt Timestamp when first visited (milliseconds since epoch)
 * @property visitCount Number of times the user has visited this site
 */
@Entity(
    tableName = "history",
    indices = [
        Index(value = ["url"], unique = true),
        Index(value = ["created_at"])
    ]
)
data class WebsiteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "favicon_url")
    val faviconUrl: String? = null,

    @ColumnInfo(name = "canonical_url")
    val canonicalUrl: String? = null,

    @ColumnInfo(name = "theme_color")
    val themeColor: String? = null,

    @ColumnInfo(name = "amp_url")
    val ampUrl: String? = null,

    @ColumnInfo(name = "bookmarked")
    val bookmarked: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "visit_count")
    val visitCount: Int = 1
)

/**
 * Extension functions to convert between legacy Website model and Room entity
 */

/**
 * Converts legacy Website Java model to Room entity
 */
fun Website.toEntity(): WebsiteEntity {
    return WebsiteEntity(
        url = this.url,
        title = this.title,
        faviconUrl = this.faviconUrl,
        canonicalUrl = this.canonicalUrl,
        themeColor = this.themeColor,
        ampUrl = this.ampUrl,
        bookmarked = this.bookmarked,
        createdAt = this.createdAt,
        visitCount = this.count
    )
}

/**
 * Converts Room entity to legacy Website Java model
 */
fun WebsiteEntity.toWebsite(): Website {
    return Website(
        title,
        url,
        faviconUrl,
        canonicalUrl,
        themeColor,
        ampUrl,
        bookmarked,
        createdAt,
        visitCount
    )
}

/**
 * Helper to parse theme color safely
 */
fun WebsiteEntity.themeColorInt(): Int {
    return try {
        if (themeColor != null) {
            Color.parseColor(themeColor)
        } else {
            Constants.NO_COLOR
        }
    } catch (e: Exception) {
        Constants.NO_COLOR
    }
}
