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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import arun.com.chromer.browsing.BrowsingActivity
import arun.com.chromer.browsing.webview.EmbeddableWebViewActivity
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.di.activity.ActivityComponent
import timber.log.Timber

/**
 * RAS-55 · 原生气泡的「外部浏览器 Custom Tab」薄壳。
 *
 * 作为气泡 task 的根 Activity，在 onCreate 用 **activity context 且不带
 * `FLAG_ACTIVITY_NEW_TASK`** 启动「所选外部浏览器的 Custom Tab」——系统会把这个跨 App 的
 * CCT 渲进气泡浮窗：页面由外部浏览器自身进程渲染，天然复用其登录态（cookie/localStorage）。
 * 真机验证矩阵见 `docs/changes/55-native-bubble-cct/SPIKE-RESULT.md`。
 * 是否走本薄壳由用户偏好 [arun.com.chromer.settings.RxPreferences.bubbleExternalBrowser]
 * 控制（[arun.com.chromer.bubbles.system.BubbleNotificationManager] 选择气泡目标）。
 *
 * 与 [CustomTabActivity] 的关系：
 * - 复刻其「activity context + 不带 NEW_TASK 启动 CCT」范式与 ghost-tab 自杀模式；
 * - 但**不复用** [CustomTabs] 的 `forUrl().launch()` facade，原因有二：
 *   1. facade 的 `getSession()` 会复用 `WebHeadService` 的预热 session，该 session 绑定的
 *      浏览器未必等于用户所选的 `customTabPackage`——气泡场景必须严格命中所选浏览器才能兑现
 *      「复用其登录态」的承诺，故此处显式建无 session 的最小 `CustomTabsIntent`；
 *   2. facade 异常时回退到**全屏** `WebViewActivity`，而气泡内的回退目标应是可嵌入的
 *      [EmbeddableWebViewActivity]（见 [fallbackToWebView]），以保住浮窗形态。
 * - 与 [EmbeddableWebViewActivity] 一样在 manifest 声明
 *   `allowEmbedded/resizeableActivity/documentLaunchMode`，这是气泡能承载它的前提。
 *
 * ⚠️ 已知限制：跨 UID CCT 渲进气泡浮窗依赖平台**未承诺**的行为；系统/浏览器更新可能使其
 * 退化为全屏打开（退化后 ≈ Web Heads 体验，页面本身仍正常，属可接受降级而非功能破坏）。
 */
class BubbleCctShellActivity : BrowsingActivity() {

  /**
   * Ghost-tab 处理（模式同 [CustomTabActivity]）：CCT/回退页关闭后本薄壳会重新获得焦点，
   * 若不自杀用户会看到一个空白半透明壳挂在气泡里。[onAttachedToWindow] 在首次 onResume
   * **之后**才回调，因此启动 CCT 的那次 resume 不会误杀；此后任何一次 resume 都意味着
   * 上层页面已关闭，finish 自己以干净收掉气泡 task。
   */
  private var isLoaded = false

  override fun onCreate(savedInstanceState: Bundle?) {
    // BrowsingActivity.onCreate 在 intent.data == null 时会 toast + finish，此时不再启动任何页面。
    super.onCreate(savedInstanceState)
    when {
      isFinishing -> Unit
      savedInstanceState == null -> launchInBubble()
      else ->
        // 重建（savedInstanceState != null）说明本壳曾被系统销毁后再次露脸，典型场景：
        // 配置变更/进程回收期间 CCT 位于栈顶，壳被懒回收；等 CCT 关闭、轮到壳可见时才被
        // 重建——此刻页面已关，直接 finish 即干净收掉气泡 task（与 CustomTabActivity 同款语义）。
        // 即便个别场景壳在气泡重新展开时直接置顶被重建，finish 后气泡通知仍在，用户再点一次
        // 气泡会以全新实例重启 CCT（documentLaunchMode=always），行为自愈。
        // ⚠️ 绝不能在此改成重新 launch CCT：上述典型场景会变成「页面关了又被拉起」的僵尸循环。
        finish()
    }
  }

  /**
   * 在气泡 task 内启动所选外部浏览器的 CCT。任何一步不可用都优雅回退到内置可嵌入 WebView
   * （[fallbackToWebView]），保证气泡在所有设备上都能打开页面。
   */
  private fun launchInBubble() {
    val uri: Uri = intent?.data ?: return // BrowsingActivity 已兜底 finish，防御性判空。
    val pkg = CustomTabs.getCustomTabPackage(this)
    if (pkg == null) {
      Timber.w("No custom tab provider available, falling back to WebView for %s", uri)
      fallbackToWebView(uri)
      return
    }
    // 显式建无 session 的最小 CCT intent（不走 CustomTabs facade，理由见类注释），
    // 且**不追加任何 flag**——尤其不带 FLAG_ACTIVITY_NEW_TASK：用 activity context 留在
    // 气泡 task 内，是系统把跨 App CCT 渲进气泡浮窗的前提。
    val customTabsIntent = CustomTabsIntent.Builder().build().apply {
      intent.setPackage(pkg)
    }
    try {
      customTabsIntent.launchUrl(this, uri)
      Timber.d("Launched bubble CCT pkg=%s uri=%s", pkg, uri)
    } catch (e: Exception) {
      Timber.e(e, "Bubble CCT launch failed for pkg=%s, falling back to WebView", pkg)
      fallbackToWebView(uri)
    }
  }

  /**
   * 回退路径：CCT 不可用（无 provider / 启动异常）时，用内置的可嵌入 WebView——即原生气泡
   * 的默认目标——打开页面。同样以 activity context、不带 NEW_TASK 启动 ⇒ 留在气泡 task 内、
   * 浮窗形态不变；页面关闭后由 ghost-tab 逻辑（[onResume]）收掉本壳。
   */
  private fun fallbackToWebView(uri: Uri) {
    try {
      startActivity(Intent(this, EmbeddableWebViewActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = uri
      })
    } catch (e: Exception) {
      // 连内置 WebView 都起不来（理论上不可能）：loud log 并收掉空壳，避免留下空白气泡。
      Timber.e(e, "Fallback WebView launch failed for %s", uri)
      finish()
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    isLoaded = true
  }

  override fun onResume() {
    super.onResume()
    if (isLoaded) {
      finish()
    }
  }

  override fun inject(activityComponent: ActivityComponent) {
    activityComponent.inject(this)
  }

  override fun onWebsiteLoaded(website: Website) {
    // no-op：薄壳不渲染页面，仅转交外部浏览器 CCT / 回退 WebView。
  }

  override val layoutRes: Int get() = 0
}
