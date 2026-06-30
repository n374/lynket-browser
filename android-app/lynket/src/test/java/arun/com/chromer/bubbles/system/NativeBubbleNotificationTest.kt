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

package arun.com.chromer.bubbles.system

import android.app.NotificationManager
import androidx.core.content.pm.ShortcutManagerCompat
import arun.com.chromer.LynketRobolectricSuite
import arun.com.chromer.data.website.model.Website
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Regression test for native bubbles not appearing on Android 11+ (API 30+).
 *
 * From Android 11 a notification only surfaces as a bubble when it references a *published,
 * long-lived* sharing shortcut (a conversation). The original code built bubble metadata without
 * publishing such a shortcut / setting a shortcut id, so the platform silently downgraded the
 * bubble to a normal (suppressed) notification and nothing showed. This verifies that
 * [BubbleNotificationManager.showBubbles]:
 *  - publishes a long-lived dynamic shortcut for the URL, and
 *  - posts a notification that references that shortcut and carries bubble metadata.
 */
@Config(sdk = [34])
class NativeBubbleNotificationTest : LynketRobolectricSuite() {

  @Test
  fun showBubbles_publishesLongLivedShortcut_andPostsBubbleNotification() {
    val url = "https://jandan.net/t/6169986"
    val manager = BubbleNotificationManager(application)

    manager.showBubbles(
      BubbleLoadData(
        website = Website(url),
        fromMinimize = false,
        fromAmp = false,
        incognito = false
      )
    ).blockingGet()

    val shortcut = ShortcutManagerCompat
      .getShortcuts(application, ShortcutManagerCompat.FLAG_MATCH_DYNAMIC)
      .find { it.id == url }
    assertNotNull("a dynamic conversation shortcut for the URL must be published", shortcut)

    val notificationManager = application.getSystemService(NotificationManager::class.java)
    val bubbleNotification = shadowOf(notificationManager).allNotifications
      .firstOrNull { it.bubbleMetadata != null }
    assertNotNull("a notification with bubble metadata must be posted", bubbleNotification)
    assertEquals(
      "the bubble notification must reference the shortcut (Android 11+ conversation rule)",
      url,
      bubbleNotification!!.shortcutId
    )
  }
}
