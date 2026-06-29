/*
 * Lynket
 *
 * Copyright (C) 2024 Arunkumar
 *
 * Replacement for dev.arunkumar.android:common SNAPSHOT dependency
 * Resource wrapper for async operations
 */
package dev.arunkumar.android.common

/**
 * A generic wrapper for async operations with loading, success, error, and idle states.
 * Similar to Result type but with explicit loading state.
 */
sealed class Resource<out T> {
    /**
     * Initial state before any operation
     */
    object Idle : Resource<Nothing>()

    /**
     * Operation is in progress
     */
    object Loading : Resource<Nothing>()

    /**
     * Operation completed successfully with data
     */
    data class Success<T>(val data: T) : Resource<T>()

    /**
     * Operation failed with error
     */
    data class Error(
        val throwable: Throwable,
        val message: String? = throwable.message
    ) : Resource<Nothing>()

    /**
     * Transform the data if this is a Success, otherwise return the same state
     */
    fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
        is Idle -> this
    }

    /**
     * Execute action if this is a Success
     */
    inline fun onSuccess(action: (T) -> Unit): Resource<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Execute action if this is an Error
     */
    inline fun onError(action: (Throwable) -> Unit): Resource<T> {
        if (this is Error) action(throwable)
        return this
    }

    /**
     * Execute action if this is Loading
     */
    inline fun onLoading(action: () -> Unit): Resource<T> {
        if (this is Loading) action()
        return this
    }

    /**
     * Get data or null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Get data or default value
     */
    fun getOrElse(default: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Success -> data
        else -> default
    }

    /**
     * Check if this is a success
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Check if this is an error
     */
    fun isError(): Boolean = this is Error

    /**
     * Check if this is loading
     */
    fun isLoading(): Boolean = this is Loading

    /**
     * Check if this is idle
     */
    fun isIdle(): Boolean = this is Idle

    companion object {
        /**
         * Create idle resource
         */
        fun <T> idle(): Resource<T> = Idle

        /**
         * Create loading resource
         */
        fun <T> loading(): Resource<T> = Loading

        /**
         * Create success resource
         */
        fun <T> success(data: T): Resource<T> = Success(data)

        /**
         * Create error resource
         */
        fun <T> error(throwable: Throwable, message: String? = null): Resource<T> =
            Error(throwable, message)
    }
}
