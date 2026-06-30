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

package arun.com.chromer.browsing.webview

import android.content.Intent
import android.net.Uri
import arun.com.chromer.LynketRobolectricSuite
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * Regression test for WebViewActivity crashing on launch with
 * `UninitializedPropertyAccessException: lateinit property binding`. BrowsingActivity.onCreate()
 * (super) starts observing ViewModel LiveData that deliver synchronously, invoking
 * onWebsiteLoaded()/onToolbarColorSet() (which touch `binding`) before onCreate inflates it. The fix
 * inflates `binding` before super.onCreate(); building the Activity through onCreate must not throw.
 */
@Config(sdk = [34])
class WebViewActivityLaunchTest : LynketRobolectricSuite() {

  @Test
  fun onCreate_completesWithoutBindingCrash() {
    val intent = Intent(application, WebViewActivity::class.java).apply {
      data = Uri.parse("https://jandan.net/t/6169986")
    }

    val activity = Robolectric.buildActivity(WebViewActivity::class.java, intent).create().get()

    assertNotNull(activity)
    assertFalse("WebViewActivity must not finish itself on launch", activity.isFinishing)
  }
}
