/*
 *
 *  Lynket
 *
 *  Copyright (C) 2025 Arunkumar
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

package arun.com.chromer.browsing.openwith

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import arun.com.chromer.R
import arun.com.chromer.shared.views.IntentPickerBottomSheet

/**
 * Activity that shows a bottom sheet picker for opening a URL with different apps.
 * Modernized to use Material Design BottomSheetDialogFragment instead of Flipboard BottomSheet.
 */
class OpenIntentWithActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_with)

        val intentData = intent?.data
        if (intentData != null) {
            val webSiteIntent = Intent(ACTION_VIEW, intentData)

            val bottomSheet = IntentPickerBottomSheet.newInstance(
                intent = webSiteIntent,
                titleRes = R.string.open_with,
                listener = IntentPickerBottomSheet.OnIntentPickedListener { activityInfo ->
                    webSiteIntent.component = activityInfo.componentName
                    webSiteIntent.flags = FLAG_ACTIVITY_CLEAR_TASK or FLAG_ACTIVITY_NEW_TASK
                    startActivity(webSiteIntent)
                    finish()
                }
            ).setFilter(IntentPickerBottomSheet.selfPackageExcludeFilter(this))

            bottomSheet.show(supportFragmentManager, "intent_picker")
        } else {
            invalidLink()
        }
    }

    private fun invalidLink() {
        Toast.makeText(this, getString(R.string.invalid_link), Toast.LENGTH_SHORT).show()
        finish()
    }
}
