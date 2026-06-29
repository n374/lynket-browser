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

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import arun.com.chromer.R
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.databinding.FragmentSlideOverIntroBinding
import arun.com.chromer.di.fragment.FragmentComponent
import arun.com.chromer.shared.base.fragment.BaseFragment
import arun.com.chromer.tabs.TabsManager
import arun.com.chromer.util.glide.GlideApp
import com.github.paolorotolo.appintro.ISlideBackgroundColorHolder
import javax.inject.Inject

open class SlideOverExplanationFragment : BaseFragment(), ISlideBackgroundColorHolder {
  private var _binding: FragmentSlideOverIntroBinding? = null
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
    get() = R.layout.fragment_slide_over_intro

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = FragmentSlideOverIntroBinding.inflate(inflater, container, false).also {
    _binding = it
  }.root

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    GlideApp.with(this).load(R.drawable.chromer_hd_icon).into(binding.imageView)
    binding.tryItButton.setOnClickListener {
      tabsManager.openBrowsingTab(
        requireContext(),
        Website("https://goo.gl/search/lynket"),
        smart = false,
        fromNewTab = false
      )
      Handler(Looper.getMainLooper()).postDelayed(
        {
          if (isAdded) {
            Toast.makeText(context, R.string.slide_over_fragment_close_prompt, Toast.LENGTH_SHORT)
              .show()
          }
        },
        200
      )
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
