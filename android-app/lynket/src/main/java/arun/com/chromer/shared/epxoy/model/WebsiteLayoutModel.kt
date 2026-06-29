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

package arun.com.chromer.shared.epxoy.model

import android.view.View
import androidx.core.text.toSpannable
import arun.com.chromer.R
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.databinding.WidgetWebsiteGridItemBinding
import arun.com.chromer.tabs.TabsManager
import arun.com.chromer.util.glide.GlideApp
import arun.com.chromer.util.makeMatchingBold
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyAttribute.Option.DoNotHash
import com.airbnb.epoxy.EpoxyModelClass
import dev.arunkumar.android.epoxy.model.KotlinEpoxyModelWithHolder
import dev.arunkumar.android.epoxy.model.KotlinHolder

@EpoxyModelClass(layout = R.layout.widget_website_grid_item)
abstract class WebsiteLayoutModel : KotlinEpoxyModelWithHolder<WebsiteLayoutModel.ViewHolder>() {
  class ViewHolder : KotlinHolder() {
    internal lateinit var binding: WidgetWebsiteGridItemBinding

    override fun bindView(itemView: View) {
      super.bindView(itemView)
      binding = WidgetWebsiteGridItemBinding.bind(itemView)
    }
  }

  @EpoxyAttribute
  lateinit var website: Website

  @EpoxyAttribute(DoNotHash)
  lateinit var tabsManager: TabsManager

  @EpoxyAttribute
  var query: String = ""

  override fun bind(holder: ViewHolder) {
    holder.binding.apply {
      label.text = website.safeLabel().toSpannable().makeMatchingBold(query)
      root.setOnClickListener {
        tabsManager.openUrl(root.context, website)
      }
      GlideApp.with(root.context)
        .load(website)
        .into(icon)
    }
  }
}
