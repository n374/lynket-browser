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

package arun.com.chromer.about

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arun.com.chromer.BuildConfig
import arun.com.chromer.R
import arun.com.chromer.about.changelog.Changelog
import arun.com.chromer.databinding.FragmentAboutBinding
import arun.com.chromer.databinding.FragmentAboutListItemTemplateBinding
import arun.com.chromer.extenstions.gone
import arun.com.chromer.shared.Constants
import arun.com.chromer.util.glide.GlideApp
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable

/**
 * Created by Arun on 11/11/2015.
 */
class AboutFragment : Fragment() {
  private var _binding: FragmentAboutBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentAboutBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    populateData()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  private fun populateData() {
    binding.aboutAppVersionList.apply {
      isNestedScrollingEnabled = false
      layoutManager = LinearLayoutManager(context)
      adapter = AppAdapter()
    }
    binding.aboutAuthorVersionList.apply {
      isNestedScrollingEnabled = false
      layoutManager = LinearLayoutManager(context)
      adapter = AuthorAdapter()
    }
    binding.creditsRv.apply {
      isNestedScrollingEnabled = false
      layoutManager = LinearLayoutManager(context)
      adapter = CreditsAdapter()
    }
  }

  internal inner class AppAdapter : RecyclerView.Adapter<AppAdapter.ItemHolder>() {
    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int
    ) = ItemHolder(
      FragmentAboutListItemTemplateBinding.inflate(
        LayoutInflater.from(activity),
        parent,
        false
      )
    )

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
      val materialDark = ContextCompat.getColor(requireActivity(), R.color.colorAccent)
      holder.itemBinding.aboutAppSubtitle.visibility = View.VISIBLE
      when (position) {
        0 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.version)
          holder.itemBinding.aboutAppSubtitle.text = BuildConfig.VERSION_NAME
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_information_outline)
            .color(materialDark)
            .sizeDp(24)
        }
        1 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.changelog)
          holder.itemBinding.aboutAppSubtitle.setText(R.string.see_whats_new)
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_chart_line)
            .color(materialDark)
            .sizeDp(24)
        }
        2 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.follow_twitter)
          holder.itemBinding.aboutAppSubtitle.visibility = View.GONE
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_twitter)
            .color(materialDark)
            .sizeDp(24)
        }
        3 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.discuss_on_reddit)
          holder.itemBinding.aboutAppSubtitle.visibility = View.GONE
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_reddit)
            .color(materialDark)
            .sizeDp(24)
        }
        4 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.licenses)
          holder.itemBinding.aboutAppSubtitle.visibility = View.GONE
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_wallet_membership)
            .color(materialDark)
            .sizeDp(24)
        }
        5 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.translations)
          holder.itemBinding.aboutAppSubtitle.setText(R.string.help_translations)
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_translate)
            .color(materialDark)
            .sizeDp(24)
        }
        6 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.source)
          holder.itemBinding.aboutAppSubtitle.setText(R.string.contribute_to_chromer)
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_source_branch)
            .color(materialDark)
            .sizeDp(24)
        }
      }
    }

    override fun getItemCount() = 6

    internal inner class ItemHolder(val itemBinding: FragmentAboutListItemTemplateBinding) :
      RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

      init {
        itemView.setOnClickListener(this)
      }

      override fun onClick(view: View) {
        when (adapterPosition) {
          0 -> return
          1 -> Changelog.show(activity)
          2 -> {
            val twitterIntent = Intent(
              Intent.ACTION_VIEW,
              Uri.parse("https://twitter.com/LynketApp")
            )
            requireActivity().startActivity(twitterIntent)
          }
          3 -> {
            val communityIntent = Intent(
              Intent.ACTION_VIEW,
              Uri.parse("https://www.reddit.com/r/lynket/")
            )
            requireActivity().startActivity(communityIntent)
          }
          4 -> {
            val licenses = Intent(
              Intent.ACTION_VIEW,
              Uri.parse("https://htmlpreview.github.io/?https://github.com/arunkumar9t2/lynket-browser/blob/main/notices.html")
            )
            requireActivity().startActivity(licenses)
          }
          5 -> {
            val oneSkyIntent = Intent(
              Intent.ACTION_VIEW,
              Uri.parse("http://os0l2aw.oneskyapp.com/collaboration/project/62112")
            )
            requireActivity().startActivity(oneSkyIntent)
          }
        }
      }
    }
  }

  internal inner class AuthorAdapter : RecyclerView.Adapter<AuthorAdapter.ItemHolder>() {

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int
    ): ItemHolder {
      return ItemHolder(
        FragmentAboutListItemTemplateBinding.inflate(
          LayoutInflater.from(activity),
          parent,
          false
        )
      )
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
      when (position) {
        0 -> {
          holder.itemBinding.aboutAppTitle.text = Constants.ME
          holder.itemBinding.aboutAppSubtitle.text = Constants.LOCATION
          holder.itemBinding.aboutRowItemImage.layoutParams.height =
            resources.getDimension(R.dimen.arun_height).toInt()
          holder.itemBinding.aboutRowItemImage.layoutParams.width =
            resources.getDimension(R.dimen.arun_width).toInt()
          val imageBitmap = BitmapFactory.decodeResource(resources, R.drawable.arun)
          val roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(resources, imageBitmap)
          roundedBitmapDrawable.setAntiAlias(true)
          roundedBitmapDrawable.isCircular = true
          holder.itemBinding.aboutRowItemImage.setImageDrawable(roundedBitmapDrawable)
        }
        1 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.add_to_circles)
          holder.itemBinding.aboutAppSubtitle.visibility = View.GONE
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_google_circles)
            .color(ContextCompat.getColor(requireActivity(), R.color.google_plus))
            .sizeDp(24)
        }
        2 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.follow_twitter)
          holder.itemBinding.aboutAppSubtitle.visibility = View.GONE
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_twitter)
            .color(ContextCompat.getColor(requireActivity(), R.color.twitter))
            .sizeDp(24)
        }
        3 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.connect_linkedIn)
          holder.itemBinding.aboutAppSubtitle.visibility = View.GONE
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_linkedin_box)
            .color(ContextCompat.getColor(requireActivity(), R.color.linkedin))
            .sizeDp(24)
        }
        4 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.fork_on_github)
          holder.itemBinding.aboutAppSubtitle.visibility = View.GONE
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_github_circle)
            .color(Color.BLACK)
            .sizeDp(24)
        }
        5 -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.more_apps)
          holder.itemBinding.aboutAppSubtitle.visibility = View.GONE
          holder.itemBinding.aboutRowItemImage.background = IconicsDrawable(requireActivity())
            .icon(CommunityMaterial.Icon.cmd_google_play)
            .color(ContextCompat.getColor(requireActivity(), R.color.play_store_green))
            .sizeDp(24)
        }
      }
    }

    override fun getItemCount(): Int {
      return 6
    }

    internal inner class ItemHolder(val itemBinding: FragmentAboutListItemTemplateBinding) :
      RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

      init {
        itemView.setOnClickListener(this)
      }

      override fun onClick(view: View) {
        when (adapterPosition) {
          0 -> return
          1 -> {
            val myProfile =
              Intent(Intent.ACTION_VIEW, Uri.parse("http://google.com/+arunkumar5592"))
            requireActivity().startActivity(myProfile)
          }
          2 -> {
            val twitterIntent =
              Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/arunkumar_9t2"))
            requireActivity().startActivity(twitterIntent)
          }
          3 -> {
            val linkedInIntent =
              Intent(Intent.ACTION_VIEW, Uri.parse("http://in.linkedin.com/in/arunkumar9t2"))
            requireActivity().startActivity(linkedInIntent)
          }
          4 -> {
            val github = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/arunkumar9t2/"))
            requireActivity().startActivity(github)
          }
          5 -> {
            startActivity(
              Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/dev?id=9082544673727889961")
              )
            )
          }
        }
      }
    }
  }

  internal inner class CreditsAdapter : RecyclerView.Adapter<CreditsAdapter.ItemHolder>() {

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int
    ): ItemHolder {
      return ItemHolder(
        FragmentAboutListItemTemplateBinding.inflate(
          LayoutInflater.from(activity),
          parent,
          false
        )
      )
    }

    private val patryk = "Patryk"
    private val max = "Max"
    private val beta = "Beta Testers"
    private val items = arrayListOf(
      patryk,
      max,
      beta
    )
    private val patrykProfileImg =
      "https://lh3.googleusercontent.com/hZdzG3b5epdGAOtQQgwSwBEeGqbIbQGg68lTD7Nvp2caLJ0CeIRksMII52Q8J6SwZbWcbFRCiNYg2ss=w384-h383-rw-no"
    private val maxImg =
      "https://lh3.googleusercontent.com/lJn5h7sLkNMBlQwbZsyZyPrp0JNv8woEtX0hLg1o1uLmMri1VkVN10DM2XJkI4owV5u5MS5ABPbQ4s4=s1024-rw-no"

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
      when (items[position]) {
        patryk -> {
          holder.itemBinding.aboutAppTitle.text = "Patryk Goworowski"
          holder.itemBinding.aboutAppSubtitle.setText(R.string.icon_design)
          GlideApp.with(holder.itemView.context).load(patrykProfileImg)
            .into(holder.itemBinding.aboutRowItemImage)
        }
        max -> {
          holder.itemBinding.aboutAppTitle.text = "Max Patchs"
          holder.itemBinding.aboutAppSubtitle.setText(R.string.illustrations_and_video)
          GlideApp.with(holder.itemView.context).load(maxImg).into(holder.itemBinding.aboutRowItemImage)
        }
        beta -> {
          holder.itemBinding.aboutAppTitle.setText(R.string.beta_testers)
          holder.itemBinding.aboutAppSubtitle.gone()
          holder.itemBinding.aboutRowItemImage.setImageDrawable(
            IconicsDrawable(requireActivity())
              .icon(CommunityMaterial.Icon.cmd_google_plus)
              .colorRes(R.color.md_red_700)
              .sizeDp(24)
          )
        }
      }
    }

    override fun getItemCount() = items.size

    internal inner class ItemHolder(val itemBinding: FragmentAboutListItemTemplateBinding) :
      RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

      init {
        itemView.setOnClickListener(this)
      }

      override fun onClick(view: View) {
        when (items[adapterPosition]) {
          patryk -> {
            requireActivity()
              .startActivity(
                Intent(
                  Intent.ACTION_VIEW,
                  Uri.parse("https://plus.google.com/+PatrykGoworowski")
                )
              )
          }
          max -> {
            requireActivity()
              .startActivity(
                Intent(
                  Intent.ACTION_VIEW,
                  Uri.parse("https://plus.google.com/+Windows10-tutorialsBlogspot")
                )
              )
          }
          beta -> {
            requireActivity()
              .startActivity(
                Intent(
                  Intent.ACTION_VIEW,
                  Uri.parse("https://plus.google.com/communities/109754631011301174504")
                )
              )
          }
        }
      }
    }
  }

  companion object {
    fun newInstance() = AboutFragment()
  }
}
