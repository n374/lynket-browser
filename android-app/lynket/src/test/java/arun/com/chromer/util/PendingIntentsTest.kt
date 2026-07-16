/*
 *
 *  Lynket
 *
 *  Copyright (C) 2026 Arunkumar
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package arun.com.chromer.util

import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.os.Build
import arun.com.chromer.LynketTestApplication
import arun.com.chromer.browsing.customtabs.bottombar.BottomBarManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Layer-2 behavior test for [PendingIntents] and the one directly-callable creation point
 * (`BottomBarManager.getOnClickPendingIntent`). See RAS-54 design §6.
 *
 * Robolectric 4.3 caps at SDK 28, so the API-31 branch of [PendingIntents.mutable] is exercised by
 * overriding `Build.VERSION.SDK_INT` via reflection — the only way to assert the API-31 behaviour
 * without upgrading Robolectric. CustomTabs (private methods) and WebHeadService (foreground
 * service) are covered by the layer-1 source guard instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], application = LynketTestApplication::class)
class PendingIntentsTest {

  private fun withSdkInt(value: Int, block: () -> Unit) {
    val original = Build.VERSION.SDK_INT
    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", value)
    try {
      block()
    } finally {
      ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", original)
    }
  }

  @Test
  fun immutable_alwaysAddsImmutableFlag_andKeepsBase() {
    val flags = PendingIntents.immutable(FLAG_UPDATE_CURRENT)
    assertTrue("immutable() must set FLAG_IMMUTABLE", flags and FLAG_IMMUTABLE != 0)
    assertTrue("immutable() must preserve the base flag", flags and FLAG_UPDATE_CURRENT != 0)
  }

  @Test
  fun mutable_addsMutableFlag_onApi31() {
    withSdkInt(Build.VERSION_CODES.S) {
      val flags = PendingIntents.mutable(FLAG_UPDATE_CURRENT)
      assertTrue("mutable() must set FLAG_MUTABLE on API 31+", flags and FLAG_MUTABLE != 0)
      assertTrue("mutable() must NOT set FLAG_IMMUTABLE", flags and FLAG_IMMUTABLE == 0)
      assertTrue("mutable() must preserve the base flag", flags and FLAG_UPDATE_CURRENT != 0)
    }
  }

  @Test
  fun mutable_isNoOp_below31() {
    withSdkInt(Build.VERSION_CODES.M) {
      // Pre-Android-12 a PendingIntent is implicitly mutable, so no flag is added — behaviour is
      // identical to the original code and must never carry FLAG_IMMUTABLE.
      val flags = PendingIntents.mutable(FLAG_UPDATE_CURRENT)
      assertEquals("mutable() must be a no-op below API 31", FLAG_UPDATE_CURRENT, flags)
      assertTrue("mutable() must never be immutable", flags and FLAG_IMMUTABLE == 0)
    }
  }

  @Test
  fun bottomBarOnClickPendingIntent_isNotImmutable() {
    val activity: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    val pendingIntent: PendingIntent =
      BottomBarManager.getOnClickPendingIntent(activity, "https://example.com")
    // The bottom bar relies on the browser filling clicked-id + URL, so this point must stay
    // mutable — assert it is never immutable (the failure mode that silently kills the bottom bar).
    val flags = shadowOf(pendingIntent).flags
    assertTrue("BottomBar PendingIntent must not be immutable", flags and FLAG_IMMUTABLE == 0)
  }
}
