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
import android.webkit.WebView
import arun.com.chromer.LynketTestApplication
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Ported from master (WebViewConfiguratorTest), adapted to spike's Robolectric 4.3 (sdk 21).
 *
 * Verifies [WebViewConfigurator.configure] enables both JavaScript and DOM Storage. With DOM
 * Storage disabled (the WebView default), window.localStorage is null and JS-async pages (e.g.
 * jandan's comment widget) throw and show "加载失败". See WebViewConfigurator kdoc.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = LynketTestApplication::class)
class WebViewConfiguratorTest {

  @Test
  fun configure_enables_javascript_and_dom_storage() {
    val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    val settings = WebView(activity).settings

    WebViewConfigurator.configure(settings)

    assertTrue("JavaScript must be enabled for JS-driven pages", settings.javaScriptEnabled)
    assertTrue(
      "DOM Storage must be enabled, otherwise window.localStorage is null and pages " +
        "(e.g. jandan comments) throw and show 加载失败",
      settings.domStorageEnabled
    )
  }
}
