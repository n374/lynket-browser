/*
 * Lynket
 *
 * Copyright (C) 2024 Arunkumar
 *
 * Replacement for dev.arunkumar.android:epoxy-utils SNAPSHOT dependency
 * Epoxy RecyclerView utilities
 */
package dev.arunkumar.android.epoxy

import com.airbnb.epoxy.EpoxyModel

/**
 * Span size override callback that makes models take full width
 */
object TotalSpanOverride : EpoxyModel.SpanSizeOverrideCallback {
    override fun getSpanSize(totalSpanCount: Int, position: Int, itemCount: Int): Int {
        return totalSpanCount
    }
}

/**
 * Set the span size for this model in a grid layout
 * Returns the same model for chaining
 */
fun <T : EpoxyModel<*>> T.span(spanCount: Int): T {
    return this.apply {
        spanSizeOverride { totalSpanCount, position, itemCount ->
            spanCount.coerceAtMost(totalSpanCount)
        }
    }
}

/**
 * Make this model span the full width in a grid layout
 */
fun <T : EpoxyModel<*>> T.fullSpan(): T {
    return this.apply {
        spanSizeOverride { totalSpanCount, position, itemCount ->
            totalSpanCount
        }
    }
}

/**
 * Placeholder for span() property - needs investigation
 * TODO: Determine what this should return based on usage in SuggestionController.kt
 */
val Any.span: Int
    get() {
        throw NotImplementedError("span property needs implementation based on usage context")
    }
