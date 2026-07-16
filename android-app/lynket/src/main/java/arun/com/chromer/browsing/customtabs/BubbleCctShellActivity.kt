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

package arun.com.chromer.browsing.customtabs

import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import arun.com.chromer.browsing.BrowsingActivity
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.di.activity.ActivityComponent
import timber.log.Timber

/**
 * RAS-55 · SPIKE-ONLY 薄壳 Activity —— 验证「原生气泡承载外部浏览器 CCT」（proposal 收敛的「路径 2」）。
 *
 * 与 [CustomTabActivity] 的关系（见 design §5.1）：
 * - 复刻「activity context + 不带 `FLAG_ACTIVITY_NEW_TASK` 启动 CCT」这一现成范式；
 * - 但**不复用** [CustomTabs] 的 `forUrl().launch()` facade——该 facade 藏了两个会制造假阳的副作用：
 *   (c) `getSession()` 会复用 `WebHeadService` 的预热 session，可能覆盖被测浏览器（结论指错对象）；
 *   (d) `openCustomTab()` 在无包/异常时 fallback 到内置 `WebViewActivity`，把「CCT 起不来」伪装成「页面打开了」。
 *   spike 需要一段可控、可 log、无 fallback 掩盖的最小启动，故此处自实现。
 * - 与 [EmbeddableWebViewActivity] 一样在 manifest 声明 `allowEmbedded/resizeable/documentLaunchMode`，
 *   这是气泡能承载它的前提。
 *
 * ⚠️ 蓄意偏离 [CustomTabActivity]（design §5.2）：本薄壳**移除全部自动 `finish()`**
 * （`CustomTabActivity` 有两处：`onResume` 里 `isLoaded → finish()`，以及 `onCreate` 里
 * `savedInstanceState != null → finish()`）。薄壳是气泡 task 的**根 Activity**，任何自动 finish 都可能
 * 销毁气泡 task、破坏 dumpsys 取证的稳定 expanded 态，制造假阴/不可复现。代价是 CCT 关闭后可能露出空白薄壳，
 * spike 可接受，productization 再处理。
 *
 * 本类仅供可行性验证，通过后才做正式功能化，不随产品发布。
 */
class BubbleCctShellActivity : BrowsingActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    // BrowsingActivity.onCreate 会在 intent.data == null 时 finish；气泡 viewIntent 带 data=url，故正常继续。
    super.onCreate(savedInstanceState)
    // §5.2：不写 `savedInstanceState != null → finish()`。仅首次创建时启动 CCT，避免重建时重复拉起。
    if (savedInstanceState == null) {
      launchExternalCct()
    }
  }

  private fun launchExternalCct() {
    val uri: Uri = intent?.data ?: run {
      Timber.e("[BUBBLE_SPIKE] no data uri, NO fallback")
      return
    }
    // 所选后端浏览器；null 即判失败并 loud log（§5.1），绝不静默回退。
    val pkg = CustomTabs.getCustomTabPackage(this)
    if (pkg == null) {
      Timber.e("[BUBBLE_SPIKE] no custom tab package resolved, NO fallback, uri=%s", uri)
      return
    }
    // §5.1(c)：不复用 CustomTabs facade，自建 builder ⇒ 天然不触碰 WebHeadService session，
    // 无 null-session 覆盖被测浏览器之虞。
    val cct = CustomTabsIntent.Builder().build()
    cct.intent.setPackage(pkg)
    // §5.1(b)：launch 前把实际 flags 打进 log，消掉「androidx.browser 是否默认加 flag」的黑箱——以真机 log 为准。
    Timber.d("[BUBBLE_SPIKE] pkg=%s flags=0x%x uri=%s", pkg, cct.intent.flags, uri)
    try {
      // this = activity context，且**不追加任何 flag**（尤其不带 FLAG_ACTIVITY_NEW_TASK）——这是路径 2 的核心前提。
      cct.launchUrl(this, uri)
      Timber.d("[BUBBLE_SPIKE] launched CCT pkg=%s uri=%s", pkg, uri)
    } catch (e: Exception) {
      // §5.1(d)：捕获异常即判失败、loud log，**绝不回退内置 WebView**。
      Timber.e(e, "[BUBBLE_SPIKE] launch failed, NO fallback, pkg=%s uri=%s", pkg, uri)
    }
  }

  override fun inject(activityComponent: ActivityComponent) {
    activityComponent.inject(this)
  }

  override fun onWebsiteLoaded(website: Website) {
    // no-op：薄壳不渲染页面，仅转交外部浏览器 CCT。
  }

  override val layoutRes: Int get() = 0
}
