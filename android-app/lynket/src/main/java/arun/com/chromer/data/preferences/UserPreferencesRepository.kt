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

package arun.com.chromer.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 1.4: Modern preferences repository using DataStore
 *
 * Replaces legacy SharedPreferences (Preferences.java) with DataStore.
 * Provides type-safe, asynchronous access to app preferences using Kotlin Flow.
 *
 * Benefits over SharedPreferences:
 * - Asynchronous API (no blocking UI thread)
 * - Type-safe with compile-time checks
 * - Observable via Flow (reactive updates)
 * - Transactional updates (data consistency)
 * - Built-in migration from SharedPreferences
 *
 * All preferences are exposed as Flows that emit whenever values change.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    /**
     * Data class representing all user preferences.
     * Emitted by the preferences Flow whenever any preference changes.
     */
    data class UserPreferences(
        // First Run
        val isFirstRun: Boolean = true,

        // Toolbar Settings
        val isColoredToolbar: Boolean = true,
        val toolbarColor: Int = 0, // Will be set to colorPrimary in UI
        val dynamicToolbar: Boolean = false,
        val dynamicToolbarOnApp: Boolean = false,
        val dynamicToolbarOnWeb: Boolean = false,

        // Web Heads Settings
        val webHeadsEnabled: Boolean = false,
        val webHeadsColor: Int = 0, // Will be set to web_head_color in UI
        val webHeadsFavicons: Boolean = true,
        val webHeadsSpawnLocation: Int = 1,
        val webHeadsSize: Int = 1,
        val webHeadsCloseOnOpen: Boolean = false,

        // Animation Settings
        val animationType: Int = 1,
        val animationSpeed: Int = 1,

        // Browser Settings
        val preferredCustomTabPackage: String? = null,
        val secondaryBrowserComponent: String? = null,
        val useWebView: Boolean = false,
        val preferredAction: Int = 1, // PREFERRED_ACTION_BROWSER

        // Article Mode
        val articleMode: Boolean = false,
        val articleTheme: Int = 1, // THEME_DARK
        val articleTextSizeIncrement: Int = 0,

        // Features
        val ampMode: Boolean = false,
        val incognitoMode: Boolean = false,
        val fullIncognitoMode: Boolean = false,
        val bottomBarEnabled: Boolean = true,
        val mergeTabsAndApps: Boolean = true,
        val perAppSettings: Boolean = false,

        // Performance
        val warmUp: Boolean = false,
        val preFetch: Boolean = false,
        val wifiOnlyPrefetch: Boolean = false,
        val preFetchNotification: Boolean = true,
        val aggressiveLoading: Boolean = false,

        // Sharing
        val favShareComponent: String? = null,

        // Minimize
        val minimizeBehavior: String = "1"
    ) {
        /**
         * Helper to check if animation is enabled
         */
        val isAnimationEnabled: Boolean
            get() = animationType != 0

        /**
         * Helper to check if minimize to webhead is enabled
         */
        val minimizeToWebHead: Boolean
            get() = minimizeBehavior == "2"

        /**
         * Helper to check if dynamic toolbar is enabled for both app and web
         */
        val dynamicToolbarEnabledForBoth: Boolean
            get() = dynamicToolbar && dynamicToolbarOnWeb

        /**
         * Helper to check if app-based toolbar color is enabled
         */
        val isAppBasedToolbar: Boolean
            get() = dynamicToolbarOnApp && dynamicToolbar
    }

    /**
     * Flow of user preferences.
     * Emits the latest preferences whenever any value changes.
     * Catches IOExceptions and emits default values on error.
     */
    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading preferences")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferences(preferences)
        }

    /**
     * Maps DataStore Preferences to UserPreferences data class
     */
    private fun mapPreferences(preferences: Preferences): UserPreferences {
        return UserPreferences(
            isFirstRun = preferences[PreferencesKeys.FIRST_RUN] ?: true,
            isColoredToolbar = preferences[PreferencesKeys.TOOLBAR_COLOR_PREF] ?: true,
            toolbarColor = preferences[PreferencesKeys.TOOLBAR_COLOR] ?: 0,
            dynamicToolbar = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: false,
            dynamicToolbarOnApp = preferences[PreferencesKeys.DYNAMIC_COLOR_APP] ?: false,
            dynamicToolbarOnWeb = preferences[PreferencesKeys.DYNAMIC_COLOR_WEB] ?: false,
            webHeadsEnabled = preferences[PreferencesKeys.WEB_HEAD_ENABLED] ?: false,
            webHeadsColor = preferences[PreferencesKeys.WEB_HEADS_COLOR] ?: 0,
            webHeadsFavicons = preferences[PreferencesKeys.WEB_HEAD_FAVICON] ?: true,
            webHeadsSpawnLocation = preferences[PreferencesKeys.WEB_HEAD_SPAWN_LOCATION] ?: 1,
            webHeadsSize = preferences[PreferencesKeys.WEB_HEAD_SIZE] ?: 1,
            webHeadsCloseOnOpen = preferences[PreferencesKeys.WEB_HEAD_CLOSE_ON_OPEN] ?: false,
            animationType = preferences[PreferencesKeys.ANIMATION_TYPE] ?: 1,
            animationSpeed = preferences[PreferencesKeys.ANIMATION_SPEED] ?: 1,
            preferredCustomTabPackage = preferences[PreferencesKeys.PREFERRED_CUSTOM_TAB_PACKAGE],
            secondaryBrowserComponent = preferences[PreferencesKeys.SECONDARY_PREF],
            useWebView = preferences[PreferencesKeys.USE_WEBVIEW_PREF] ?: false,
            preferredAction = preferences[PreferencesKeys.PREFERRED_ACTION] ?: 1,
            articleMode = preferences[PreferencesKeys.ARTICLE_MODE] ?: false,
            articleTheme = preferences[PreferencesKeys.ARTICLE_THEME] ?: 1,
            articleTextSizeIncrement = preferences[PreferencesKeys.ARTICLE_TEXT_SIZE] ?: 0,
            ampMode = preferences[PreferencesKeys.AMP_MODE] ?: false,
            incognitoMode = preferences[PreferencesKeys.INCOGNITO_MODE] ?: false,
            fullIncognitoMode = preferences[PreferencesKeys.FULL_INCOGNITO_MODE] ?: false,
            bottomBarEnabled = preferences[PreferencesKeys.BOTTOM_BAR_ENABLED] ?: true,
            mergeTabsAndApps = preferences[PreferencesKeys.MERGE_TABS_AND_APPS] ?: true,
            perAppSettings = preferences[PreferencesKeys.PER_APP_SETTINGS] ?: false,
            warmUp = preferences[PreferencesKeys.WARM_UP] ?: false,
            preFetch = preferences[PreferencesKeys.PRE_FETCH] ?: false,
            wifiOnlyPrefetch = preferences[PreferencesKeys.WIFI_PREFETCH] ?: false,
            preFetchNotification = preferences[PreferencesKeys.PRE_FETCH_NOTIFICATION] ?: true,
            aggressiveLoading = preferences[PreferencesKeys.AGGRESSIVE_LOADING] ?: false,
            favShareComponent = preferences[PreferencesKeys.FAV_SHARE_PREF],
            minimizeBehavior = preferences[PreferencesKeys.MINIMIZE_BEHAVIOR_PREFERENCE] ?: "1"
        )
    }

    // ========== Update Methods ==========
    // Suspend functions for updating individual preferences

    suspend fun setFirstRunComplete() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_RUN] = false
        }
    }

    suspend fun setToolbarColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOOLBAR_COLOR] = color
        }
    }

    suspend fun setColoredToolbar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOOLBAR_COLOR_PREF] = enabled
        }
    }

    suspend fun setWebHeadsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEB_HEAD_ENABLED] = enabled
        }
    }

    suspend fun setWebHeadsColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEB_HEADS_COLOR] = color
        }
    }

    suspend fun setWebHeadsFavicons(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEB_HEAD_FAVICON] = enabled
        }
    }

    suspend fun setWebHeadsCloseOnOpen(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEB_HEAD_CLOSE_ON_OPEN] = enabled
        }
    }

    suspend fun setDynamicToolbar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setDynamicToolbarOnApp(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR_APP] = enabled
        }
    }

    suspend fun setDynamicToolbarOnWeb(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR_WEB] = enabled
        }
    }

    suspend fun setAmpMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AMP_MODE] = enabled
        }
    }

    suspend fun setArticleMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARTICLE_MODE] = enabled
        }
    }

    suspend fun setArticleTheme(theme: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARTICLE_THEME] = theme
        }
    }

    suspend fun setArticleTextSizeIncrement(increment: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARTICLE_TEXT_SIZE] = increment
        }
    }

    suspend fun setIncognitoMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.INCOGNITO_MODE] = enabled
        }
    }

    suspend fun setFullIncognitoMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_INCOGNITO_MODE] = enabled
        }
    }

    suspend fun setUseWebView(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_WEBVIEW_PREF] = enabled
        }
    }

    suspend fun setCustomTabPackage(packageName: String?) {
        dataStore.edit { preferences ->
            if (packageName != null) {
                preferences[PreferencesKeys.PREFERRED_CUSTOM_TAB_PACKAGE] = packageName
            } else {
                preferences.remove(PreferencesKeys.PREFERRED_CUSTOM_TAB_PACKAGE)
            }
            // When setting custom tab, disable WebView
            preferences[PreferencesKeys.USE_WEBVIEW_PREF] = false
        }
    }

    suspend fun setSecondaryBrowserComponent(component: String?) {
        dataStore.edit { preferences ->
            if (component != null) {
                preferences[PreferencesKeys.SECONDARY_PREF] = component
            } else {
                preferences.remove(PreferencesKeys.SECONDARY_PREF)
            }
        }
    }

    suspend fun setFavShareComponent(component: String?) {
        dataStore.edit { preferences ->
            if (component != null) {
                preferences[PreferencesKeys.FAV_SHARE_PREF] = component
            } else {
                preferences.remove(PreferencesKeys.FAV_SHARE_PREF)
            }
        }
    }

    suspend fun setWarmUp(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WARM_UP] = enabled
        }
    }

    suspend fun setPreFetch(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRE_FETCH] = enabled
        }
    }

    suspend fun setWifiOnlyPrefetch(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_PREFETCH] = enabled
        }
    }

    suspend fun setPreFetchNotification(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRE_FETCH_NOTIFICATION] = enabled
        }
    }

    suspend fun setAggressiveLoading(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AGGRESSIVE_LOADING] = enabled
        }
    }

    suspend fun setBottomBar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BOTTOM_BAR_ENABLED] = enabled
        }
    }

    suspend fun setMergeTabs(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MERGE_TABS_AND_APPS] = enabled
        }
    }

    suspend fun setPerAppSettings(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PER_APP_SETTINGS] = enabled
        }
    }

    suspend fun setMinimizeBehavior(behavior: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MINIMIZE_BEHAVIOR_PREFERENCE] = behavior
        }
    }

    suspend fun setAnimationType(type: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANIMATION_TYPE] = type
        }
    }

    suspend fun setAnimationSpeed(speed: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANIMATION_SPEED] = speed
        }
    }

    suspend fun setPreferredAction(action: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_ACTION] = action
        }
    }

    suspend fun setWebHeadsSpawnLocation(location: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEB_HEAD_SPAWN_LOCATION] = location
        }
    }

    suspend fun setWebHeadsSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEB_HEAD_SIZE] = size
        }
    }

    /**
     * Clear all preferences (for testing or reset)
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * DataStore preference keys
     */
    private object PreferencesKeys {
        val FIRST_RUN = booleanPreferencesKey("firstrun_3")
        val TOOLBAR_COLOR_PREF = booleanPreferencesKey("toolbar_color_pref")
        val TOOLBAR_COLOR = intPreferencesKey("toolbar_color")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DYNAMIC_COLOR_APP = booleanPreferencesKey("dynamic_color_app")
        val DYNAMIC_COLOR_WEB = booleanPreferencesKey("dynamic_color_web")
        val WEB_HEAD_ENABLED = booleanPreferencesKey("webhead_enabled_pref")
        val WEB_HEADS_COLOR = intPreferencesKey("webhead_color")
        val WEB_HEAD_FAVICON = booleanPreferencesKey("webhead_favicons_pref")
        val WEB_HEAD_SPAWN_LOCATION = intPreferencesKey("webhead_spawn_preference")
        val WEB_HEAD_SIZE = intPreferencesKey("webhead_size_preference")
        val WEB_HEAD_CLOSE_ON_OPEN = booleanPreferencesKey("webhead_close_onclick_pref")
        val ANIMATION_TYPE = intPreferencesKey("animation_preference")
        val ANIMATION_SPEED = intPreferencesKey("animation_speed_preference")
        val PREFERRED_CUSTOM_TAB_PACKAGE = stringPreferencesKey("preferred_package")
        val SECONDARY_PREF = stringPreferencesKey("secondary_preference")
        val USE_WEBVIEW_PREF = booleanPreferencesKey("use_webview_pref")
        val PREFERRED_ACTION = intPreferencesKey("preferred_action_preference")
        val ARTICLE_MODE = booleanPreferencesKey("article_mode_pref")
        val ARTICLE_THEME = intPreferencesKey("article_theme_preference")
        val ARTICLE_TEXT_SIZE = intPreferencesKey("article_text_size_pref")
        val AMP_MODE = booleanPreferencesKey("amp_mode_pref")
        val INCOGNITO_MODE = booleanPreferencesKey("incognito_mode_pref")
        val FULL_INCOGNITO_MODE = booleanPreferencesKey("full_incognito_mode")
        val BOTTOM_BAR_ENABLED = booleanPreferencesKey("bottombar_enabled_preference")
        val MERGE_TABS_AND_APPS = booleanPreferencesKey("merge_tabs_and_apps_preference")
        val PER_APP_SETTINGS = booleanPreferencesKey("blacklist_preference")
        val WARM_UP = booleanPreferencesKey("warm_up_preference")
        val PRE_FETCH = booleanPreferencesKey("pre_fetch_preference")
        val WIFI_PREFETCH = booleanPreferencesKey("wifi_preference")
        val PRE_FETCH_NOTIFICATION = booleanPreferencesKey("pre_fetch_notification_preference")
        val AGGRESSIVE_LOADING = booleanPreferencesKey("aggressive_loading")
        val FAV_SHARE_PREF = stringPreferencesKey("fav_share_preference")
        val MINIMIZE_BEHAVIOR_PREFERENCE = stringPreferencesKey("minimize_behavior_preference")
    }
}
