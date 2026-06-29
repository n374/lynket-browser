/*
 * Lynket
 *
 * Copyright (C) 2024 Arunkumar
 *
 * Replacement for dev.arunkumar.android:common SNAPSHOT dependency
 * Original library no longer available
 */
package dev.arunkumar.android.common

import android.content.res.Resources
import android.util.TypedValue

/**
 * Convert dp to pixels
 */
fun Int.dpToPx(): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()
}

/**
 * Convert dp to pixels (Float version)
 */
fun Float.dpToPx(): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )
}

/**
 * Convert sp to pixels
 */
fun Int.spToPx(): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()
}

/**
 * Convert sp to pixels (Float version)
 */
fun Float.spToPx(): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        Resources.getSystem().displayMetrics
    )
}

/**
 * Placeholder for common() function - needs investigation
 * TODO: Determine what this should return based on usage
 */
fun <T> common(): T {
    throw NotImplementedError("common() function needs implementation based on usage context")
}
