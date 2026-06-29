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

package arun.com.chromer.tips

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arun.com.chromer.R
import arun.com.chromer.databinding.ActivityTipsBinding
import arun.com.chromer.databinding.LayoutTipsCardBinding
import arun.com.chromer.di.activity.ActivityComponent
import arun.com.chromer.extenstions.inflate
import arun.com.chromer.shared.base.activity.BaseActivity
import arun.com.chromer.util.Utils
import com.bumptech.glide.RequestManager
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import javax.inject.Inject

class TipsActivity : BaseActivity() {

  private lateinit var binding: ActivityTipsBinding

  override fun inject(activityComponent: ActivityComponent) = activityComponent.inject(this)

  @Inject
  lateinit var requestManager: RequestManager

  override val layoutRes: Int get() = R.layout.activity_tips

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityTipsBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setupToolbar()
    setupTipsList()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        finish()
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  private fun setupToolbar() {
    setTitle(R.string.tips)
    setSupportActionBar(binding.toolbar)
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setHomeAsUpIndicator(R.drawable.article_ic_close)
    }
  }

  private fun setupTipsList() {
    binding.tipsRecyclerView.layoutManager = LinearLayoutManager(this)
    binding.tipsRecyclerView.setHasFixedSize(true)
    binding.tipsRecyclerView.adapter = TipsRecyclerViewAdapter()
  }

  inner class TipsRecyclerViewAdapter : RecyclerView.Adapter<TipsItemHolder>() {
    private val provider = 0
    private val secBrowser = 1
    private val perApp = 2
    private val bottomBar = 3
    private val articleKeywords = 4
    private val quicksettings = 5

    private val items = ArrayList<Int>()

    init {
      items.add(provider)
      items.add(secBrowser)
      items.add(perApp)
      if (Utils.ANDROID_LOLLIPOP) {
        items.add(bottomBar)
      }
      items.add(articleKeywords)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        items.add(quicksettings)
      }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: TipsItemHolder, position: Int) {
      when (items[position]) {
        provider -> {
          holder.binding.title.setText(R.string.choose_provider)
          holder.binding.subtitle.setText(R.string.choose_provider_tip)
          requestManager.load(R.drawable.tips_providers).into(holder.binding.image)
          holder.binding.icon.setImageDrawable(
            IconicsDrawable(this@TipsActivity)
              .icon(CommunityMaterial.Icon.cmd_cards)
              .colorRes(R.color.accent)
              .sizeDp(24)
          )
        }
        secBrowser -> {
          holder.binding.title.setText(R.string.choose_secondary_browser)
          holder.binding.subtitle.setText(R.string.tips_secondary_browser)
          requestManager.load(R.drawable.tip_secondary_browser).into(holder.binding.image)
          holder.binding.icon.setImageDrawable(
            IconicsDrawable(this@TipsActivity)
              .icon(CommunityMaterial.Icon.cmd_earth)
              .colorRes(R.color.accent)
              .sizeDp(24)
          )
        }
        perApp -> {
          holder.binding.title.setText(R.string.per_app_settings)
          holder.binding.subtitle.setText(R.string.per_app_settings_explanation)
          requestManager.load(R.drawable.tips_per_app_settings).into(holder.binding.image)
          holder.binding.icon.setImageDrawable(
            IconicsDrawable(this@TipsActivity)
              .icon(CommunityMaterial.Icon.cmd_apps)
              .colorRes(R.color.accent)
              .sizeDp(24)
          )
        }
        bottomBar -> {
          holder.binding.title.setText(R.string.bottom_bar)
          holder.binding.subtitle.setText(R.string.tips_bottom_bar)
          requestManager.load(R.drawable.tips_bottom_bar).into(holder.binding.image)
          holder.binding.icon.setImageDrawable(
            IconicsDrawable(this@TipsActivity)
              .icon(CommunityMaterial.Icon.cmd_drag_horizontal)
              .colorRes(R.color.accent)
              .sizeDp(24)
          )
        }
        articleKeywords -> {
          holder.binding.title.setText(R.string.article_mode)
          holder.binding.subtitle.setText(R.string.tips_article_mode)
          requestManager.load(R.drawable.tips_article_keywords).into(holder.binding.image)
          holder.binding.icon.setImageDrawable(
            IconicsDrawable(this@TipsActivity)
              .icon(CommunityMaterial.Icon.cmd_file_document)
              .colorRes(R.color.accent)
              .sizeDp(24)
          )
        }
        quicksettings -> {
          holder.binding.title.setText(R.string.quick_settings)
          holder.binding.subtitle.setText(R.string.quick_settings_tip)
          requestManager.load(R.drawable.tips_quick_settings).into(holder.binding.image)
          holder.binding.icon.setImageDrawable(
            IconicsDrawable(this@TipsActivity)
              .icon(CommunityMaterial.Icon.cmd_settings)
              .colorRes(R.color.accent)
              .sizeDp(24)
          )
        }
      }
    }

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int
    ): TipsItemHolder {
      val binding = LayoutTipsCardBinding.inflate(
        parent.context.getSystemService(android.view.LayoutInflater::class.java),
        parent,
        false
      )
      return TipsItemHolder(binding)
    }
  }

  class TipsItemHolder(val binding: LayoutTipsCardBinding) : RecyclerView.ViewHolder(binding.root)
}
