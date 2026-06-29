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

package arun.com.chromer.home.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import arun.com.chromer.R
import arun.com.chromer.browsing.providerselection.ProviderSelectionActivity
import arun.com.chromer.data.Result
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.databinding.FragmentHomeBinding
import arun.com.chromer.di.fragment.FragmentComponent
import arun.com.chromer.extenstions.appName
import arun.com.chromer.extenstions.gone
import arun.com.chromer.extenstions.show
import arun.com.chromer.extenstions.watch
import arun.com.chromer.settings.Preferences
import arun.com.chromer.settings.browsingoptions.BrowsingOptionsActivity
import arun.com.chromer.shared.Constants
import arun.com.chromer.shared.base.Snackable
import arun.com.chromer.shared.base.fragment.BaseFragment
import arun.com.chromer.tips.TipsActivity
import arun.com.chromer.util.HtmlCompat
import arun.com.chromer.util.RxEventBus
import arun.com.chromer.util.glide.GlideApp
import arun.com.chromer.util.glide.appicon.ApplicationIcon
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import javax.inject.Inject

/**
 * Created by Arunkumar on 07-04-2017.
 */
class HomeFragment : BaseFragment(), Snackable {
  private var _binding: FragmentHomeBinding? = null
  private val binding get() = _binding!!
  @Inject
  lateinit var recentsAdapter: RecentsAdapter

  @Inject
  lateinit var rxEventBus: RxEventBus

  @Inject
  lateinit var viewModelFactory: ViewModelProvider.Factory

  @Inject
  lateinit var preferences: Preferences

  private lateinit var homeFragmentViewModel: HomeFragmentViewModel

  override fun inject(fragmentComponent: FragmentComponent) = fragmentComponent.inject(this)

  override val layoutRes: Int get() = R.layout.fragment_home

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    return super.onCreateView(inflater, container, savedInstanceState).also {
      return binding.root
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupMaterialSearch()
    setupRecents()
    setupProviderCard()
    setupTipsCard()
    setupEventListeners()
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    homeFragmentViewModel =
      ViewModelProviders.of(this, viewModelFactory).get(HomeFragmentViewModel::class.java)
    observeViewModel()
  }

  override fun onResume() {
    super.onResume()
    if (!isHidden) {
      loadRecents()
    }
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    if (!hidden) {
      activity?.setTitle(R.string.app_name)
      loadRecents()
    }
  }

  override fun snack(message: String) {
    (activity as Snackable).snack(message)
  }

  override fun snackLong(message: String) {
    (activity as Snackable).snackLong(message)
  }

  private fun setRecents(websites: List<Website>) {
    recentsAdapter.setWebsites(websites)
    if (websites.isEmpty()) {
      binding.recentMissingText.show()
    } else {
      binding.recentMissingText.gone()
    }
  }

  private fun setupMaterialSearch() {

  }

  private fun loadRecents() {
    homeFragmentViewModel.loadRecents()
  }

  private fun setupRecents() {
    binding.recentsHeaderIcon.setImageDrawable(
      IconicsDrawable(context!!)
        .icon(CommunityMaterial.Icon.cmd_history)
        .colorRes(R.color.accent)
        .sizeDp(24)
    )
    binding.recentsList.apply {
      layoutManager = GridLayoutManager(activity, 4)
      adapter = recentsAdapter
    }
  }

  private fun observeViewModel() {
    homeFragmentViewModel.recentsResultLiveData.watch(this) { result ->
      when (result) {
        is Result.Loading<List<Website>> -> {
          // TODO Show progress bar.
        }
        is Result.Success<List<Website>> -> {
          setRecents(result.data!!)
        }
        else -> {
        }
      }
    }
  }


  // Provider card moved to Epoxy model in HomeActivity (ProviderInfoModel)
  // Keeping method for compatibility but it's no longer used
  private fun setupProviderCard() {
    // No-op: Provider card views removed from fragment_home.xml
    // Provider info now displayed via HomeFeedController in HomeActivity
  }


  private fun setupTipsCard() {
    binding.tipsIcon.setImageDrawable(
      IconicsDrawable(context!!)
        .icon(CommunityMaterial.Icon.cmd_lightbulb_on)
        .colorRes(R.color.md_yellow_700)
        .sizeDp(24)
    )
  }

  private fun setupEventListeners() {
    // Provider change button removed - now handled in HomeActivity

    binding.tipsButton.setOnClickListener {
      Handler().postDelayed({
        startActivity(Intent(context, TipsActivity::class.java))
      }, 200)
    }

    subs.add(rxEventBus
      .filteredEvents<BrowsingOptionsActivity.ProviderChanged>()
      .subscribe { setupProviderCard() })
  }
}
