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

package arun.com.chromer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Phase 3.1: Material3 theme for Lynket Browser
 *
 * Provides a modern Material3 theme with:
 * - Dynamic color (Android 12+)
 * - Dark/Light theme support
 * - Lynket brand colors as fallback
 */

// Lynket brand colors
private val LynketBlue = Color(0xFF1E88E5)
private val LynketBlueVariant = Color(0xFF1565C0)
private val LynketCyan = Color(0xFF26C6DA)

/**
 * Dark color scheme for Lynket
 */
private val DarkColorScheme = darkColorScheme(
    primary = LynketBlue,
    onPrimary = Color.White,
    primaryContainer = LynketBlueVariant,
    onPrimaryContainer = Color(0xFFD1E3FF),
    secondary = LynketCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00838F),
    onSecondaryContainer = Color(0xFFB3E5FC),
    tertiary = Color(0xFFEF5350),
    onTertiary = Color.White,
    error = Color(0xFFF44336),
    onError = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE1E2E6),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE1E2E6),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFC7C7C7)
)

/**
 * Light color scheme for Lynket
 */
private val LightColorScheme = lightColorScheme(
    primary = LynketBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E3FF),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = LynketCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB3E5FC),
    onSecondaryContainer = Color(0xFF00363D),
    tertiary = Color(0xFFEF5350),
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF44464E)
)

/**
 * Lynket Browser theme.
 *
 * Automatically uses:
 * - Dynamic color on Android 12+ (if enabled)
 * - Dark theme based on system settings (if enabled)
 * - Lynket brand colors as fallback
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use dynamic color (Android 12+)
 * @param content The composable content
 */
@Composable
fun ChromerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color is available on Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // Use predefined dark/light schemes
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Update system bar colors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Custom typography for Lynket.
 * Uses default Material3 typography with potential customizations.
 */
private val Typography = Typography(
    // Can customize fonts here if needed
    // headlineLarge = TextStyle(
    //     fontFamily = CustomFontFamily,
    //     fontWeight = FontWeight.Bold,
    //     fontSize = 32.sp
    // ),
    // etc.
)
