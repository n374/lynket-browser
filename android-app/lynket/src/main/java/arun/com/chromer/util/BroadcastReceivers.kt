/*
 * Lynket
 *
 * Copyright (C) 2026 Arunkumar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package arun.com.chromer.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * Helpers for registering [BroadcastReceiver]s.
 *
 * On Android 13+ (API 33+), [Context.registerReceiver] for non-system broadcasts must explicitly
 * declare whether the receiver is exported, or it throws `SecurityException`. Under targetSdk 35
 * this is enforced and previously crashed services that registered app-internal receivers without
 * the flag (see WebHeadService / Bubble mode).
 */
object BroadcastReceivers {

  /**
   * Registers a receiver for app-internal (non-system) broadcasts with the required
   * not-exported flag, so no other app can deliver these broadcasts to us. Use this for receivers
   * that only handle the app's own actions (e.g. PendingIntents fired by our own notifications).
   */
  @JvmStatic
  fun registerNotExported(context: Context, receiver: BroadcastReceiver, filter: IntentFilter) {
    ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
  }
}
