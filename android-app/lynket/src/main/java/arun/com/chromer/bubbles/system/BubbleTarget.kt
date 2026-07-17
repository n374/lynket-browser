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

/**
 * RAS-55：原生气泡展开时承载页面的目标引擎。
 *
 * - [EXTERNAL_BROWSER_CCT]：所选外部浏览器的 Custom Tab（薄壳
 *   [arun.com.chromer.browsing.customtabs.BubbleCctShellActivity]），复用该浏览器登录态。
 * - [INTERNAL_WEBVIEW]：App 内置可嵌入 WebView
 *   ([arun.com.chromer.browsing.webview.EmbeddableWebViewActivity])——历史默认行为。
 */
internal enum class BubbleTarget {
  EXTERNAL_BROWSER_CCT,
  INTERNAL_WEBVIEW
}

/**
 * 纯逻辑：决定原生气泡展开的目标引擎。抽成无 Android 依赖的函数以便单测锁定这条关键分支
 * （零回归默认 + 偏好驱动 + 显式覆盖优先级），见 `BubbleTargetResolverTest`。
 *
 * @param explicitUseCctShell 单次显式覆盖（[BubbleLoadData.useCctShell]）；null = 不覆盖，跟随偏好。
 * @param externalBrowserPreferenceEnabled 用户偏好
 *   [arun.com.chromer.settings.RxPreferences.bubbleExternalBrowser] 的当前值（默认 false）。
 * @return 目标引擎；显式值优先，否则跟随偏好。
 */
internal fun resolveBubbleTarget(
  explicitUseCctShell: Boolean?,
  externalBrowserPreferenceEnabled: Boolean
): BubbleTarget =
  if (explicitUseCctShell ?: externalBrowserPreferenceEnabled) {
    BubbleTarget.EXTERNAL_BROWSER_CCT
  } else {
    BubbleTarget.INTERNAL_WEBVIEW
  }
