/*
 *
 *  Lynket
 *
 *  Copyright (C) 2022 Arunkumar
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

package arun.com.chromer.browsing.browserintercept

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import arun.com.chromer.R
import arun.com.chromer.bubbles.system.BubbleLoadData
import arun.com.chromer.bubbles.system.BubbleNotificationManager
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.di.activity.ActivityComponent
import arun.com.chromer.extenstions.finishAndRemoveTaskCompat
import arun.com.chromer.settings.RxPreferences
import arun.com.chromer.shared.base.activity.BaseActivity
import arun.com.chromer.tabs.TabsManager
import arun.com.chromer.util.SafeIntent
import io.reactivex.rxkotlin.subscribeBy
import java.lang.ref.WeakReference
import javax.inject.Inject

@SuppressLint("GoogleAppIndexingApiWarning")
class BrowserInterceptActivity : BaseActivity() {
  @Inject
  lateinit var defaultTabsManager: TabsManager

  @Inject
  lateinit var rxPreferences: RxPreferences

  @Inject
  lateinit var bubbleNotificationManager: BubbleNotificationManager

  override val layoutRes: Int get() = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intent?.let {
      val safeIntent = SafeIntent(intent)
      val url = safeIntent.dataString
      if (safeIntent.data == null || url == null) {
        invalidLink()
        return
      }

      // Spike RAS-38: the platform only honors BubbleMetadata.setAutoExpandBubble(true)
      // when the posting app is in the *foreground* at post time. This translucent,
      // excluded-from-recents activity is exactly that foreground moment. So for the
      // native-bubble path we post the bubble synchronously here on the main thread while
      // this activity is resumed, then delay finish() briefly so the platform registers
      // the foreground-posted, auto-expanding bubble before we vanish. Without this the
      // normal flow posts the bubble from a background pool thread *after* this activity
      // has finished, and the platform silently downgrades it to a collapsed bubble.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && rxPreferences.nativeBubbles.get()) {
        postForegroundBubble(url)
        return
      }

      defaultTabsManager.processIncomingIntent(this, intent)
        .subscribeBy(onComplete = { finishAndRemoveTaskCompat() })
    } ?: run {
      finishAndRemoveTaskCompat()
    }
  }

  /**
   * Spike RAS-38: post the bubble inline (on the foreground main thread) and keep this
   * invisible host visible just long enough for the auto-expand to take effect, then remove
   * the task so the user is left looking at whatever was behind us with the bubble floating
   * on top.
   */
  private fun postForegroundBubble(url: String) {
    val website = Website(url)
    bubbleNotificationManager
      .showBubbles(BubbleLoadData(website = website, fromMinimize = false, fromAmp = false, incognito = false, contextRef = WeakReference(this)))
      .subscribeBy(onError = {}, onSuccess = {})
    // Give the platform a beat to inflate + auto-expand the bubble while we are still the
    // foreground (importance FOREGROUND) app, then disappear.
    window.decorView.postDelayed({ finishAndRemoveTaskCompat() }, 800)
  }

  override fun inject(activityComponent: ActivityComponent) {
    activityComponent.inject(this)
  }

  private fun invalidLink() {
    Toast.makeText(this, getString(R.string.unsupported_link), LENGTH_SHORT).show()
    finishAndRemoveTaskCompat()
  }
}
