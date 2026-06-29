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

package arun.com.chromer.history

import android.os.Bundle
import android.view.MenuItem
import arun.com.chromer.R
import arun.com.chromer.databinding.ActivityHistoryBinding
import arun.com.chromer.di.activity.ActivityComponent
import arun.com.chromer.shared.FabHandler
import arun.com.chromer.shared.base.Snackable
import arun.com.chromer.shared.base.activity.BaseActivity
import com.google.android.material.snackbar.Snackbar

class HistoryActivity : BaseActivity(), Snackable {

  private lateinit var binding: ActivityHistoryBinding

  override val layoutRes: Int
    get() = R.layout.activity_history

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityHistoryBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)
    if (supportActionBar != null) {
      supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    binding.fab.setOnClickListener {
      supportFragmentManager.fragments
        .asSequence()
        .filter { !it.isHidden && it is FabHandler }
        .map { it as FabHandler }
        .toList()
        .first()
        .onFabClick()
    }

    supportFragmentManager
      .beginTransaction()
      .replace(R.id.fragment_container, HistoryFragment())
      .commit()
  }

  override fun snack(textToSnack: String) {
    Snackbar.make(binding.coordinatorLayout, textToSnack, Snackbar.LENGTH_SHORT).show()
  }

  override fun snackLong(textToSnack: String) {
    Snackbar.make(binding.coordinatorLayout, textToSnack, Snackbar.LENGTH_LONG).show()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finishWithTransition()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onBackPressed() {
    finishWithTransition()
    super.onBackPressed()
  }

  override fun inject(activityComponent: ActivityComponent) {
    activityComponent.inject(this)
  }
}
