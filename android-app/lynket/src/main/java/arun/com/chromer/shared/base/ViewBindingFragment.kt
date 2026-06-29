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
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Base fragment class that simplifies ViewBinding usage with proper lifecycle handling.
 *
 * Usage example:
 * ```
 * class MyFragment : ViewBindingFragment<FragmentMyBinding>() {
 *   override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentMyBinding
 *     get() = FragmentMyBinding::inflate
 *
 *   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *     super.onViewCreated(view, savedInstanceState)
 *     binding.myTextView.text = "Hello"
 *   }
 * }
 * ```
 */
abstract class ViewBindingFragment<VB : ViewBinding> : Fragment() {

  private var _binding: VB? = null

  /**
   * Access to the binding object. Only valid between onCreateView and onDestroyView.
   */
  protected val binding: VB
    get() = _binding ?: throw IllegalStateException(
      "Binding is only valid between onCreateView and onDestroyView"
    )

  /**
   * Provide the binding inflater. Example: FragmentMainBinding::inflate
   */
  protected abstract val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = bindingInflater(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
