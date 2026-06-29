/*
 * Lynket
 *
 * Copyright (C) 2024 Arunkumar
 *
 * Utilities for working with Android Lollipop+ document tasks
 */
package arun.com.chromer.tabs

import android.app.ActivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import timber.log.Timber

/**
 * Utilities for working with document-based tasks (Android Lollipop+)
 */
object DocumentUtils {
    /**
     * Safely get task info from an AppTask
     * Returns null if the task is no longer valid or accessible
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getTaskInfoFromTask(task: ActivityManager.AppTask): ActivityManager.RecentTaskInfo? {
        return try {
            task.taskInfo
        } catch (e: IllegalArgumentException) {
            // Task may have been removed
            Timber.d(e, "Failed to get task info")
            null
        } catch (e: SecurityException) {
            // App may not have permission to access this task
            Timber.d(e, "Security exception getting task info")
            null
        }
    }

    /**
     * Check if a task is still valid
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun isTaskValid(task: ActivityManager.AppTask): Boolean {
        return getTaskInfoFromTask(task) != null
    }
}
