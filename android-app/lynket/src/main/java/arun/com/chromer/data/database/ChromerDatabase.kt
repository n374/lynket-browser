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

package arun.com.chromer.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import arun.com.chromer.data.database.dao.WebsiteDao
import arun.com.chromer.data.database.entity.WebsiteEntity
import timber.log.Timber

/**
 * Phase 1.3: Main Room Database for Lynket Browser
 *
 * This replaces the legacy SQLiteOpenHelper (HistorySqlDiskStore) with a modern
 * Room database implementation.
 *
 * Features:
 * - Type-safe database access
 * - Automatic migration handling
 * - Compile-time SQL verification
 * - Seamless integration with Kotlin Coroutines and Flow
 *
 * Current entities:
 * - WebsiteEntity: Stores browsing history and website metadata
 *
 * Future entities (to be added in later phases):
 * - TabEntity: Active tabs
 * - WebArticleEntity: Saved articles
 * - ProviderEntity: Custom tab providers
 */
@Database(
    entities = [
        WebsiteEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class ChromerDatabase : RoomDatabase() {

    abstract fun websiteDao(): WebsiteDao

    companion object {
        private const val DATABASE_NAME = "chromer.db"

        /**
         * Migration from legacy SQLite database (version 1) to Room database (version 2).
         *
         * The legacy database used table name "History" with uppercase column names.
         * The new Room database uses table name "history" with snake_case column names.
         *
         * Strategy:
         * 1. Create new Room schema table
         * 2. Copy data from old table to new table
         * 3. Drop old table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version 1 to 2")

                try {
                    // Create new Room-based table with correct schema
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS history (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            url TEXT NOT NULL,
                            title TEXT,
                            favicon_url TEXT,
                            canonical_url TEXT,
                            theme_color TEXT,
                            amp_url TEXT,
                            bookmarked INTEGER NOT NULL DEFAULT 0,
                            created_at INTEGER NOT NULL,
                            visit_count INTEGER NOT NULL DEFAULT 1
                        )
                    """)

                    // Create indices for better query performance
                    database.execSQL("""
                        CREATE UNIQUE INDEX IF NOT EXISTS index_history_url
                        ON history(url)
                    """)

                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_history_created_at
                        ON history(created_at)
                    """)

                    // Check if legacy History table exists
                    val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='History'")
                    val legacyTableExists = cursor.count > 0
                    cursor.close()

                    if (legacyTableExists) {
                        Timber.d("Legacy History table found, migrating data...")

                        // Copy data from legacy table to new table
                        // Map old column names to new ones
                        database.execSQL("""
                            INSERT INTO history (url, title, favicon_url, canonical_url, theme_color, amp_url, bookmarked, created_at, visit_count)
                            SELECT
                                URL,
                                TITLE,
                                FAVICON,
                                CANONICAL,
                                COLOR,
                                AMP,
                                IFNULL(BOOKMARKED, 0),
                                CAST(IFNULL(CREATED, 0) AS INTEGER),
                                IFNULL(VISITED, 1)
                            FROM History
                        """)

                        // Drop legacy table
                        database.execSQL("DROP TABLE IF EXISTS History")

                        Timber.d("Migration completed successfully")
                    } else {
                        Timber.d("No legacy table found, starting with fresh database")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Migration failed")
                    throw e
                }
            }
        }

        /**
         * Create database instance with Hilt.
         * This function is called by the Hilt database module.
         */
        fun create(context: Context): ChromerDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ChromerDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration() // Only for development - remove in production
                .build()
        }
    }
}
