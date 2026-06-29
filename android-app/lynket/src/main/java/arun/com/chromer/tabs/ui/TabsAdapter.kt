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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import arun.com.chromer.R
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.databinding.FragmentTabsItemTemplateBinding
import arun.com.chromer.tabs.*
import arun.com.chromer.util.glide.GlideRequests
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable

/**
 * Created by arunk on 07-03-2017.
 */
class TabsAdapter
constructor(
  val glideRequests: GlideRequests,
  val tabsManager: TabsManager
) : ListAdapter<TabsManager.Tab, TabsAdapter.TabsViewHolder>(TabDiff) {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): TabsViewHolder {
    val binding = FragmentTabsItemTemplateBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return TabsViewHolder(binding, ::getItem)
  }

  override fun onBindViewHolder(
    holder: TabsViewHolder,
    position: Int
  ) = holder.bind(getItem(position))

  override fun onViewRecycled(holder: TabsViewHolder) {
    super.onViewRecycled(holder)
    glideRequests.clear(holder.binding.icon)
  }

  fun getTabAt(adapterPosition: Int): TabsManager.Tab = getItem(adapterPosition)

  inner class TabsViewHolder(
    val binding: FragmentTabsItemTemplateBinding,
    getItem: (Int) -> TabsManager.Tab
  ) : RecyclerView.ViewHolder(binding.root) {

    init {
      itemView.setOnClickListener {
        if (adapterPosition != RecyclerView.NO_POSITION) {
          val tab = getItem(adapterPosition)
          val url = tab.url
          tabsManager.reOrderTabByUrl(
            itemView.context,
            Website(url),
            listOf(tab.getTargetActivityName())
          )
        }
      }
    }

    fun bind(tab: TabsManager.Tab) {
      if (tab.website != null) {
        binding.websiteTitle.text = tab.website?.safeLabel()
        glideRequests.load(tab.website).into(binding.icon)
        binding.websiteUrl.text = tab.website?.url
        when (tab.type) {
          WEB_VIEW, WEB_VIEW_EMBEDDED -> {
            binding.websiteTabMode.setText(R.string.web_view)
            binding.websiteTabModeIcon.setImageDrawable(
              IconicsDrawable(itemView.context)
                .icon(CommunityMaterial.Icon.cmd_web)
                .color(ContextCompat.getColor(itemView.context, R.color.md_blue_500))
                .sizeDp(16)
            )
          }
          CUSTOM_TAB -> {
            binding.websiteTabMode.setText(R.string.custom_tab)
            binding.websiteTabModeIcon.setImageDrawable(
              IconicsDrawable(itemView.context)
                .icon(CommunityMaterial.Icon.cmd_google_chrome)
                .color(ContextCompat.getColor(itemView.context, R.color.md_orange_500))
                .sizeDp(16)
            )
          }
          ARTICLE -> {
            binding.websiteTabMode.setText(R.string.article_mode)
            binding.websiteTabModeIcon.setImageDrawable(
              IconicsDrawable(itemView.context)
                .icon(CommunityMaterial.Icon.cmd_file_document)
                .color(ContextCompat.getColor(itemView.context, R.color.md_grey_700))
                .sizeDp(16)
            )
          }
        }
      } else {
        //  binding.websiteTitle.text = tab.url
      }
    }
  }

  private object TabDiff : DiffUtil.ItemCallback<TabsManager.Tab>() {

    override fun areItemsTheSame(
      oldItem: TabsManager.Tab,
      newItem: TabsManager.Tab
    ): Boolean = oldItem == newItem

    override fun areContentsTheSame(
      oldItem: TabsManager.Tab,
      newItem: TabsManager.Tab
    ): Boolean = oldItem == newItem
  }
}
