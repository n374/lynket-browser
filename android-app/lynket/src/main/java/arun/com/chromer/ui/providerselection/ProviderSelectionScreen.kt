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

package arun.com.chromer.ui.providerselection

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import arun.com.chromer.browsing.providerselection.ModernProviderSelectionViewModel
import arun.com.chromer.browsing.providerselection.ModernProviderSelectionViewModel.ProviderSelectionUiState
import arun.com.chromer.data.apps.model.Provider
import coil.compose.AsyncImage

/**
 * Phase 3.3: ProviderSelectionScreen
 *
 * Modern Compose UI for selecting Custom Tab provider.
 *
 * Features:
 * - Grid of available Custom Tab providers
 * - WebView fallback option
 * - Install providers via Play Store
 * - Shows selected provider
 * - Confirmation for WebView selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSelectionScreen(
    navController: NavController,
    viewModel: ModernProviderSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showWebViewDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Browser") },
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
        when (val state = uiState) {
            is ProviderSelectionUiState.Loading -> {
                LoadingState()
            }
            is ProviderSelectionUiState.Success -> {
                ProviderSelectionContent(
                    providers = state.providers,
                    selectedPackage = state.selectedPackage,
                    usingWebView = state.usingWebView,
                    onProviderClick = { provider ->
                        if (provider.installed) {
                            viewModel.selectProvider(provider)
                            navController.popBackStack()
                        }
                    },
                    onProviderInstallClick = { provider ->
                        // Open Play Store for non-installed providers
                        // (handled in provider grid item)
                    },
                    onWebViewClick = {
                        showWebViewDialog = true
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            is ProviderSelectionUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = viewModel::refresh
                )
            }
        }

        // WebView confirmation dialog
        if (showWebViewDialog) {
            WebViewConfirmationDialog(
                onConfirm = {
                    viewModel.selectWebView()
                    showWebViewDialog = false
                    navController.popBackStack()
                },
                onDismiss = {
                    showWebViewDialog = false
                }
            )
        }
    }
}

@Composable
private fun ProviderSelectionContent(
    providers: List<Provider>,
    selectedPackage: String?,
    usingWebView: Boolean,
    onProviderClick: (Provider) -> Unit,
    onProviderInstallClick: (Provider) -> Unit,
    onWebViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // WebView option card
        WebViewCard(
            selected = usingWebView,
            onClick = onWebViewClick
        )

        // Divider
        HorizontalDivider()

        // Custom Tab providers section
        Text(
            text = "Custom Tab Providers",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (providers.isEmpty()) {
            EmptyProvidersState()
        } else {
            // Provider grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(providers, key = { it.packageName }) { provider ->
                    ProviderGridItem(
                        provider = provider,
                        isSelected = !usingWebView && provider.packageName == selectedPackage,
                        onClick = {
                            if (provider.installed) {
                                onProviderClick(provider)
                            } else {
                                onProviderInstallClick(provider)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WebViewCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Web,
                contentDescription = "WebView",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System WebView",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Fallback option, may have limited features",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ProviderGridItem(
    provider: Provider,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable {
                if (!provider.installed) {
                    // Open Play Store
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("market://details?id=${provider.packageName}")
                        setPackage("com.android.vending")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to browser
                        val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://play.google.com/store/apps/details?id=${provider.packageName}")
                        }
                        context.startActivity(browserIntent)
                    }
                } else {
                    onClick()
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                !provider.installed -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App icon
            AsyncImage(
                model = provider.iconUri,
                contentDescription = provider.appName,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // App name
            Text(
                text = provider.appName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (provider.installed)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Selected indicator
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Install indicator
            if (!provider.installed) {
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Install",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WebViewConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use WebView?") },
        text = {
            Text(
                "WebView is a fallback option and may have limited features compared to Custom Tabs:\n\n" +
                        "• No tab sharing with browser\n" +
                        "• No password autofill\n" +
                        "• No payment integration\n" +
                        "• Slower page loading\n\n" +
                        "Are you sure you want to use WebView?"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes, Use WebView")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmptyProvidersState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No Custom Tab providers found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Install a browser like Chrome to use Custom Tabs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Error loading providers",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
