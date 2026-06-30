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

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Helpers for handling window insets under edge-to-edge layouts.
 *
 * Apps targeting SDK 35 are drawn edge-to-edge on Android 15+ (API 35+). Content that does not
 * consume the system-bar insets ends up under the status/navigation bars (e.g. a top toolbar's
 * icons become untappable). [applyContentInsets] applies those insets as padding so content stays
 * within the safe area.
 */
object EdgeToEdge {

  /**
   * Pads [view] by the system-bar + display-cutout insets so its content is not drawn under the
   * status bar, navigation bar or a display cutout. Safe on all API levels: pre-edge-to-edge the
   * dispatched insets are 0, so this is a no-op there.
   */
  fun applyContentInsets(view: View) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
      val bars = insets.getInsets(
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
      )
      v.updatePadding(top = bars.top, bottom = bars.bottom, left = bars.left, right = bars.right)
      insets
    }
  }
}
