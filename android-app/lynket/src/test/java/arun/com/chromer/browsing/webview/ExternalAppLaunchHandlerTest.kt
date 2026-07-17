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

package arun.com.chromer.browsing.webview

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import arun.com.chromer.LynketTestApplication
import arun.com.chromer.R
import arun.com.chromer.browsing.webview.ExternalAppLinkResolver.ExternalAppLink
import arun.com.chromer.settings.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * RAS-58 · Handler orchestration: remembered→direct launch (AC-6), unremembered→dialog (AC-3),
 * same-page dedup (design §4), fail-loud launch errors (AC-7 — the correctness red line:
 * exception paths are mandatory tests, happy-path green alone is NOT a pass).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], application = LynketTestApplication::class)
class ExternalAppLaunchHandlerTest {

  private lateinit var activity: Activity
  private lateinit var preferences: Preferences

  private val launched = mutableListOf<Intent>()

  /** Test double: records dialogs/toasts instead of touching real windows. */
  private inner class RecordingHandler(
    starter: (Intent) -> Unit = { launched += it }
  ) : ExternalAppLaunchHandler(activity, preferences, ExternalAppLinkResolver(), starter) {
    val dialogsShown = mutableListOf<ExternalAppLink>()
    val toasts = mutableListOf<Int>()

    public override fun showConfirmationDialog(link: ExternalAppLink) {
      dialogsShown += link
    }

    public override fun showToast(message: Int) {
      toasts += message
    }
  }

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    preferences = Preferences(activity)
    preferences.clearExternalAppLaunchChoices()
    launched.clear()
  }

  @Test
  fun `http url is not intercepted`() {
    val handler = RecordingHandler()
    assertFalse(handler.shouldOverrideUrlLoading("https://example.com"))
    assertTrue(handler.dialogsShown.isEmpty())
    assertTrue(launched.isEmpty())
  }

  @Test
  fun `null url is not intercepted`() {
    assertFalse(RecordingHandler().shouldOverrideUrlLoading(null))
  }

  @Test
  fun `unremembered external link shows dialog and does not launch (AC-3, AC-5)`() {
    val handler = RecordingHandler()
    assertTrue(handler.shouldOverrideUrlLoading("weixin://dl/business"))
    assertEquals(1, handler.dialogsShown.size)
    // No confirmation given (= user cancelled) → nothing may launch.
    assertTrue(launched.isEmpty())
  }

  @Test
  fun `remembered allow key launches directly without dialog (AC-6)`() {
    preferences.rememberExternalAppLaunch("scheme:weixin")
    val handler = RecordingHandler()
    assertTrue(handler.shouldOverrideUrlLoading("weixin://dl/business"))
    assertTrue(handler.dialogsShown.isEmpty())
    assertEquals(1, launched.size)
  }

  @Test
  fun `same allow key does not re-prompt while a prompt is pending (design §4)`() {
    val handler = RecordingHandler()
    handler.shouldOverrideUrlLoading("weixin://dl/a")
    handler.shouldOverrideUrlLoading("weixin://dl/b")
    assertEquals("redirect bombardment must not stack dialogs", 1, handler.dialogsShown.size)

    handler.onPageStarted()
    handler.shouldOverrideUrlLoading("weixin://dl/c")
    assertEquals("new page load must prompt again", 2, handler.dialogsShown.size)
  }

  @Test
  fun `deliberate re-click after cancel prompts again (AC-3 AC-5, cross-review fix)`() {
    val handler = RecordingHandler()
    handler.shouldOverrideUrlLoading("weixin://dl/a")
    val link = handler.dialogsShown.single()

    // User cancels (dialog dismissed without confirmation)…
    handler.onPromptDismissed(link)
    // …then deliberately clicks the same link again: it MUST prompt again, never be
    // silently swallowed.
    handler.shouldOverrideUrlLoading("weixin://dl/a")

    assertEquals(2, handler.dialogsShown.size)
    assertTrue(launched.isEmpty())
  }

  @Test
  fun `confirming with remember persists choice and launches (AC-4, AC-6)`() {
    val handler = RecordingHandler()
    handler.shouldOverrideUrlLoading("weixin://dl/business")
    val link = handler.dialogsShown.single()

    handler.onLaunchConfirmed(link, remember = true)

    assertEquals(1, launched.size)
    assertTrue(preferences.isExternalAppLaunchAllowed("scheme:weixin"))
  }

  @Test
  fun `confirming without remember launches but persists nothing`() {
    val handler = RecordingHandler()
    handler.shouldOverrideUrlLoading("weixin://dl/business")

    handler.onLaunchConfirmed(handler.dialogsShown.single(), remember = false)

    assertEquals(1, launched.size)
    assertFalse(preferences.isExternalAppLaunchAllowed("scheme:weixin"))
  }

  @Test
  fun `ActivityNotFoundException fails loud - toast, no crash (AC-7)`() {
    val handler = RecordingHandler(starter = { throw ActivityNotFoundException("not installed") })
    preferences.rememberExternalAppLaunch("scheme:weixin")

    assertTrue(handler.shouldOverrideUrlLoading("weixin://dl/business"))

    assertEquals(listOf(R.string.external_app_not_found), handler.toasts)
  }

  @Test
  fun `SecurityException fails loud - toast, no crash (AC-7)`() {
    val handler = RecordingHandler(starter = { throw SecurityException("protected component") })
    preferences.rememberExternalAppLaunch("scheme:weixin")

    assertTrue(handler.shouldOverrideUrlLoading("weixin://dl/business"))

    assertEquals(listOf(R.string.external_app_not_found), handler.toasts)
  }

  @Test
  fun `parse failure is intercepted with feedback, never fed back to webview (AC-1)`() {
    val handler = RecordingHandler()
    assertTrue(handler.shouldOverrideUrlLoading("intent://foo#Intent;package=com.x"))
    assertEquals(listOf(R.string.external_app_link_unparsable), handler.toasts)
    assertTrue(handler.dialogsShown.isEmpty())
    assertTrue(launched.isEmpty())
  }

  @Test
  fun `no dialog after activity is finishing (lifecycle guard)`() {
    val handler = RecordingHandler()
    activity.finish()
    assertTrue(handler.shouldOverrideUrlLoading("weixin://dl/business"))
    assertTrue(handler.dialogsShown.isEmpty())
    assertTrue(launched.isEmpty())
  }
}
