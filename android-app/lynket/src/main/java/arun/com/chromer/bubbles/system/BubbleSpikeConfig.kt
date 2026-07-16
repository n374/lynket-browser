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
 * RAS-55 · SPIKE-ONLY 编译期开关（design §5.3）。
 *
 * 控制原生气泡展开的目标 Activity：
 * - `true`（实验组）→ [arun.com.chromer.browsing.customtabs.BubbleCctShellActivity]，
 *   气泡内改用「所选外部浏览器的 Custom Tab」渲染（验证路径 2 是否被平台允许）。
 * - `false`（对照组）→ [arun.com.chromer.browsing.webview.EmbeddableWebViewActivity]，
 *   维持现状的 App 内置 WebView，用于隔离「气泡本身坏了」与「CCT 嵌不进」两类失败。
 *
 * 这是**默认值**；`BubbleSpikeTriggerActivity` 可用 intent extra `target=control|experiment`
 * 在**同一 APK** 内逐次覆盖，实现同机 A/B（design §5.3「同一 APK 下先跑对照组…再切实验组」）。
 * 翻这个常量再重编则改变默认组。spike 通过后本文件随功能化清理，不随产品发布。
 */
object BubbleSpikeConfig {
  const val DEFAULT_USE_CCT_SHELL: Boolean = true
}
