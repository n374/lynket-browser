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
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import arun.com.chromer.R
import arun.com.chromer.databinding.FragmentWebHeadsIntroBinding
import arun.com.chromer.di.fragment.FragmentComponent
import arun.com.chromer.shared.base.fragment.BaseFragment
import arun.com.chromer.tabs.TabsManager
import arun.com.chromer.util.glide.GlideApp
import com.github.paolorotolo.appintro.ISlideBackgroundColorHolder
import javax.inject.Inject

open class WebHeadsIntroFragment : BaseFragment(), ISlideBackgroundColorHolder {
  private var _binding: FragmentWebHeadsIntroBinding? = null
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
    get() = R.layout.fragment_web_heads_intro

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = FragmentWebHeadsIntroBinding.inflate(inflater, container, false).also {
    _binding = it
  }.root

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    GlideApp.with(this).load(R.drawable.tutorial_web_heads).into(binding.imageView)
    binding.watchDemo.setOnClickListener {
      startActivity(
        Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://www.youtube.com/watch?v=3gbz8PI8BVI&feature=youtu.be")
        )
      )
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
