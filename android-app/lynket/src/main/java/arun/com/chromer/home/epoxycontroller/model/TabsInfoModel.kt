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

package arun.com.chromer.home.epoxycontroller.model

import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arun.com.chromer.R
import arun.com.chromer.databinding.LayoutTabsInfoCardBinding
import arun.com.chromer.extenstions.gone
import arun.com.chromer.extenstions.show
import arun.com.chromer.tabs.TabsManager
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyAttribute.Option.DoNotHash
import com.airbnb.epoxy.EpoxyModelClass
import dev.arunkumar.android.epoxy.model.KotlinEpoxyModelWithHolder
import dev.arunkumar.android.epoxy.model.KotlinHolder

@EpoxyModelClass(layout = R.layout.layout_tabs_info_card)
abstract class TabsInfoModel : KotlinEpoxyModelWithHolder<TabsInfoModel.ViewHolder>() {
  @EpoxyAttribute
  lateinit var tabs: List<TabsManager.Tab>

  @EpoxyAttribute(DoNotHash)
  lateinit var tabsManager: TabsManager

  override fun bind(holder: ViewHolder) {
    super.bind(holder)
    holder.binding.tabsDescription.text = holder.binding.tabsDescription.context.resources.getQuantityString(
      R.plurals.active_tabs,
      tabs.size,
      tabs.size
    )
    holder.binding.tabsCard.setOnClickListener {
      tabsManager.showTabsActivity()
    }
    if (tabs.isEmpty()) {
      holder.binding.tabsPreviewRecyclerView.gone()
    } else {
      holder.binding.tabsPreviewRecyclerView.show()
      holder.binding.tabsPreviewRecyclerView.withModels {
        tabs.forEach { tab ->
          tab {
            id(tab.hashCode())
            tab(tab)
            tabsManager(tabsManager)
          }
        }
      }
    }
  }

  class ViewHolder : KotlinHolder() {
    internal lateinit var binding: LayoutTabsInfoCardBinding

    override fun bindView(itemView: View) {
      super.bindView(itemView)
      binding = LayoutTabsInfoCardBinding.bind(itemView)
      binding.tabsPreviewRecyclerView.apply {
        (itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false
        layoutManager = LinearLayoutManager(
          binding.root.context,
          RecyclerView.HORIZONTAL,
          false
        )
      }
    }
  }
}
