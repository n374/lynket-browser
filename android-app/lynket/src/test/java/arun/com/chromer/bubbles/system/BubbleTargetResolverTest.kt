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

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * RAS-55：锁定原生气泡目标引擎的决策逻辑（[resolveBubbleTarget]）——功能的核心分支。纯逻辑，
 * 无 Android 依赖，不需要 Robolectric。
 */
class BubbleTargetResolverTest {

  /** 零回归红线：不显式覆盖(null) 且偏好关闭(false) ⇒ 维持内置 WebView。 */
  @Test
  fun `default (no override, preference off) keeps internal WebView`() {
    assertEquals(
      BubbleTarget.INTERNAL_WEBVIEW,
      resolveBubbleTarget(explicitUseCctShell = null, externalBrowserPreferenceEnabled = false)
    )
  }

  /** 偏好开启且不覆盖 ⇒ 走外部浏览器 CCT。 */
  @Test
  fun `preference on (no override) selects external browser CCT`() {
    assertEquals(
      BubbleTarget.EXTERNAL_BROWSER_CCT,
      resolveBubbleTarget(explicitUseCctShell = null, externalBrowserPreferenceEnabled = true)
    )
  }

  /** 显式覆盖优先于偏好：显式 true ⇒ CCT，即便偏好关闭。 */
  @Test
  fun `explicit true overrides preference off`() {
    assertEquals(
      BubbleTarget.EXTERNAL_BROWSER_CCT,
      resolveBubbleTarget(explicitUseCctShell = true, externalBrowserPreferenceEnabled = false)
    )
  }

  /** 显式覆盖优先于偏好：显式 false ⇒ 内置 WebView，即便偏好开启。 */
  @Test
  fun `explicit false overrides preference on`() {
    assertEquals(
      BubbleTarget.INTERNAL_WEBVIEW,
      resolveBubbleTarget(explicitUseCctShell = false, externalBrowserPreferenceEnabled = true)
    )
  }
}
