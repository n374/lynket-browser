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

package arun.com.chromer.settings.browsingmode

import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.ContextCompat
import arun.com.chromer.R
import arun.com.chromer.settings.BUBBLE_EXTERNAL_BROWSER_PREFERENCE
import arun.com.chromer.settings.NATIVE_BUBBLES_PREFERENCE
import arun.com.chromer.settings.preferences.BasePreferenceFragment
import arun.com.chromer.settings.widgets.IconSwitchPreference
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable

/**
 * RAS-55：原生气泡（Native Bubbles）专属选项。目前只有一项——「用外部浏览器打开气泡」：
 * 气泡展开时改用所选外部浏览器的 Custom Tab 承载页面，复用其登录态
 * （见 [arun.com.chromer.browsing.customtabs.BubbleCctShellActivity]）。
 *
 * 仅在原生气泡浏览模式（[NATIVE_BUBBLES_PREFERENCE]）下开关可用；宿主
 * [BrowsingModeActivity] 已将整卡 gate 到 Android Q+（原生气泡的系统前提）。
 */
class NativeBubblePreferenceFragment : BasePreferenceFragment(),
  SharedPreferences.OnSharedPreferenceChangeListener {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.native_bubble_options)
    setupExternalBrowserPreference()
  }

  override fun onResume() {
    super.onResume()
    updateToggleState()
  }

  private fun setupExternalBrowserPreference() {
    (findPreference(BUBBLE_EXTERNAL_BROWSER_PREFERENCE) as IconSwitchPreference).icon =
      IconicsDrawable(requireContext())
        .icon(CommunityMaterial.Icon.cmd_open_in_new)
        .color(ContextCompat.getColor(requireContext(), R.color.material_dark_light))
        .sizeDp(24)
  }

  /** 该开关只在原生气泡模式下有意义，其余模式置灰。 */
  private fun updateToggleState() {
    val nativeBubblesEnabled = sharedPreferences.getBoolean(NATIVE_BUBBLES_PREFERENCE, false)
    enableDisablePreference(nativeBubblesEnabled, BUBBLE_EXTERNAL_BROWSER_PREFERENCE)
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    // 用户在上方卡片切换浏览模式时（BrowsingModeActivity 写 NATIVE_BUBBLES_PREFERENCE）联动刷新。
    if (NATIVE_BUBBLES_PREFERENCE == key) {
      updateToggleState()
    }
  }

  companion object {
    fun newInstance(): NativeBubblePreferenceFragment {
      return NativeBubblePreferenceFragment()
    }
  }
}
