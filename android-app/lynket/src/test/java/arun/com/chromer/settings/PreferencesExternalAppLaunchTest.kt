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

package arun.com.chromer.settings

import androidx.test.core.app.ApplicationProvider
import arun.com.chromer.LynketTestApplication
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * RAS-58 · "remember my choice" allowlist storage (design §5) — including the StringSet
 * copy-on-write pitfall: mutating the set returned by getStringSet in place makes
 * SharedPreferences silently drop updates.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], application = LynketTestApplication::class)
class PreferencesExternalAppLaunchTest {

  private lateinit var preferences: Preferences

  @Before
  fun setUp() {
    preferences = Preferences(ApplicationProvider.getApplicationContext())
    preferences.clearExternalAppLaunchChoices()
  }

  @Test
  fun `unknown key is not allowed by default`() {
    assertFalse(preferences.isExternalAppLaunchAllowed("scheme:weixin"))
  }

  @Test
  fun `remembered key is allowed`() {
    preferences.rememberExternalAppLaunch("package:com.tencent.mm")
    assertTrue(preferences.isExternalAppLaunchAllowed("package:com.tencent.mm"))
    assertFalse("other keys must stay unaffected", preferences.isExternalAppLaunchAllowed("scheme:weixin"))
  }

  @Test
  fun `sequential remembers accumulate (copy-on-write regression guard)`() {
    // With an in-place mutation bug, the second remember would be swallowed by the
    // SharedPreferences cache returning the same Set instance.
    preferences.rememberExternalAppLaunch("scheme:weixin")
    preferences.rememberExternalAppLaunch("package:com.tencent.mm")
    assertTrue(preferences.isExternalAppLaunchAllowed("scheme:weixin"))
    assertTrue(preferences.isExternalAppLaunchAllowed("package:com.tencent.mm"))
  }

  @Test
  fun `clear removes all remembered keys`() {
    preferences.rememberExternalAppLaunch("scheme:weixin")
    preferences.rememberExternalAppLaunch("package:com.tencent.mm")
    preferences.clearExternalAppLaunchChoices()
    assertFalse(preferences.isExternalAppLaunchAllowed("scheme:weixin"))
    assertFalse(preferences.isExternalAppLaunchAllowed("package:com.tencent.mm"))
  }

  @Test
  fun `remember is idempotent`() {
    preferences.rememberExternalAppLaunch("scheme:weixin")
    preferences.rememberExternalAppLaunch("scheme:weixin")
    assertTrue(preferences.isExternalAppLaunchAllowed("scheme:weixin"))
  }
}
