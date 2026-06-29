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

package arun.com.chromer.ui.article

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import arun.com.chromer.browsing.article.ArticleActivity
import timber.log.Timber

/**
 * Phase 3.3: ArticleScreen - Compose wrapper for article/reader mode
 *
 * This screen acts as a bridge between the modern Compose navigation
 * and the legacy ArticleActivity, which handles article parsing and display.
 *
 * It launches ArticleActivity and immediately navigates back,
 * similar to how BrowserScreen works.
 *
 * Future Enhancement: When article mode is fully modernized, this screen
 * could contain:
 * - Article parsing UI (Mercury parser, Readability, etc.)
 * - Reader mode with adjustable text size
 * - Theming options (light/dark/sepia)
 * - Bookmark/save functionality
 * - Text-to-speech integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    url: String,
    navController: NavController
) {
    val context = LocalContext.current

    // Launch article activity when screen is first composed
    LaunchedEffect(url) {
        try {
            Timber.d("ArticleScreen: Launching article mode for URL: $url")

            // Launch ArticleActivity (which handles article extraction and display)
            val intent = Intent(context, ArticleActivity::class.java).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)

            // Navigate back immediately - the article display happens in ArticleActivity
            // This matches the behavior of the legacy flow
            navController.popBackStack()
        } catch (e: Exception) {
            Timber.e(e, "Error launching article mode for URL: $url")
            // Stay on screen to show error
        }
    }

    // Show a loading indicator while launching
    // (User will typically only see this for a split second)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opening Article...") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()

                Text(
                    text = "Extracting article...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Reader mode provides distraction-free reading",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
