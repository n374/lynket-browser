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
package dev.arunkumar.android.epoxy.model

import android.view.View
import com.airbnb.epoxy.EpoxyHolder
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A Kotlin-friendly EpoxyHolder that provides view binding capabilities.
 * This class is designed to work with Kotlin synthetic properties.
 */
abstract class KotlinHolder : EpoxyHolder() {
  lateinit var containerView: View

  override fun bindView(itemView: View) {
    containerView = itemView
  }
}

/**
 * Extension function to create a lazy view binding property.
 */
fun <V : View> KotlinHolder.bind(id: Int): ReadOnlyProperty<KotlinHolder, V> =
  object : ReadOnlyProperty<KotlinHolder, V> {
    override fun getValue(thisRef: KotlinHolder, property: KProperty<*>): V {
      @Suppress("UNCHECKED_CAST")
      return containerView.findViewById(id) as V
    }
  }
