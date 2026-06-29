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

package arun.com.chromer.search.view

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.speech.RecognizerIntent.EXTRA_RESULTS
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import androidx.recyclerview.widget.SimpleItemAnimator
import arun.com.chromer.R
import arun.com.chromer.databinding.WidgetMaterialSearchViewBinding
import arun.com.chromer.di.view.Detaches
import arun.com.chromer.di.view.ViewComponent
import arun.com.chromer.extenstions.gone
import arun.com.chromer.search.suggestion.SuggestionController
import arun.com.chromer.search.suggestion.items.SuggestionItem.HistorySuggestionItem
import arun.com.chromer.search.suggestion.items.SuggestionType.*
import arun.com.chromer.shared.Constants.REQUEST_CODE_VOICE
import arun.com.chromer.shared.base.ProvidesActivityComponent
import arun.com.chromer.util.Utils
import arun.com.chromer.util.animations.spring
import arun.com.chromer.util.epoxy.intercepts
import arun.com.chromer.util.glide.GlideApp
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.focusChanges
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.jakewharton.rxbinding3.widget.textChanges
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import dev.arunkumar.android.rxschedulers.SchedulerProvider
import dev.arunkumar.android.rxschedulers.compose
import dev.arunkumar.android.rxschedulers.poolToUi
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

@SuppressLint("CheckResult")
class MaterialSearchView
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

  private var viewComponent: ViewComponent? = null

  private val binding: WidgetMaterialSearchViewBinding = WidgetMaterialSearchViewBinding.inflate(
    LayoutInflater.from(context),
    this,
    true
  )

  private val normalColor: Int = context.getColor(R.color.accent_icon_no_focus)
  private val focusedColor: Int = context.getColor(R.color.colorAccent)

  private val xIcon: IconicsDrawable by lazy {
    IconicsDrawable(context)
      .icon(CommunityMaterial.Icon.cmd_close)
      .color(normalColor)
      .sizeDp(16)
  }
  private val voiceIcon: IconicsDrawable by lazy {
    IconicsDrawable(context)
      .icon(CommunityMaterial.Icon.cmd_microphone)
      .color(normalColor)
      .sizeDp(18)
  }
  private val menuIcon: IconicsDrawable by lazy {
    IconicsDrawable(context)
      .icon(CommunityMaterial.Icon.cmd_menu)
      .color(normalColor)
      .sizeDp(18)
  }

  @Inject
  lateinit var searchPresenter: SearchPresenter

  @Inject
  lateinit var schedulerProvider: SchedulerProvider

  @Inject
  lateinit var suggestionController: SuggestionController

  @Inject
  @Detaches
  lateinit var viewDetaches: Observable<Unit>

  private val voiceSearchFailed = PublishSubject.create<Any>()
  private val searchPerformed = PublishSubject.create<String>()
  private val focusChanges = BehaviorSubject.createDefault(false)

  private val searchQuery get() = if (binding.msvEditText.text == null) "" else binding.msvEditText.text.toString()

  private val searchTermChanges by lazy {
    binding.msvEditText.textChanges()
      .skipInitialValue()
      .takeUntil(viewDetaches)
      .share()
  }

  val editText: EditText get() = binding.msvEditText

  fun voiceSearchFailed(): Observable<Any> = voiceSearchFailed.hide()

  fun searchPerforms(): Observable<String> = searchPerformed
    .hide()
    .switchMap(searchPresenter::getSearchUrl)

  private val leftIconClicks by lazy { binding.msvLeftIcon.clicks().share() }

  init {

    if (context is ProvidesActivityComponent) {
      viewComponent = context
        .activityComponent
        .viewComponentFactory().create(this)
        .also { component -> component.inject(this) }
    }

    binding.searchSuggestions.apply {
      (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
      layoutManager = GridLayoutManager(context, 4, VERTICAL, true)
      setController(suggestionController)
      clipToPadding = true
    }
  }


  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    setOnClickListener { if (!binding.msvEditText.hasFocus()) gainFocus() }
    setupEditText()
    setupLeftIcon()
    setupVoiceIcon()

    binding.msvClearIcon.clicks().subscribe { binding.msvEditText.text = null }

    setupSuggestionController()
    setupPresenter()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    viewComponent = null
  }

  override fun clearFocus() {
    clearFocus(null)
  }

  override fun hasFocus() = binding.msvEditText.hasFocus() && super.hasFocus()

  fun focusChanges(): Observable<Boolean> = focusChanges.hide()

  fun gainFocus() {
    handleIconsState()
    setFocusedColor()
    focusChanges.onNext(true)
  }

  fun loseFocus(endAction: (() -> Unit)? = null) {
    setNormalColor()
    binding.msvEditText.text = null
    hideKeyboard()
    hideSuggestions()
    focusChanges.onNext(false)
    handleIconsState()
    endAction?.invoke()
  }

  override fun setOnClickListener(onClickListener: OnClickListener?) = Unit

  fun menuClicks(): Observable<Unit> = leftIconClicks
    .filter { focusChanges.value == false }
    .share()

  fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CODE_VOICE) {
      when (resultCode) {
        RESULT_OK -> {
          val resultList = data?.getStringArrayListExtra(EXTRA_RESULTS)
          if (resultList != null && resultList.isNotEmpty()) {
            searchPerformed(resultList.first())
          }
        }
      }
    }
  }

  private fun setupLeftIcon() {
    class CompositeIconResource(val drawable: Drawable? = null, val uri: Uri? = null) {
      fun apply(view: ImageView) {
        when {
          drawable != null -> view.setImageDrawable(drawable)
          uri != null -> GlideApp.with(view).load(uri).into(view)
        }
      }
    }
    binding.msvLeftIcon.run {
      setImageDrawable(menuIcon)
      leftIconClicks
        .filter { focusChanges.value == true }
        .takeUntil(viewDetaches).subscribe {
          suggestionController.showSearchProviders = true
        }
      Observable.combineLatest(
        focusChanges,
        searchPresenter.selectedSearchProvider
      ) { hasFocus, searchProvider ->
        if (hasFocus) {
          CompositeIconResource(uri = searchProvider.iconUri)
        } else {
          CompositeIconResource(drawable = menuIcon)
        }
      }.compose(schedulerProvider.poolToUi())
        .takeUntil(viewDetaches)
        .subscribe { iconResource -> iconResource.apply(this) }
    }
    searchTermChanges.subscribe {
      suggestionController.showSearchProviders = false
    }
  }

  private fun setupVoiceIcon() {
    binding.msvVoiceIcon.run {
      setImageDrawable(voiceIcon)
      setOnClickListener {
        if (searchQuery.isNotEmpty()) {
          binding.msvEditText.setText("")
          clearFocus()
        } else {
          if (Utils.isVoiceRecognizerPresent(context)) {
            (context as Activity).startActivityForResult(
              Utils.getRecognizerIntent(context),
              REQUEST_CODE_VOICE
            )
          } else {
            voiceSearchFailed.onNext(Any())
          }
        }
      }
    }
  }

  private fun setupEditText() {
    binding.msvEditText.run {
      focusChanges()
        .takeUntil(viewDetaches)
        .subscribe { hasFocus ->
          if (hasFocus) {
            gainFocus()
          } else {
            loseFocus()
          }
        }
      editorActionEvents { event -> event.actionId == IME_ACTION_SEARCH }
        .map { searchQuery }
        .observeOn(schedulerProvider.ui)
        .takeUntil(viewDetaches)
        .subscribe(::searchPerformed)
      searchTermChanges
        .takeUntil(viewDetaches)
        .observeOn(schedulerProvider.ui)
        .subscribe { handleIconsState() }
    }
  }

  private fun setupPresenter() {
    searchPresenter.run {
      registerSearch(searchTermChanges.map { it.toString() })

      suggestions.takeUntil(viewDetaches)
        .observeOn(schedulerProvider.ui)
        .subscribe(::setSuggestions)

      searchEngines.takeUntil(viewDetaches)
        .observeOn(schedulerProvider.ui)
        .subscribe { searchProviders ->
          suggestionController.searchProviders = searchProviders
        }

      registerSearchProviderClicks(suggestionController.searchProviderClicks)
    }
  }

  private fun setupSuggestionController() {
    suggestionController.intercepts()
      .map { it.isEmpty() }
      .observeOn(schedulerProvider.ui)
      .takeUntil(viewDetaches)
      .subscribe { isEmpty ->
        binding.searchSuggestions.gone(isEmpty)
        if (!isEmpty) {
          binding.searchSuggestions.scrollToPosition(0)
        }
      }

    suggestionController.suggestionClicks
      .observeOn(schedulerProvider.pool)
      .map { suggestionItem ->
        when (suggestionItem) {
          is HistorySuggestionItem -> suggestionItem.subTitle
          else -> suggestionItem.title
        } ?: ""
      }.filter { it.isNotEmpty() }
      .observeOn(schedulerProvider.ui)
      .takeUntil(viewDetaches)
      .subscribe(::searchPerformed)

    suggestionController.suggestionLongClicks
      .filter { it.title.isNotEmpty() }
      .takeUntil(viewDetaches)
      .subscribe {
        binding.msvEditText.setText(it.title)
        binding.msvEditText.setSelection(it.title.length)
      }
  }

  private fun clearFocus(endAction: (() -> Unit)?) {
    loseFocus(endAction)
    val view = findFocus()
    view?.clearFocus()
    super.clearFocus()
  }


  private fun hideKeyboard() {
    (context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
      windowToken,
      0
    )
  }

  private fun setFocusedColor() {
    binding.msvLeftIcon.setImageDrawable(menuIcon.color(focusedColor))
    binding.msvClearIcon.setImageDrawable(menuIcon.color(focusedColor))
    binding.msvVoiceIcon.setImageDrawable(voiceIcon.color(focusedColor))
  }

  private fun setNormalColor() {
    binding.msvLeftIcon.setImageDrawable(menuIcon.color(normalColor))
    binding.msvClearIcon.setImageDrawable(menuIcon.color(normalColor))
    binding.msvVoiceIcon.setImageDrawable(voiceIcon.color(normalColor))
  }

  private fun handleIconsState() {
    val color = if (binding.msvEditText.hasFocus()) focusedColor else normalColor
    if (searchQuery.isNotEmpty()) {
      binding.msvClearIcon.run {
        setImageDrawable(xIcon.color(color))
        spring(SpringAnimation.ALPHA).animateToFinalPosition(1F)
      }
      binding.msvVoiceIcon.setImageDrawable(voiceIcon.color(color))
    } else {
      binding.msvClearIcon.run {
        setImageDrawable(xIcon.color(color))
        spring(SpringAnimation.ALPHA).animateToFinalPosition(0F)
      }
      binding.msvVoiceIcon.setImageDrawable(voiceIcon.color(color))
    }
  }

  private fun searchPerformed(searchQuery: String) {
    Timber.d("Search performed : $searchQuery")
    clearFocus { searchPerformed.onNext(searchQuery) }
  }

  private fun hideSuggestions() {
    suggestionController.clear()
  }

  private fun setSuggestions(suggestionResult: SuggestionResult) {
    suggestionController.query = suggestionResult.query.trim()
    val suggestion = suggestionResult.suggestions
    return when (suggestionResult.suggestionType) {
      COPY -> suggestionController.copySuggestions = suggestion
      GOOGLE -> suggestionController.googleSuggestions = suggestion
      HISTORY -> suggestionController.historySuggestions = suggestion
    }
  }
}
