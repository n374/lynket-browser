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

package arun.com.chromer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import arun.com.chromer.ui.navigation.ChromerNavGraph
import arun.com.chromer.ui.theme.ChromerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Phase 3: Modern MainActivity using Jetpack Compose
 *
 * This replaces the legacy HomeActivity as the main entry point.
 * Uses the full modern Android stack:
 * - ComponentActivity (Compose-optimized)
 * - @AndroidEntryPoint for Hilt injection
 * - Edge-to-edge display
 * - ChromerNavGraph for navigation
 * - Material3 theme
 *
 * This activity serves as the container for the entire Compose UI.
 * All screens are rendered inside ChromerNavGraph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display (modern Android pattern)
        enableEdgeToEdge()

        // Set Compose content
        setContent {
            ChromerTheme {
                // Surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Navigation graph handles all screens
                    ChromerNavGraph()
                }
            }
        }
    }
}
