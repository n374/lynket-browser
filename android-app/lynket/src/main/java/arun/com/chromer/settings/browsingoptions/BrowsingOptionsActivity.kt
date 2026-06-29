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

package arun.com.chromer.settings.browsingoptions

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arun.com.chromer.R
import arun.com.chromer.browsing.providerselection.ProviderSelectionActivity
import arun.com.chromer.di.activity.ActivityComponent
import arun.com.chromer.settings.Preferences
import arun.com.chromer.settings.widgets.AppPreferenceCardView
import arun.com.chromer.shared.Constants.TEXT_SHARE_INTENT
import arun.com.chromer.shared.Constants.WEB_INTENT
import arun.com.chromer.shared.ServiceManager
import arun.com.chromer.shared.base.Snackable
import arun.com.chromer.shared.base.activity.BaseActivity
import arun.com.chromer.shared.views.IntentPickerBottomSheet
import arun.com.chromer.util.HtmlCompat
import arun.com.chromer.util.RxEventBus
import arun.com.chromer.util.Utils
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import javax.inject.Inject

/**
 * Activity for configuring browsing options including custom tabs provider, secondary browser,
 * favorite share app, and bottom bar actions.
 * Modernized to use Material Design BottomSheetDialogFragment instead of Flipboard BottomSheet.
 */
class BrowsingOptionsActivity : BaseActivity(), Snackable,
    SharedPreferences.OnSharedPreferenceChangeListener {

    override val layoutRes: Int = R.layout.activity_browsing_options

    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: Toolbar
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var customTabPreferenceView: AppPreferenceCardView
    private lateinit var browserPreferenceView: AppPreferenceCardView
    private lateinit var favSharePreferenceView: AppPreferenceCardView
    private lateinit var errorView: TextView

    @Inject
    lateinit var eventBus: RxEventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize views
        recyclerView = findViewById(R.id.bottom_bar_action_list)
        toolbar = findViewById(R.id.toolbar)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        customTabPreferenceView = findViewById(R.id.customtab_preference_view)
        browserPreferenceView = findViewById(R.id.browser_preference_view)
        favSharePreferenceView = findViewById(R.id.favshare_preference_view)
        errorView = findViewById(R.id.error)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction()
            .replace(R.id.behaviour_fragment_container, BehaviorPreferenceFragment.newInstance())
            .replace(R.id.web_head_fragment_container, WebHeadOptionsFragment.newInstance())
            .replace(
                R.id.bottom_bar_preference_fragment_container,
                BottomBarPreferenceFragment.newInstance()
            )
            .commit()

        initBottomActions()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
        showHideErrorView()
        customTabPreferenceView.refreshState()
    }

    override fun onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    private fun initBottomActions() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = BottomActionsAdapter(this)
    }

    private fun setupClickListeners() {
        customTabPreferenceView.setOnClickListener {
            startActivity(Intent(this, ProviderSelectionActivity::class.java))
        }

        browserPreferenceView.setOnClickListener {
            showBrowserPicker()
        }

        favSharePreferenceView.setOnClickListener {
            showFavSharePicker()
        }
    }

    private fun showBrowserPicker() {
        val bottomSheet = IntentPickerBottomSheet.newInstance(
            intent = WEB_INTENT,
            titleRes = R.string.choose_secondary_browser,
            listener = IntentPickerBottomSheet.OnIntentPickedListener { activityInfo ->
                browserPreferenceView.updatePreference(activityInfo.componentName)
                snack(String.format(getString(R.string.secondary_browser_success), activityInfo.label))
            }
        ).setFilter(IntentPickerBottomSheet.selfPackageExcludeFilter(this))

        bottomSheet.show(supportFragmentManager, "browser_picker")
    }

    private fun showFavSharePicker() {
        val bottomSheet = IntentPickerBottomSheet.newInstance(
            intent = TEXT_SHARE_INTENT,
            titleRes = R.string.choose_fav_share_app,
            listener = IntentPickerBottomSheet.OnIntentPickedListener { activityInfo ->
                favSharePreferenceView.updatePreference(activityInfo.componentName)
                snack(String.format(getString(R.string.fav_share_success), activityInfo.label))
            }
        ).setFilter(IntentPickerBottomSheet.selfPackageExcludeFilter(this))

        bottomSheet.show(supportFragmentManager, "fav_share_picker")
    }

    private fun showHideErrorView() {
        errorView.visibility = if (!Preferences.get(this).webHeads()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (Preferences.WEB_HEAD_ENABLED.equals(key, ignoreCase = true)) {
            showHideErrorView()
        }
    }

    override fun snack(message: String) {
        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun snackLong(message: String) {
        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG).show()
    }

    override fun inject(activityComponent: ActivityComponent) {
        activityComponent.inject(this)
    }

    class ProviderChanged

    /**
     * Adapter for displaying bottom bar actions explanation
     */
    class BottomActionsAdapter(private val context: Context) :
        RecyclerView.Adapter<BottomActionsAdapter.BottomActionHolder>() {

        private val items = mutableListOf<String>().apply {
            add(NEW_TAB)
            add(SHARE)
            if (Utils.ANDROID_LOLLIPOP) {
                add(TABS)
                add(MINIMIZE)
            }
            add(ARTICLE)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomActionHolder {
            val view = LayoutInflater.from(context).inflate(
                R.layout.activity_browsing_option_bottom_actions_item_template,
                parent,
                false
            )
            return BottomActionHolder(view)
        }

        override fun onBindViewHolder(holder: BottomActionHolder, position: Int) {
            val iconColor = ContextCompat.getColor(context, R.color.colorAccentLighter)
            val item = items[position]

            when (item) {
                NEW_TAB -> {
                    holder.icon.setImageDrawable(
                        createIcon(CommunityMaterial.Icon.cmd_plus, iconColor)
                    )
                    holder.action.text = HtmlCompat.fromHtml(
                        context.getString(R.string.new_tab_action_explanation)
                    )
                }
                SHARE -> {
                    holder.icon.setImageDrawable(
                        createIcon(CommunityMaterial.Icon.cmd_share_variant, iconColor)
                    )
                    holder.action.text = HtmlCompat.fromHtml(
                        context.getString(R.string.share_action_explanation)
                    )
                }
                MINIMIZE -> {
                    holder.icon.setImageDrawable(
                        createIcon(CommunityMaterial.Icon.cmd_arrow_down, iconColor)
                    )
                    holder.action.text = HtmlCompat.fromHtml(
                        context.getString(R.string.minimize_action_explanation)
                    )
                }
                ARTICLE -> {
                    holder.icon.setImageDrawable(
                        createIcon(CommunityMaterial.Icon.cmd_file_document, iconColor)
                    )
                    holder.action.text = HtmlCompat.fromHtml(
                        context.getString(R.string.bottom_bar_article_mode_explanation)
                    )
                }
                TABS -> {
                    holder.icon.setImageDrawable(
                        createIcon(CommunityMaterial.Icon.cmd_view_agenda, iconColor)
                    )
                    holder.action.text = HtmlCompat.fromHtml(
                        context.getString(R.string.bottom_bar_tabs_explanation)
                    )
                }
            }
        }

        override fun getItemCount(): Int = items.size

        private fun createIcon(icon: CommunityMaterial.Icon, color: Int): Drawable {
            return IconicsDrawable(context)
                .icon(icon)
                .color(color)
                .sizeDp(18)
        }

        class BottomActionHolder(view: View) : RecyclerView.ViewHolder(view) {
            val action: TextView = view.findViewById(R.id.bottom_action)
            val icon: ImageView = view.findViewById(R.id.bottom_icon)
        }

        companion object {
            private const val NEW_TAB = "NEW_TAB"
            private const val SHARE = "SHARE"
            private const val MINIMIZE = "MINIMIZE"
            private const val ARTICLE = "ARTICLE"
            private const val TABS = "TABS"
        }
    }
}
