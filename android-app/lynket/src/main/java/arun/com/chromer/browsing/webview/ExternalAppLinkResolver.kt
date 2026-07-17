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
import android.content.pm.PackageManager
import android.net.Uri
import timber.log.Timber
import java.net.URISyntaxException
import java.util.Locale

/**
 * Classifies WebView navigations and resolves external-app links (non http(s) schemes,
 * `intent://`) into a hardened, launchable [Intent]. Pure logic, no UI — see
 * docs/changes/58-external-app-launch/design.md.
 *
 * Correctness note (design §3): under targetSdk 35 package-visibility filtering,
 * PackageManager pre-resolution of arbitrary custom schemes is UNRELIABLE — an installed app
 * may still resolve to nothing. Pre-resolution here is best-effort for the dialog label ONLY;
 * it must never gate "installed / can open" decisions. The single authoritative signal is
 * `startActivity` + catching `ActivityNotFoundException` (handled in [ExternalAppLaunchHandler]).
 */
class ExternalAppLinkResolver {

  sealed class Result {
    /** http(s) or a WebView-internal scheme — let the WebView load it as today (AC-2). */
    object PassThrough : Result()

    /** External-app link successfully parsed and hardened. */
    data class External(val link: ExternalAppLink) : Result()

    /** Non http(s) link that failed to parse — must still be intercepted, never fed back to the WebView (AC-1). */
    object ParseFailure : Result()
  }

  /**
   * @param intent hardened intent ready for `startActivity`
   * @param allowKey "remember my choice" key: `package:<pkg>` when the target package is known,
   *   else `scheme:<scheme>` (design §5)
   * @param displayLabel best-effort target description for the dialog — app label when
   *   resolvable, else package name or scheme. Never authoritative.
   */
  data class ExternalAppLink(
    val intent: Intent,
    val allowKey: String,
    val displayLabel: String
  )

  fun resolve(url: String, packageManager: PackageManager?): Result {
    val scheme = Uri.parse(url).scheme?.lowercase(Locale.ROOT)
    if (scheme == null || scheme in WEB_VIEW_SCHEMES) {
      return Result.PassThrough
    }
    val intent = try {
      Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
    } catch (e: URISyntaxException) {
      Timber.e(e, "Failed to parse external app link")
      return Result.ParseFailure
    }
    harden(intent)
    val targetPackage = intent.`package`
    val allowKey = if (targetPackage != null) {
      "package:$targetPackage"
    } else {
      "scheme:${(intent.data?.scheme ?: scheme).lowercase(Locale.ROOT)}"
    }
    return Result.External(
      ExternalAppLink(
        intent = intent,
        allowKey = allowKey,
        displayLabel = bestEffortLabel(intent, packageManager) ?: targetPackage ?: scheme
      )
    )
  }

  /**
   * Hardening against web pages abusing `intent://` for privilege escalation, aligned with
   * Chromium's ExternalNavigationHandler conventions (design §2).
   */
  private fun harden(intent: Intent) {
    intent.addCategory(Intent.CATEGORY_BROWSABLE)
    intent.component = null
    intent.selector = null
    intent.flags = intent.flags and (
      Intent.FLAG_GRANT_READ_URI_PERMISSION
        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
      ).inv()
  }

  /**
   * Best-effort app label for the confirmation dialog. A `null` here means nothing about
   * whether the app is installed (package visibility) — callers must not gate on it.
   */
  private fun bestEffortLabel(intent: Intent, packageManager: PackageManager?): String? {
    packageManager ?: return null
    return try {
      val resolveInfo = packageManager.resolveActivity(intent, 0) ?: return null
      // "android" is the system resolver, not the actual target app.
      if (resolveInfo.activityInfo?.packageName == "android") return null
      resolveInfo.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
      Timber.e(e, "Best-effort label resolution failed")
      null
    }
  }

  companion object {
    /**
     * Schemes the WebView handles itself — pass through unchanged (AC-2 + design §1).
     * Everything else is treated as an external-app link. content:// is included: WebView
     * loads it natively (allowContentAccess), and Chromium's ExternalNavigationHandler
     * never externalizes it either.
     */
    private val WEB_VIEW_SCHEMES = setOf(
      "http", "https", "about", "data", "blob", "file", "javascript", "content"
    )
  }
}
