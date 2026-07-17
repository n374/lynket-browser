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
import android.net.Uri
import android.webkit.WebResourceRequest
import arun.com.chromer.LynketTestApplication
import arun.com.chromer.browsing.webview.ExternalAppLinkResolver.ExternalAppLink
import arun.com.chromer.settings.Preferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * RAS-58 · Both shouldOverrideUrlLoading overloads must behave identically for the same URL
 * (design §1 — cross-path divergence between API 23 String path and API 24+ request path is a
 * correctness defect).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], application = LynketTestApplication::class)
class ExternalAppInterceptingWebViewClientTest {

  private lateinit var client: ExternalAppInterceptingWebViewClient
  private lateinit var handler: SilentDialogHandler

  /** Suppresses the real dialog/toast so the client can be exercised headlessly. */
  private class SilentDialogHandler(
    activity: Activity,
    preferences: Preferences
  ) : ExternalAppLaunchHandler(activity, preferences) {
    override fun showConfirmationDialog(link: ExternalAppLink) = Unit
    override fun showToast(message: Int) = Unit
  }

  @Before
  fun setUp() {
    val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    handler = SilentDialogHandler(activity, Preferences(activity))
    client = ExternalAppInterceptingWebViewClient(handler)
  }

  private fun request(url: String) = object : WebResourceRequest {
    override fun getUrl(): Uri = Uri.parse(url)
    override fun isForMainFrame() = true
    override fun isRedirect() = false
    override fun hasGesture() = false
    override fun getMethod() = "GET"
    override fun getRequestHeaders(): Map<String, String> = emptyMap()
  }

  @Test
  fun `string and request overloads agree for every url class`() {
    val cases = mapOf(
      "https://example.com" to false,
      "http://example.com" to false,
      "about:blank" to false,
      "weixin://dl/business" to true,
      "market://details?id=x" to true,
      "intent://pay/#Intent;scheme=alipays;package=com.x;end" to true,
      "intent://broken#Intent;package=com.x" to true // parse failure still intercepted (AC-1)
    )
    cases.forEach { (url, expected) ->
      @Suppress("DEPRECATION")
      val stringResult = client.shouldOverrideUrlLoading(null, url)
      // Reset page state so the dedup set can't couple the two invocations.
      client.onPageStarted(null, "https://example.com", null)
      val requestResult = client.shouldOverrideUrlLoading(null, request(url))
      client.onPageStarted(null, "https://example.com", null)

      assertEquals("String overload wrong for $url", expected, stringResult)
      assertEquals("overloads diverge for $url", stringResult, requestResult)
    }
  }
}
