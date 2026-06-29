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

package arun.com.chromer.search.suggestion.model

import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import androidx.core.text.toSpannable
import arun.com.chromer.R
import arun.com.chromer.databinding.WidgetSuggestionsItemTemplateBinding
import arun.com.chromer.extenstions.gone
import arun.com.chromer.extenstions.show
import arun.com.chromer.search.suggestion.items.SuggestionItem
import arun.com.chromer.search.suggestion.items.SuggestionType.*
import arun.com.chromer.util.makeMatchingBold
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyAttribute.Option.DoNotHash
import com.airbnb.epoxy.EpoxyModelClass
import dev.arunkumar.android.epoxy.model.KotlinEpoxyModelWithHolder
import dev.arunkumar.android.epoxy.model.KotlinHolder

@EpoxyModelClass(layout = R.layout.widget_suggestions_item_template)
abstract class SuggestionLayoutModel :
  KotlinEpoxyModelWithHolder<SuggestionLayoutModel.ViewHolder>() {
  @EpoxyAttribute
  lateinit var suggestionItem: SuggestionItem

  @EpoxyAttribute(DoNotHash)
  lateinit var copyIcon: Drawable

  @EpoxyAttribute(DoNotHash)
  lateinit var historyIcon: Drawable

  @EpoxyAttribute(DoNotHash)
  lateinit var searchIcon: Drawable

  @EpoxyAttribute(DoNotHash)
  lateinit var onClickListener: View.OnClickListener

  @EpoxyAttribute(DoNotHash)
  lateinit var onLongClickListener: View.OnLongClickListener

  @EpoxyAttribute
  var query: String = ""

  override fun bind(holder: ViewHolder) {
    super.bind(holder)
    holder.apply {
      binding.suggestionsText.text = suggestionItem.title.toSpannable().makeMatchingBold(query)
      when (suggestionItem.type) {
        COPY -> binding.suggestionIcon.setImageDrawable(copyIcon)
        GOOGLE -> binding.suggestionIcon.setImageDrawable(searchIcon)
        HISTORY -> binding.suggestionIcon.setImageDrawable(historyIcon)
      }
      when {
        TextUtils.isEmpty(suggestionItem.subTitle) -> {
          binding.suggestionsSubTitle.gone()
          binding.suggestionsSubTitle.text = null
        }
        else -> {
          binding.suggestionsSubTitle.show()
          binding.suggestionsSubTitle.text = suggestionItem.subTitle
        }
      }
      binding.root.setOnClickListener(onClickListener)
      binding.root.setOnLongClickListener(onLongClickListener)
    }
  }

  class ViewHolder : KotlinHolder() {
    internal lateinit var binding: WidgetSuggestionsItemTemplateBinding

    override fun bindView(itemView: View) {
      super.bindView(itemView)
      binding = WidgetSuggestionsItemTemplateBinding.bind(itemView)
    }
  }
}
