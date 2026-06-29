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

package arun.com.chromer.util.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 1.5/2: Modern event bus using Kotlin SharedFlow
 *
 * Replaces RxEventBus (RxJava PublishSubject) with Kotlin SharedFlow.
 * Provides type-safe, coroutine-based event communication across components.
 *
 * Benefits over RxEventBus:
 * - No RxJava dependency
 * - Built-in backpressure handling
 * - Coroutine-native (suspend functions)
 * - Type-safe event filtering with inline reified types
 * - Replay support for late subscribers
 *
 * Usage:
 * ```kotlin
 * // Post event
 * viewModelScope.launch {
 *     eventBus.emit(WebHeadCreatedEvent(url))
 * }
 *
 * // Observe specific event type
 * viewModelScope.launch {
 *     eventBus.observe<WebHeadCreatedEvent>()
 *         .collect { event ->
 *             // Handle event
 *         }
 * }
 * ```
 */
@Singleton
class EventBus @Inject constructor() {

    private val _events = MutableSharedFlow<Event>(
        replay = 0, // Don't replay old events to new subscribers
        extraBufferCapacity = 64, // Buffer up to 64 events
        onBufferOverflow = BufferOverflow.DROP_OLDEST // Drop oldest if buffer full
    )

    /**
     * SharedFlow of all events.
     * Observers can filter for specific event types.
     */
    val events: SharedFlow<Event> = _events.asSharedFlow()

    /**
     * Emit an event to all observers.
     * This is a suspend function and should be called from a coroutine scope.
     *
     * @param event The event to emit
     */
    suspend fun emit(event: Event) {
        Timber.d("Emitting event: ${event::class.simpleName}")
        _events.emit(event)
    }

    /**
     * Emit an event synchronously (non-suspend).
     * Use this when you can't use suspend functions.
     * Note: May fail if buffer is full, returns false in that case.
     *
     * @param event The event to emit
     * @return true if event was emitted, false if buffer was full
     */
    fun tryEmit(event: Event): Boolean {
        val success = _events.tryEmit(event)
        if (success) {
            Timber.d("Emitted event: ${event::class.simpleName}")
        } else {
            Timber.w("Failed to emit event (buffer full): ${event::class.simpleName}")
        }
        return success
    }

    /**
     * Observe events of a specific type.
     * Uses inline reified type parameter for type-safe filtering.
     *
     * Example:
     * ```kotlin
     * eventBus.observe<WebHeadCreatedEvent>()
     *     .collect { event ->
     *         // Only WebHeadCreatedEvent instances
     *     }
     * ```
     */
    inline fun <reified T : Event> observe(): kotlinx.coroutines.flow.Flow<T> {
        return events.filterIsInstance<T>()
    }
}

/**
 * Base sealed interface for all events.
 * All events must implement this interface.
 *
 * Sealed interfaces ensure exhaustive when() expressions and
 * provide compile-time safety for event types.
 */
sealed interface Event {

    /**
     * Web Heads related events
     */
    sealed interface WebHeadEvent : Event {
        data class Created(val url: String) : WebHeadEvent
        data class Clicked(val url: String) : WebHeadEvent
        data class Closed(val url: String) : WebHeadEvent
        data object AllClosed : WebHeadEvent
    }

    /**
     * Tab related events
     */
    sealed interface TabEvent : Event {
        data class Added(val url: String) : TabEvent
        data class Removed(val url: String) : TabEvent
        data class Updated(val url: String) : TabEvent
        data object AllClosed : TabEvent
    }

    /**
     * Browser related events
     */
    sealed interface BrowserEvent : Event {
        data class PageLoaded(val url: String) : BrowserEvent
        data class PageStarted(val url: String) : BrowserEvent
        data class PageError(val url: String, val errorCode: Int) : BrowserEvent
        data class DownloadStarted(val url: String) : BrowserEvent
    }

    /**
     * History related events
     */
    sealed interface HistoryEvent : Event {
        data class ItemAdded(val url: String) : HistoryEvent
        data class ItemRemoved(val url: String) : HistoryEvent
        data object Cleared : HistoryEvent
    }

    /**
     * Settings related events
     */
    sealed interface SettingsEvent : Event {
        data object Changed : SettingsEvent
        data class ThemeChanged(val isDark: Boolean) : SettingsEvent
        data class WebHeadsToggled(val enabled: Boolean) : SettingsEvent
    }

    /**
     * General app events
     */
    sealed interface AppEvent : Event {
        data object RefreshRequested : AppEvent
        data object BackPressed : AppEvent
        data class DeepLinkReceived(val url: String) : AppEvent
    }
}
