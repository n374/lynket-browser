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

import android.webkit.WebSettings

/**
 * Central place to configure the [WebSettings] used by Lynket's in-app WebView browsing
 * ([WebViewActivity]). Ported from master commit d3996723.
 */
object WebViewConfigurator {

  /**
   * Applies the minimum settings required to render modern, JS-driven pages correctly.
   *
   * - [WebSettings.setJavaScriptEnabled]: pages are JS-driven.
   * - [WebSettings.setDomStorageEnabled]: **root-cause fix.** When DOM Storage is disabled
   *   (the Android WebView default), `window.localStorage` / `sessionStorage` is reported as
   *   `null` to page scripts, so any access throws `TypeError: Cannot read properties of null`.
   *   Many sites that lazy-load content via JS touch `localStorage` while rendering — e.g.
   *   jandan's comment widget calls `localStorage.getItem(...)` in its `$idIsBlocked` check,
   *   the exception aborts the jQuery Deferred chain, and the comment area shows
   *   "呃...加载失败，点击重新加载" even though the comment API itself returned HTTP 200.
   *   Enabling DOM Storage (available since API 1, well below our minSdk 23) fixes this and the
   *   same class of JS-async content failures in WebView mode.
   */
  fun configure(settings: WebSettings) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
  }
}
