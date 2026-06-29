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

package arun.com.chromer.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import arun.com.chromer.BuildConfig

/**
 * Phase 3.3: AboutScreen
 *
 * Modern Compose UI for app information and links.
 *
 * Features:
 * - App version and build info
 * - License information (GPL-3.0)
 * - Social media links (Twitter, Reddit)
 * - GitHub source repository
 * - Developer information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // App Section
            item {
                SectionHeader("App Information")
            }

            item {
                AboutItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    onClick = null
                )
            }

            item {
                AboutItem(
                    icon = Icons.Default.Gavel,
                    title = "License",
                    subtitle = "GNU General Public License v3.0",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                AboutItem(
                    icon = Icons.Default.Code,
                    title = "Source Code",
                    subtitle = "View on GitHub",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://github.com/arunkumar9t2/lynket-browser")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                AboutItem(
                    icon = Icons.Default.BugReport,
                    title = "Report Issues",
                    subtitle = "GitHub Issues",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://github.com/arunkumar9t2/lynket-browser/issues")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Community Section
            item {
                SectionHeader("Community")
            }

            item {
                AboutItem(
                    icon = Icons.Default.ChatBubble,
                    title = "Reddit Community",
                    subtitle = "r/lynket",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://www.reddit.com/r/lynket/")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                AboutItem(
                    icon = Icons.Default.Tag,
                    title = "Twitter",
                    subtitle = "@LynketApp",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://twitter.com/LynketApp")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Developer Section
            item {
                SectionHeader("Developer")
            }

            item {
                AboutItem(
                    icon = Icons.Default.Person,
                    title = "Arunkumar",
                    subtitle = "Developer",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://github.com/arunkumar9t2")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Legal Section
            item {
                SectionHeader("Legal")
            }

            item {
                AboutItem(
                    icon = Icons.Default.Article,
                    title = "Open Source Licenses",
                    subtitle = "Third-party libraries",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://htmlpreview.github.io/?https://github.com/arunkumar9t2/lynket-browser/blob/main/notices.html")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Lynket Browser",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fast, customizable browser for Android",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Copyright © 2025 Arunkumar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun AboutItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
