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
import arun.com.chromer.R
import arun.com.chromer.databinding.LayoutFeedHeaderBinding
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import dev.arunkumar.android.epoxy.model.KotlinEpoxyModelWithHolder
import dev.arunkumar.android.epoxy.model.KotlinHolder

@EpoxyModelClass(layout = R.layout.layout_feed_header)
abstract class HeaderLayoutModel : KotlinEpoxyModelWithHolder<HeaderLayoutModel.ViewHolder>() {
  class ViewHolder : KotlinHolder() {
    internal lateinit var binding: LayoutFeedHeaderBinding

    override fun bindView(itemView: View) {
      super.bindView(itemView)
      binding = LayoutFeedHeaderBinding.bind(itemView)
    }
  }

  @EpoxyAttribute
  lateinit var title: String

  override fun bind(holder: ViewHolder) {
    holder.binding.header.text = title
  }
}
