/*
 * Lynket
 *
 * Copyright (C) 2025 Arunkumar
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

package arun.com.chromer.shared.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Base activity class that simplifies ViewBinding usage.
 *
 * Usage example:
 * ```
 * class MyActivity : ViewBindingActivity<ActivityMyBinding>() {
 *   override val bindingInflater: (LayoutInflater) -> ActivityMyBinding
 *     get() = ActivityMyBinding::inflate
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     binding.myTextView.text = "Hello"
 *   }
 * }
 * ```
 */
abstract class ViewBindingActivity<VB : ViewBinding> : AppCompatActivity() {

  private var _binding: VB? = null

  /**
   * Access to the binding object. Only valid between onCreate and onDestroy.
   */
  protected val binding: VB
    get() = _binding ?: throw IllegalStateException(
      "Binding is only valid between onCreate and onDestroy"
    )

  /**
   * Provide the binding inflater. Example: ActivityMainBinding::inflate
   */
  protected abstract val bindingInflater: (LayoutInflater) -> VB

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    _binding = bindingInflater(layoutInflater)
    setContentView(binding.root)
  }

  override fun onDestroy() {
    super.onDestroy()
    _binding = null
  }
}
