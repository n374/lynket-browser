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

package arun.com.chromer.di.hilt

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Singleton

/**
 * Phase 1.4: Hilt module for DataStore
 *
 * Provides DataStore instance with automatic migration from SharedPreferences.
 * All existing user preferences are seamlessly migrated on first DataStore access.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val USER_PREFERENCES_NAME = "user_preferences"

    /**
     * Provides SharedPreferences for migration purposes.
     * Legacy code can still use this during transition period.
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Provides DataStore<Preferences> with migration from SharedPreferences.
     *
     * Features:
     * - Automatic migration from default SharedPreferences
     * - Corruption handling (returns empty preferences on error)
     * - Runs on IO dispatcher for optimal performance
     * - Migrates ALL existing preferences automatically
     *
     * Migration happens automatically on first DataStore access.
     * After migration, SharedPreferences data remains intact (not deleted).
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { exception ->
                    Timber.e(exception, "DataStore file corrupted, using empty preferences")
                    emptyPreferences()
                }
            ),
            migrations = listOf(
                // Migrate ALL keys from SharedPreferences to DataStore
                SharedPreferencesMigration(
                    context = context,
                    sharedPreferencesName = PreferenceManager.getDefaultSharedPreferencesName(context),
                    // Migrate all keys (don't specify keysToMigrate to migrate everything)
                    shouldRunMigration = { true }
                )
            ),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile(USER_PREFERENCES_NAME) }
        )
    }
}
