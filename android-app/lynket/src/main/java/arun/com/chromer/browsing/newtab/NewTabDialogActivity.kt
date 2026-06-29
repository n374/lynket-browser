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

package arun.com.chromer.browsing.newtab

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import arun.com.chromer.R
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.databinding.ActivityNewTabBinding
import arun.com.chromer.di.activity.ActivityComponent
import arun.com.chromer.extenstions.showKeyboard
import arun.com.chromer.shared.base.activity.BaseActivity
import arun.com.chromer.tabs.TabsManager
import com.afollestad.materialdialogs.MaterialDialog
import rx.subscriptions.CompositeSubscription
import javax.inject.Inject

/**
 * Simple dialog activity to launch new url in a popup. Shows a search view.
 */
class NewTabDialogActivity : BaseActivity() {
  private var newTabDialog: NewTabDialog? = null

  @Inject
  lateinit var tabsManager: TabsManager

  override val layoutRes: Int
    get() = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    newTabDialog = NewTabDialog(this).show()
  }


  override fun inject(activityComponent: ActivityComponent) = activityComponent.inject(this)

  override fun onDestroy() {
    super.onDestroy()
    newTabDialog?.dismiss()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    newTabDialog?.onActivityResult(requestCode, resultCode, data)
  }

  inner class NewTabDialog(
    private var activity: Activity?
  ) : DialogInterface.OnDismissListener {

    val subs = CompositeSubscription()

    private lateinit var dialog: MaterialDialog
    private var dialogBinding: ActivityNewTabBinding? = null

    fun show(): NewTabDialog {
      dialog = MaterialDialog.Builder(activity!!)
        .backgroundColorRes(android.R.color.transparent)
        .customView(R.layout.activity_new_tab, false)
        .dismissListener(this)
        .show()

      dialog.window?.let { diaWindow ->
        diaWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        diaWindow.setGravity(Gravity.BOTTOM)
      }

      dialogBinding = ActivityNewTabBinding.bind(dialog.customView!!)

      dialogBinding!!.materialSearchView.apply {
        searchPerforms()
          .takeUntil(lifecycleEvents.destroys)
          .subscribe { url ->
            postDelayed({ launchUrl(url) }, 150)
          }
        voiceSearchFailed()
          .takeUntil(lifecycleEvents.destroys)
          .subscribe {
            Toast.makeText(activity, R.string.no_voice_rec_apps, Toast.LENGTH_SHORT).show()
          }
        post {
          gainFocus()
          editText.requestFocus()
          showKeyboard(force = true)
        }
      }
      return this
    }


    private fun launchUrl(url: String) {
      activity?.let {
        tabsManager.openUrl(
          it,
          website = Website(url),
          fromApp = true,
          fromWebHeads = false,
          fromNewTab = true
        )
      }
      dialog.dismiss()
    }

    fun dismiss() {
      dialog.dismiss()
    }

    override fun onDismiss(dialogInterface: DialogInterface?) {
      subs.clear()
      activity?.finish()
      activity = null
      dialogBinding = null
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      dialogBinding?.materialSearchView?.onActivityResult(requestCode, resultCode, data)
    }
  }
}
