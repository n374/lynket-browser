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
import arun.com.chromer.data.database.ChromerDatabase
import arun.com.chromer.data.database.dao.WebsiteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Phase 1.3: Hilt module for Room Database
 *
 * Provides database and DAO instances to the dependency injection graph.
 * All dependencies are scoped to the application lifecycle (@Singleton).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the ChromerDatabase instance.
     * Database is created once and reused throughout the app lifecycle.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChromerDatabase {
        return ChromerDatabase.create(context)
    }

    /**
     * Provides WebsiteDao for history/website operations.
     * The DAO is obtained from the database instance.
     */
    @Provides
    @Singleton
    fun provideWebsiteDao(database: ChromerDatabase): WebsiteDao {
        return database.websiteDao()
    }

    // Future DAOs will be added here:
    // @Provides
    // @Singleton
    // fun provideTabDao(database: ChromerDatabase): TabDao = database.tabDao()
    //
    // @Provides
    // @Singleton
    // fun provideWebArticleDao(database: ChromerDatabase): WebArticleDao = database.webArticleDao()
}
