/*
 * Lynket
 *
 * Copyright (C) 2024 Arunkumar
 *
 * Centralized service lifecycle management
 */
package arun.com.chromer.shared

import android.content.Context
import android.content.Intent
import android.os.Build
import arun.com.chromer.appdetect.AppDetectService
import timber.log.Timber

/**
 * Manages app services - starting, stopping, and lifecycle
 */
object ServiceManager {
    /**
     * Start the app detection service
     * Handles different Android versions appropriately
     */
    fun startAppDetectionService(context: Context) {
        try {
            val intent = Intent(context, AppDetectService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android O+ requires foreground services for background apps
                try {
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to start foreground service, trying regular start")
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start app detection service")
        }
    }

    /**
     * Stop the app detection service
     */
    fun stopAppDetectionService(context: Context) {
        try {
            val intent = Intent(context, AppDetectService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop app detection service")
        }
    }
}
