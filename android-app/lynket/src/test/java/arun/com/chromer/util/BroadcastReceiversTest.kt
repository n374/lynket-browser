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
import android.content.Intent
import android.content.IntentFilter
import arun.com.chromer.LynketRobolectricSuite
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Regression test for Bubble (web-head) mode crashing on launch. On Android 13+ (API 33+),
 * registering a receiver for app-internal broadcasts without RECEIVER_EXPORTED/NOT_EXPORTED throws
 * SecurityException — under targetSdk 35 this crashed WebHeadService.onCreate and broke Bubble mode.
 * [BroadcastReceivers.registerNotExported] must register such receivers with the not-exported flag
 * without throwing on API 34.
 */
@Config(sdk = [34])
class BroadcastReceiversTest : LynketRobolectricSuite() {

  @Test
  fun registerNotExported_registersAppInternalReceiverWithoutThrowing() {
    val context: Context = application
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(c: Context?, i: Intent?) = Unit
    }
    val action = "arun.com.chromer.test.NOT_A_SYSTEM_BROADCAST"

    // Would throw SecurityException on API 33+ if the not-exported flag were missing.
    BroadcastReceivers.registerNotExported(context, receiver, IntentFilter(action))

    assertTrue(
      "receiver must be registered for the app-internal action",
      shadowOf(application).hasReceiverForIntent(Intent(action))
    )
  }
}
