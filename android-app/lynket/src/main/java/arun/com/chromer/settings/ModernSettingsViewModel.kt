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

package arun.com.chromer.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arun.com.chromer.data.preferences.UserPreferencesRepository
import arun.com.chromer.data.preferences.UserPreferencesRepository.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Phase 3: Modern SettingsViewModel
 *
 * Demonstrates DataStore integration with Compose:
 * - Real-time preference updates via Flow
 * - Type-safe preference management
 * - Instant UI reactivity
 * - No manual preference file management
 *
 * All settings changes are automatically persisted to DataStore
 * and reactively update the UI.
 */
@HiltViewModel
class ModernSettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /**
     * User preferences as StateFlow.
     * UI automatically updates when any preference changes.
     */
    val preferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    // ========== Web Heads Settings ==========

    fun setWebHeadsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setWebHeadsEnabled(enabled)
                Timber.d("Web heads enabled: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Error setting web heads")
            }
        }
    }

    fun setWebHeadsFavicons(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setWebHeadsFavicons(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting favicons")
            }
        }
    }

    fun setWebHeadsCloseOnOpen(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setWebHeadsCloseOnOpen(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting close on open")
            }
        }
    }

    // ========== Browser Settings ==========

    fun setIncognitoMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setIncognitoMode(enabled)
                Timber.d("Incognito mode: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Error setting incognito mode")
            }
        }
    }

    fun setAmpMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setAmpMode(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting AMP mode")
            }
        }
    }

    fun setArticleMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setArticleMode(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting article mode")
            }
        }
    }

    fun setUseWebView(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setUseWebView(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting WebView")
            }
        }
    }

    // ========== Appearance Settings ==========

    fun setDynamicToolbar(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setDynamicToolbar(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting dynamic toolbar")
            }
        }
    }

    fun setBottomBar(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setBottomBar(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting bottom bar")
            }
        }
    }

    // ========== Performance Settings ==========

    fun setWarmUp(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setWarmUp(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting warm up")
            }
        }
    }

    fun setPreFetch(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setPreFetch(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting prefetch")
            }
        }
    }

    fun setAggressiveLoading(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setAggressiveLoading(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting aggressive loading")
            }
        }
    }

    // ========== Advanced Settings ==========

    fun setPerAppSettings(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setPerAppSettings(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting per-app settings")
            }
        }
    }

    fun setMergeTabs(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setMergeTabs(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error setting merge tabs")
            }
        }
    }
}
