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

package arun.com.chromer.tabs.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import arun.com.chromer.R
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.databinding.FragmentTabsBinding
import arun.com.chromer.di.fragment.FragmentComponent
import arun.com.chromer.extenstions.gone
import arun.com.chromer.extenstions.show
import arun.com.chromer.shared.FabHandler
import arun.com.chromer.shared.base.fragment.BaseFragment
import arun.com.chromer.tabs.TabsManager
import arun.com.chromer.util.glide.GlideApp
import com.afollestad.materialdialogs.MaterialDialog
import javax.inject.Inject

/**
 * Created by arunk on 20-12-2017.
 */
class TabsFragment : BaseFragment(), FabHandler {

  private var _binding: FragmentTabsBinding? = null
  private val binding get() = _binding!!

  @Inject
  lateinit var tabsManager: TabsManager

  @Inject
  lateinit var viewModelFactory: ViewModelProvider.Factory

  private var tabsViewModel: TabsViewModel? = null

  lateinit var tabsAdapter: TabsAdapter

  override fun inject(fragmentComponent: FragmentComponent) {
    fragmentComponent.inject(this)
  }

  override val layoutRes: Int get() = R.layout.fragment_tabs


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentTabsBinding.inflate(inflater, container, false)
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
    setupRecyclerView()
    with(binding.swipeRefreshLayout) {
      setOnRefreshListener {
        loadTabs()
        isRefreshing = false
      }
      setColorSchemeResources(
        R.color.colorPrimary,
        R.color.colorAccent,
        R.color.colorPrimaryDarker
      )
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    tabsViewModel = ViewModelProviders.of(this, viewModelFactory).get(TabsViewModel::class.java)
    observeViewModel()
  }

  private fun observeViewModel() {
    tabsViewModel?.apply {
      loadingLiveData.observe(viewLifecycleOwner, { loading ->
        showLoading(loading!!)
      })
      tabsData.observe(viewLifecycleOwner, {
        setTabs(it!!)
      })
    }
  }

  private fun setupRecyclerView() {
    // Setup RecyclerView
    tabsAdapter = TabsAdapter(GlideApp.with(this), tabsManager)
    binding.tabsRecyclerView.apply {
      layoutManager = LinearLayoutManager(activity)
      adapter = tabsAdapter
    }

    val swipeTouch = object : ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {
      override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
      ): Boolean {
        return false
      }

      override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val tab = tabsAdapter.getTabAt(viewHolder.adapterPosition)
        activity?.let {
          tabsManager.finishTabByUrl(
            activity!!,
            Website(tab.url),
            listOf(tab.getTargetActivityName())
          )
          loadTabs()
        }
      }
    }
    ItemTouchHelper(swipeTouch).apply { attachToRecyclerView(binding.tabsRecyclerView) }
  }

  private fun setTabs(tabs: List<TabsManager.Tab>) {
    tabsAdapter.submitList(tabs)
    TransitionManager.beginDelayedTransition(binding.fragmentTabsRoot)
    if (tabs.isEmpty()) {
      binding.error.show()
      binding.swipeRefreshLayout.gone()
    } else {
      binding.error.gone()
      binding.swipeRefreshLayout.show()
    }
  }

  private fun showLoading(loading: Boolean) {
    binding.swipeRefreshLayout.isRefreshing = loading
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    if (!hidden) {
      activity!!.setTitle(R.string.title_tabs)
      loadTabs()
    }

  }

  override fun onResume() {
    super.onResume()
    if (!isHidden) {
      loadTabs()
    }
  }

  private fun loadTabs() {
    tabsViewModel?.loadTabs()
  }

  override fun onFabClick() {
    if (tabsAdapter.itemCount != 0) {
      MaterialDialog.Builder(activity!!)
        .title(R.string.are_you_sure)
        .content(R.string.tab_deletion_confirmation_content)
        .positiveText(android.R.string.yes)
        .negativeText(android.R.string.no)
        .onPositive { _, _ -> tabsViewModel?.clearAllTabs() }
        .show()
    }
  }
}
