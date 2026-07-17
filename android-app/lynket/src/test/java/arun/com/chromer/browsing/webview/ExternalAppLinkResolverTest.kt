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

import android.content.Intent
import arun.com.chromer.LynketTestApplication
import arun.com.chromer.browsing.webview.ExternalAppLinkResolver.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * RAS-58 · AC-1/AC-2 classification + intent hardening + allow-key rules.
 * See docs/changes/58-external-app-launch/design.md §2/§3/§5.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], application = LynketTestApplication::class)
class ExternalAppLinkResolverTest {

  private val resolver = ExternalAppLinkResolver()

  // --- AC-2: http(s) and WebView-internal schemes pass through untouched ---

  @Test
  fun `http, https and webview internal schemes pass through`() {
    listOf(
      "http://example.com",
      "https://example.com/path?q=1",
      "HTTPS://UPPERCASE.example.com",
      "about:blank",
      "data:text/html,<p>hi</p>",
      "blob:https://example.com/uuid",
      "file:///sdcard/x.html",
      "javascript:void(0)",
      "content://media/external/images/1"
    ).forEach { url ->
      assertTrue(
        "$url must pass through to the WebView",
        resolver.resolve(url, null) is Result.PassThrough
      )
    }
  }

  // --- AC-1: non http(s) schemes are classified as external app links ---

  @Test
  fun `custom scheme, mailto and intent urls are external`() {
    listOf(
      "weixin://dl/business/?t=abc",
      "market://details?id=com.example",
      "mailto:someone@example.com",
      "intent://pay/#Intent;scheme=alipays;end"
    ).forEach { url ->
      assertTrue("$url must be external", resolver.resolve(url, null) is Result.External)
    }
  }

  @Test
  fun `malformed intent url is a parse failure, not an exception`() {
    // Missing the mandatory ";end" token → URISyntaxException inside Intent.parseUri.
    val result = resolver.resolve("intent://foo#Intent;package=com.x", null)
    assertTrue("malformed link must map to ParseFailure", result is Result.ParseFailure)
  }

  // --- Design §2: hardening against intent:// privilege escalation ---

  @Test
  fun `explicit component and selector are stripped and BROWSABLE added`() {
    val url = "intent://host/#Intent;scheme=test;component=com.evil/.Exported;end"
    val link = (resolver.resolve(url, null) as Result.External).link
    assertNull("component must be stripped", link.intent.component)
    assertNull("selector must be stripped", link.intent.selector)
    assertTrue(
      "CATEGORY_BROWSABLE must be added",
      link.intent.hasCategory(Intent.CATEGORY_BROWSABLE)
    )
  }

  @Test
  fun `uri permission grant flags are cleared`() {
    // 0xC3 = GRANT_READ | GRANT_WRITE | GRANT_PERSISTABLE | GRANT_PREFIX ORed with noise bits.
    val url = "intent://host/#Intent;scheme=test;launchFlags=0x000000c3;end"
    val link = (resolver.resolve(url, null) as Result.External).link
    val grantMask = Intent.FLAG_GRANT_READ_URI_PERMISSION or
      Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
      Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
      Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    assertEquals("all grant flags must be cleared", 0, link.intent.flags and grantMask)
  }

  // --- Design §5: allow-key granularity — package first, scheme fallback ---

  @Test
  fun `allow key uses target package when present`() {
    val url = "intent://pay/#Intent;scheme=alipays;package=com.eg.android.AlipayGphone;end"
    val link = (resolver.resolve(url, null) as Result.External).link
    assertEquals("package:com.eg.android.AlipayGphone", link.allowKey)
  }

  @Test
  fun `allow key falls back to scheme when no package`() {
    val link = (resolver.resolve("WEIXIN://dl/business", null) as Result.External).link
    assertEquals("scheme:weixin", link.allowKey)
  }

  @Test
  fun `allow key uses parsed data scheme for intent urls without package`() {
    val url = "intent://scan/#Intent;scheme=zxing;end"
    val link = (resolver.resolve(url, null) as Result.External).link
    assertEquals("scheme:zxing", link.allowKey)
  }

  // --- Design §3: label is best-effort only, never a gate ---

  @Test
  fun `unresolvable link still resolves as external with fallback label`() {
    // No PackageManager at all — pre-resolution is impossible, yet the link MUST still be
    // offered to the user (package visibility makes pre-resolution unreliable; AC-3).
    val link = (resolver.resolve("weixin://dl/business", null) as Result.External).link
    assertEquals("weixin", link.displayLabel)
    assertFalse(link.displayLabel.isBlank())
  }

  @Test
  fun `fallback label prefers package name over scheme`() {
    val url = "intent://pay/#Intent;scheme=alipays;package=com.eg.android.AlipayGphone;end"
    val link = (resolver.resolve(url, null) as Result.External).link
    assertEquals("com.eg.android.AlipayGphone", link.displayLabel)
  }
}
