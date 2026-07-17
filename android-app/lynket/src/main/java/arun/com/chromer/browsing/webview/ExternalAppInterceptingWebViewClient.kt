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

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebViewClient that routes navigations through [ExternalAppLaunchHandler] (RAS-58).
 *
 * Both overloads MUST stay behaviorally identical (design §1): minSdk 23 only ever calls the
 * deprecated String version, API 24+ calls the [WebResourceRequest] one — divergence would be
 * a cross-path correctness defect. Deliberately NO `isForMainFrame` short-circuit: non-http(s)
 * subframe navigations (e.g. payment iframes) must be intercepted too, and the String overload
 * has no frame info to mirror such a filter.
 */
open class ExternalAppInterceptingWebViewClient(
  private val externalAppLaunchHandler: ExternalAppLaunchHandler
) : WebViewClient() {

  @Deprecated("Deprecated in Java")
  @Suppress("OverridingDeprecatedMember")
  override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
    externalAppLaunchHandler.shouldOverrideUrlLoading(url)

  override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
    externalAppLaunchHandler.shouldOverrideUrlLoading(request?.url?.toString())

  override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
    super.onPageStarted(view, url, favicon)
    // Main-frame page start defines the "page" for dialog dedup (design §4).
    externalAppLaunchHandler.onPageStarted()
  }
}
