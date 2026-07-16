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

package arun.com.chromer.bubbles.system

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import arun.com.chromer.BuildConfig
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.di.activity.ActivityComponent
import arun.com.chromer.shared.base.activity.BaseActivity
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * RAS-55 · SPIKE-ONLY 取证入口（design §5.4）。
 *
 * 存在理由：**不能用 `openWebHeads` 常规链路触发气泡**——`DefaultTabsManager.openWebHeads` 在弹出 native
 * bubble 之后，若 `aggressiveLoading && !fromMinimize && !shouldUseWebView`，会**另起**一个正常 CCT /
 * 全屏 tab，令 dumpsys / logcat / 探针令牌可能来自那个正常 CCT 而非气泡内 CCT，直接污染实验（假阳/假阴）。
 *
 * 本 Activity 绕开该副作用：直调 [BubbleNotificationManager.showBubbles]，只弹气泡、不做 aggressive-loading。
 * 供取证脚本 `adb am start` 触发：
 *
 * ```
 * adb shell am start -n arun.com.chromer/.bubbles.system.BubbleSpikeTriggerActivity \
 *   -e url "http://10.0.2.2:8000/login-state-probe.html" -e target experiment
 * ```
 *
 * - `url`：气泡打开的目标页（默认 [DEFAULT_PROBE_URL]）。
 * - `target`：`experiment`（外部浏览器 CCT 薄壳，默认）或 `control`（内置 WebView 对照组），同机 A/B。
 *
 * 仅 DEBUG 构建生效（release 直接 finish），并要求 API ≥ Q（native bubbles 前提）。不随产品发布。
 */
@RequiresApi(Build.VERSION_CODES.Q)
class BubbleSpikeTriggerActivity : BaseActivity() {

  @Inject
  lateinit var bubbleNotificationManager: BubbleNotificationManager

  override fun onCreate(savedInstanceState: Bundle?) {
    // BaseActivity.onCreate 完成 DI 注入（inject(this)），故 super 之后 bubbleNotificationManager 可用。
    super.onCreate(savedInstanceState)

    if (!BuildConfig.DEBUG) {
      Timber.w("[BUBBLE_SPIKE] trigger is DEBUG-only, ignoring on non-debug build")
      finish()
      return
    }

    val url = intent?.getStringExtra(EXTRA_URL)
      ?: intent?.dataString
      ?: DEFAULT_PROBE_URL
    // target=control → 对照组(内置 WebView)；其余(含缺省) → 实验组(外部浏览器 CCT 薄壳)。
    val useCctShell = !EXTRA_TARGET_CONTROL.equals(intent?.getStringExtra(EXTRA_TARGET), ignoreCase = true)

    Timber.d("[BUBBLE_SPIKE] trigger showBubbles url=%s useCctShell=%s", url, useCctShell)

    bubbleNotificationManager
      .showBubbles(
        BubbleLoadData(
          website = Website(url),
          fromMinimize = false,
          fromAmp = false,
          incognito = false,
          contextRef = WeakReference(applicationContext),
          useCctShell = useCctShell
        )
      )
      .subscribeOn(Schedulers.io())
      .subscribe(
        { Timber.d("[BUBBLE_SPIKE] bubble posted for %s", url) },
        { Timber.e(it, "[BUBBLE_SPIKE] failed to post bubble") }
      )

    // 立即 finish 触发者本身：它只负责发气泡，不参与取证栈。
    finish()
  }

  override fun inject(activityComponent: ActivityComponent) {
    activityComponent.inject(this)
  }

  override val layoutRes: Int get() = 0

  companion object {
    private const val EXTRA_URL = "url"
    private const val EXTRA_TARGET = "target"
    private const val EXTRA_TARGET_CONTROL = "control"
    private const val DEFAULT_PROBE_URL = "http://10.0.2.2:8000/login-state-probe.html"
  }
}
