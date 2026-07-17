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
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import arun.com.chromer.R
import arun.com.chromer.browsing.webview.ExternalAppLinkResolver.ExternalAppLink
import arun.com.chromer.settings.Preferences
import com.afollestad.materialdialogs.MaterialDialog
import timber.log.Timber

/**
 * Orchestrates the external-app-launch flow for WebView navigations (RAS-58):
 * remembered choice → launch directly; otherwise ask via confirmation dialog; launch with
 * fail-loud error handling. See docs/changes/58-external-app-launch/design.md.
 *
 * Correctness (AC-7, design §6): whether the target app can actually be opened is decided by
 * `startActivity` + catch only. `ActivityNotFoundException` AND `SecurityException` (web pages
 * can craft intents at permission-protected components) both surface as a Toast — never a
 * crash, never silence.
 */
open class ExternalAppLaunchHandler(
  private val activity: Activity,
  private val preferences: Preferences,
  private val resolver: ExternalAppLinkResolver = ExternalAppLinkResolver(),
  private val activityStarter: (Intent) -> Unit = { activity.startActivity(it) }
) {
  /**
   * Allow-keys whose confirmation dialog is currently showing — suppresses dialog
   * bombardment from redirect chains while a prompt is pending (design §4, revised after
   * dev-phase cross review). Lifecycle is owned EXCLUSIVELY by the dialog: add before
   * show, remove on dismiss (any reason), roll back if the activity dies before show.
   * Deliberately NOT cleared on page navigation — a page-start clear while a dialog is
   * still up would let the same key stack a second dialog. A for-the-page-lifetime dedup
   * was also rejected: it silently swallowed deliberate re-clicks after cancel
   * (AC-3/AC-5 violation).
   */
  private val pendingPromptKeys = HashSet<String>()

  /**
   * Single entry point for both `shouldOverrideUrlLoading` overloads.
   *
   * @return false → let the WebView load the URL (http(s)/internal schemes, AC-2);
   *   true → navigation consumed (external link handled or failed loudly, AC-1 — a non-http(s)
   *   URL is never fed back to the WebView, even on parse failure).
   */
  fun shouldOverrideUrlLoading(url: String?): Boolean {
    url ?: return false
    return when (val result = resolver.resolve(url, activity.packageManager)) {
      is ExternalAppLinkResolver.Result.PassThrough -> false
      is ExternalAppLinkResolver.Result.ParseFailure -> {
        showToast(R.string.external_app_link_unparsable)
        true
      }
      is ExternalAppLinkResolver.Result.External -> {
        handleExternalLink(result.link)
        true
      }
    }
  }

  private fun handleExternalLink(link: ExternalAppLink) {
    if (preferences.isExternalAppLaunchAllowed(link.allowKey)) {
      // Remembered "allow" → skip the dialog (AC-6). Launch still goes through the guarded path.
      launchExternalApp(link.intent)
      return
    }
    if (!pendingPromptKeys.add(link.allowKey)) {
      // A prompt for this key is already showing — swallow redirect-chain re-triggers.
      return
    }
    if (activity.isFinishing || activity.isDestroyed) {
      // Too late to show a dialog — avoid WindowLeaked.
      pendingPromptKeys.remove(link.allowKey)
      return
    }
    showConfirmationDialog(link)
  }

  /**
   * MUST be called when the confirmation dialog goes away for any reason (confirm, cancel,
   * outside touch) — re-enables prompting for this key so a deliberate later click works.
   */
  internal fun onPromptDismissed(link: ExternalAppLink) {
    pendingPromptKeys.remove(link.allowKey)
  }

  /** Open for tests to observe/short-circuit the dialog without touching real windows. */
  protected open fun showConfirmationDialog(link: ExternalAppLink) {
    val content = LayoutInflater.from(activity)
      .inflate(R.layout.dialog_external_app_launch, null, false)
    content.findViewById<TextView>(R.id.externalAppLaunchMessage).text =
      activity.getString(R.string.external_app_launch_message, link.displayLabel)
    val rememberCheckBox = content.findViewById<CheckBox>(R.id.externalAppLaunchRemember)
    MaterialDialog.Builder(activity)
      .title(R.string.external_app_launch_title)
      .customView(content, false)
      .positiveText(R.string.external_app_launch_open)
      .negativeText(android.R.string.cancel)
      .onPositive { _, _ -> onLaunchConfirmed(link, rememberCheckBox.isChecked) }
      .dismissListener { onPromptDismissed(link) }
      .show()
    // Cancel: no launch — user stays on the current page (AC-5); dismiss re-arms the prompt.
  }

  /** Positive-button path: optionally persist the choice, then launch (AC-4/AC-6). */
  internal fun onLaunchConfirmed(link: ExternalAppLink, remember: Boolean) {
    if (remember) {
      preferences.rememberExternalAppLaunch(link.allowKey)
    }
    launchExternalApp(link.intent)
  }

  /**
   * The ONLY launch path — single convergence point for failure handling so a future
   * market:// / browser_fallback_url fallback needs exactly one change site (design §6).
   */
  internal fun launchExternalApp(intent: Intent) {
    try {
      activityStarter(intent)
    } catch (e: ActivityNotFoundException) {
      failLoud(e)
    } catch (e: SecurityException) {
      failLoud(e)
    }
  }

  private fun failLoud(e: Exception) {
    Timber.e(e, "Failed to launch external app")
    showToast(R.string.external_app_not_found)
  }

  /** Open for tests to capture user-facing feedback without real Toasts. */
  protected open fun showToast(@StringRes message: Int) {
    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
  }
}
