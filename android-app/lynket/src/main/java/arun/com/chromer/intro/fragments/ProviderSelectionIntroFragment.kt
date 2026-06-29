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

package arun.com.chromer.intro.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import arun.com.chromer.R
import arun.com.chromer.browsing.providerselection.ProviderSelectionActivity
import arun.com.chromer.databinding.FragmentProviderSelectionIntroBinding
import arun.com.chromer.di.fragment.FragmentComponent
import arun.com.chromer.shared.base.fragment.BaseFragment
import arun.com.chromer.tabs.TabsManager
import arun.com.chromer.util.glide.GlideApp
import com.github.paolorotolo.appintro.ISlideBackgroundColorHolder
import javax.inject.Inject

open class ProviderSelectionIntroFragment : BaseFragment(), ISlideBackgroundColorHolder {
  private var _binding: FragmentProviderSelectionIntroBinding? = null
  private val binding get() = _binding!!

  override fun getDefaultBackgroundColor(): Int =
    ContextCompat.getColor(requireContext(), R.color.tutorialBackgrounColor)

  override fun setBackgroundColor(backgroundColor: Int) {
    _binding?.root?.setBackgroundColor(backgroundColor)
  }

  @Inject
  lateinit var tabsManager: TabsManager

  override fun inject(fragmentComponent: FragmentComponent) = fragmentComponent.inject(this)

  override val layoutRes: Int
    get() = R.layout.fragment_provider_selection_intro

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = FragmentProviderSelectionIntroBinding.inflate(inflater, container, false).also {
    _binding = it
  }.root

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    GlideApp.with(this).load(R.drawable.tutorial_choose_browser).into(binding.imageView)
    binding.chooseProviderButton.setOnClickListener {
      startActivity(Intent(context, ProviderSelectionActivity::class.java))
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
