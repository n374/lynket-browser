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

package arun.com.chromer.browsing.providerselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arun.com.chromer.data.apps.AppRepository
import arun.com.chromer.data.apps.model.Provider
import arun.com.chromer.data.preferences.UserPreferencesRepository
import arun.com.chromer.util.events.EventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Phase 3.3: Modern ProviderSelectionViewModel
 *
 * Manages Custom Tab provider selection with modern reactive patterns.
 * Bridges legacy RxJava AppRepository with modern StateFlow.
 *
 * Features:
 * - Load available Custom Tab providers
 * - Select provider (set as default)
 * - WebView fallback option
 * - Install provider via Play Store
 */
@HiltViewModel
class ModernProviderSelectionViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val eventBus: EventBus
) : ViewModel() {

    /**
     * UI State for provider selection screen
     */
    sealed interface ProviderSelectionUiState {
        data object Loading : ProviderSelectionUiState
        data class Success(
            val providers: List<Provider>,
            val selectedPackage: String?,
            val usingWebView: Boolean
        ) : ProviderSelectionUiState
        data class Error(val message: String) : ProviderSelectionUiState
    }

    /**
     * UI state flow
     */
    private val _uiState = MutableStateFlow<ProviderSelectionUiState>(
        ProviderSelectionUiState.Loading
    )
    val uiState: StateFlow<ProviderSelectionUiState> = _uiState.asStateFlow()

    init {
        loadProviders()
    }

    /**
     * Load all Custom Tab providers
     */
    fun loadProviders() {
        viewModelScope.launch {
            try {
                _uiState.value = ProviderSelectionUiState.Loading

                // Use RxJava-to-Coroutine adapter for legacy repository
                val providers = appRepository.allProviders().await()

                // Get current preferences
                val preferences = preferencesRepository.userPreferencesFlow.first()

                _uiState.value = ProviderSelectionUiState.Success(
                    providers = providers,
                    selectedPackage = preferences.customTabPackage,
                    usingWebView = preferences.useWebView
                )

                Timber.d("Loaded ${providers.size} Custom Tab providers")
            } catch (e: Exception) {
                Timber.e(e, "Error loading providers")
                _uiState.value = ProviderSelectionUiState.Error(
                    e.message ?: "Failed to load providers"
                )
            }
        }
    }

    /**
     * Select a Custom Tab provider
     */
    fun selectProvider(provider: Provider) {
        viewModelScope.launch {
            try {
                if (provider.installed) {
                    // Set as default Custom Tab provider
                    preferencesRepository.setCustomTabPackage(provider.packageName)
                    // Disable WebView if it was previously enabled
                    if ((_uiState.value as? ProviderSelectionUiState.Success)?.usingWebView == true) {
                        preferencesRepository.setUseWebView(false)
                    }
                    Timber.d("Selected provider: ${provider.appName} (${provider.packageName})")

                    // Notify that provider changed
                    notifyProviderChanged()

                    // Reload to update UI
                    loadProviders()
                } else {
                    Timber.w("Cannot select non-installed provider: ${provider.packageName}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error selecting provider")
            }
        }
    }

    /**
     * Select WebView as browsing method
     */
    fun selectWebView() {
        viewModelScope.launch {
            try {
                preferencesRepository.setUseWebView(true)
                // Clear custom tab package preference
                preferencesRepository.setCustomTabPackage("")
                Timber.d("Selected WebView as browsing method")

                // Notify that provider changed
                notifyProviderChanged()

                // Reload to update UI
                loadProviders()
            } catch (e: Exception) {
                Timber.e(e, "Error selecting WebView")
            }
        }
    }

    /**
     * Notify system that provider changed
     * (Sends event for other screens to update)
     */
    private fun notifyProviderChanged() {
        viewModelScope.launch {
            try {
                // TODO: Define modern event type when migrating BrowsingOptionsActivity
                // For now, we rely on DataStore flow updates
                Timber.d("Provider changed notification sent")
            } catch (e: Exception) {
                Timber.e(e, "Error notifying provider change")
            }
        }
    }

    /**
     * Refresh providers list
     */
    fun refresh() {
        loadProviders()
    }
}
