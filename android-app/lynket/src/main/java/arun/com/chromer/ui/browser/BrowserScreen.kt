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

package arun.com.chromer.ui.browser

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
import arun.com.chromer.browsing.customtabs.CustomTabActivity
import timber.log.Timber

/**
 * Phase 3.2: BrowserScreen - Compose wrapper for browsing
 *
 * This screen acts as a bridge between the modern Compose navigation
 * and the legacy browsing infrastructure (CustomTabActivity/WebViewActivity).
 *
 * It launches the appropriate browsing activity based on user preferences
 * and immediately navigates back, similar to how CustomTabActivity behaves.
 *
 * Future Enhancement: When fully migrated to Compose, this could contain
 * an embedded WebView or Custom Tab session directly in Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    url: String,
    navController: NavController
) {
    val context = LocalContext.current

    // Launch browsing activity when screen is first composed
    LaunchedEffect(url) {
        try {
            Timber.d("BrowserScreen: Launching browser for URL: $url")

            // Launch CustomTabActivity (which handles Custom Tabs vs WebView logic)
            val intent = Intent(context, CustomTabActivity::class.java).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)

            // Navigate back immediately - the browsing happens in CustomTabActivity
            // This matches the behavior of the legacy flow
            navController.popBackStack()
        } catch (e: Exception) {
            Timber.e(e, "Error launching browser for URL: $url")
            // Stay on screen to show error
        }
    }

    // Show a loading indicator while launching
    // (User will typically only see this for a split second)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opening...") },
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
                    text = "Opening browser...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
