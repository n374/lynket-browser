/*
 * Lynket
 *
 * Copyright (C) 2026 Arunkumar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package arun.com.chromer.util

import android.app.Activity
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import arun.com.chromer.LynketRobolectricSuite
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * Regression test for the home screen's settings gear being drawn under the status bar (and thus
 * untappable) under edge-to-edge on Android 15+/targetSdk 35. [EdgeToEdge.applyContentInsets] must
 * push content out of the system-bar / cutout area by applying those insets as padding.
 */
@Config(sdk = [34])
class EdgeToEdgeTest : LynketRobolectricSuite() {

  @Test
  fun applyContentInsets_padsViewBySystemBarAndCutoutInsets() {
    val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    val view = FrameLayout(activity)

    EdgeToEdge.applyContentInsets(view)
    val insets = WindowInsetsCompat.Builder()
      .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(10, 100, 20, 50))
      .build()
    ViewCompat.dispatchApplyWindowInsets(view, insets)

    assertEquals("top padding must equal status-bar inset", 100, view.paddingTop)
    assertEquals("bottom padding must equal nav-bar inset", 50, view.paddingBottom)
    assertEquals(10, view.paddingLeft)
    assertEquals(20, view.paddingRight)
  }
}
