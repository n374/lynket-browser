/*
 *
 *  Lynket
 *
 *  Copyright (C) 2025 Arunkumar
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

package arun.com.chromer.shared.views

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import arun.com.chromer.R
import kotlinx.coroutines.*

/**
 * Modern Material Design bottom sheet for picking apps that can handle an intent.
 * Replaces the deprecated Flipboard BottomSheetLayout with Material Components.
 */
class IntentPickerBottomSheet : BottomSheetDialogFragment() {

    private lateinit var intent: Intent
    private lateinit var titleText: String
    private var listener: OnIntentPickedListener? = null
    private var filter: Filter = Filter { true }
    private var sortMethod: Comparator<ActivityInfo> = compareBy { it.label }
    private var mixins: List<ActivityInfo> = emptyList()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_intent_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleView = view.findViewById<TextView>(R.id.title)
        val recyclerView = view.findViewById<RecyclerView>(R.id.apps_grid)

        titleView.text = titleText

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)

        // Load apps asynchronously
        scope.launch {
            val apps = withContext(Dispatchers.Default) {
                loadActivityInfos(requireContext())
            }
            recyclerView.adapter = IntentPickerAdapter(apps) { activityInfo ->
                listener?.onIntentPicked(activityInfo)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }

    private fun loadActivityInfos(context: Context): List<ActivityInfo> {
        val packageManager = context.packageManager
        val resolveInfos = packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL
        )

        val activityInfos = mutableListOf<ActivityInfo>()
        activityInfos.addAll(mixins)

        for (resolveInfo in resolveInfos) {
            val componentName = ComponentName(
                resolveInfo.activityInfo.packageName,
                resolveInfo.activityInfo.name
            )
            val activityInfo = ActivityInfo(
                resolveInfo = resolveInfo,
                label = resolveInfo.loadLabel(packageManager).toString(),
                componentName = componentName,
                icon = resolveInfo.loadIcon(packageManager)
            )
            if (filter.include(activityInfo)) {
                activityInfos.add(activityInfo)
            }
        }

        return activityInfos.sortedWith(sortMethod)
    }

    /**
     * Represents an app that can handle the intent
     */
    data class ActivityInfo(
        val resolveInfo: ResolveInfo?,
        val label: String,
        val componentName: ComponentName,
        val icon: Drawable?
    ) {
        var tag: Any? = null

        // Secondary constructor for mixin apps
        constructor(
            icon: Drawable?,
            label: String,
            context: Context,
            clazz: Class<*>
        ) : this(
            resolveInfo = null,
            label = label,
            componentName = ComponentName(context, clazz.name),
            icon = icon
        )

        fun getConcreteIntent(intent: Intent): Intent {
            return Intent(intent).apply {
                component = componentName
            }
        }
    }

    /**
     * Filter interface for excluding certain apps
     */
    fun interface Filter {
        fun include(info: ActivityInfo): Boolean
    }

    /**
     * Listener for when an app is picked
     */
    fun interface OnIntentPickedListener {
        fun onIntentPicked(activityInfo: ActivityInfo)
    }

    private inner class IntentPickerAdapter(
        private val items: List<ActivityInfo>,
        private val onItemClick: (ActivityInfo) -> Unit
    ) : RecyclerView.Adapter<IntentPickerAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_intent_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val iconView: ImageView = itemView.findViewById(R.id.icon)
            private val labelView: TextView = itemView.findViewById(R.id.label)

            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(items[position])
                    }
                }
            }

            fun bind(activityInfo: ActivityInfo) {
                labelView.text = activityInfo.label
                iconView.setImageDrawable(activityInfo.icon)
            }
        }
    }

    companion object {
        /**
         * Creates a new instance of IntentPickerBottomSheet
         */
        fun newInstance(
            intent: Intent,
            @StringRes titleRes: Int,
            listener: OnIntentPickedListener
        ): IntentPickerBottomSheet {
            return newInstance(intent, "", listener).apply {
                // Title will be set from resources in the fragment
                arguments = Bundle().apply {
                    putInt("titleRes", titleRes)
                }
            }
        }

        /**
         * Creates a new instance of IntentPickerBottomSheet
         */
        fun newInstance(
            intent: Intent,
            title: String,
            listener: OnIntentPickedListener
        ): IntentPickerBottomSheet {
            return IntentPickerBottomSheet().apply {
                this.intent = intent
                this.titleText = title
                this.listener = listener
            }
        }

        /**
         * Filter that excludes the app's own package
         */
        fun selfPackageExcludeFilter(context: Context): Filter {
            return Filter { info ->
                !info.componentName.packageName.equals(context.packageName, ignoreCase = true)
            }
        }
    }

    /**
     * Sets a custom filter for filtering apps
     */
    fun setFilter(filter: Filter): IntentPickerBottomSheet {
        this.filter = filter
        return this
    }

    /**
     * Sets a custom sort method
     */
    fun setSortMethod(sortMethod: Comparator<ActivityInfo>): IntentPickerBottomSheet {
        this.sortMethod = sortMethod
        return this
    }

    /**
     * Sets custom mixin apps to include
     */
    fun setMixins(mixins: List<ActivityInfo>): IntentPickerBottomSheet {
        this.mixins = mixins
        return this
    }
}
