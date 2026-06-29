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

package arun.com.chromer.home

import android.annotation.SuppressLint
import android.app.Application
import androidx.annotation.CallSuper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arun.com.chromer.R
import arun.com.chromer.data.history.HistoryRepository
import arun.com.chromer.data.website.model.Website
import arun.com.chromer.extenstions.StringResource
import arun.com.chromer.extenstions.appName
import arun.com.chromer.home.epoxycontroller.model.CustomTabProviderInfo
import arun.com.chromer.settings.Preferences
import arun.com.chromer.settings.RxPreferences
import arun.com.chromer.shared.Constants
import arun.com.chromer.util.glide.appicon.ApplicationIcon
import com.jakewharton.rxrelay2.PublishRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arunkumar.android.rxschedulers.SchedulerProvider
import dev.arunkumar.android.rxschedulers.asResource
import dev.arunkumar.android.rxschedulers.compose
import dev.arunkumar.android.rxschedulers.ioToUi
import dev.arunkumar.android.rxschedulers.poolToUi
import dev.arunkumar.android.common.Resource
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Legacy ViewModel for HomeActivity (XML-based UI).
 *
 * Migrated to Hilt: Uses @HiltViewModel annotation for automatic ViewModel injection.
 * Retains RxJava 2.x for now (will be migrated to Flows in future phase).
 *
 * Note: Modern Compose UI uses ModernHomeViewModel instead.
 */
@SuppressLint("CheckResource")
@HiltViewModel
class HomeActivityViewModel
@Inject
constructor(
  @ApplicationContext private val application: Application,
  private val rxPreferences: RxPreferences,
  private val schedulerProvider: SchedulerProvider,
  private val historyRepository: HistoryRepository,
  private val preferences: Preferences
) : ViewModel() {

  val providerInfoLiveData = MutableLiveData<CustomTabProviderInfo>()
  val recentsLiveData = MutableLiveData<Resource<List<Website>>>()

  /**
   * Relay to convert `onCleared` calls to data stream.
   *
   * @see onCleared
   */
  private val clearEventsRelay = PublishRelay.create<Int>()

  /**
   * [Observable] that emits `0` when onCleared is called.
   */
  @Suppress("MemberVisibilityCanBePrivate")
  private val clearEvents: Observable<Int> = clearEventsRelay.hide()

  init {
    start()
  }

  private fun start() {
    bindProviderInfo()
    bindRecentsInfo()
  }

  private fun bindRecentsInfo() {
    historyRepository.recents()
      .asResource()
      .compose(schedulerProvider.ioToUi())
      .subscribe(recentsLiveData::setValue)
  }

  private fun bindProviderInfo() {
    Observable.combineLatest(
      rxPreferences.customTabProviderPref.observe().map { packageName ->
        when {
          packageName.isEmpty() -> preferences.defaultCustomTabApp ?: ""
          else -> packageName
        }
      },
      rxPreferences.incognitoPref.observe(),
      rxPreferences.webviewPref.observe()
    ) { customTabProvider: String, isIncognito: Boolean, isWebView: Boolean ->
      if (customTabProvider.isEmpty() || isIncognito || isWebView) {
        CustomTabProviderInfo(
          iconUri = ApplicationIcon.createUri(Constants.SYSTEM_WEBVIEW),
          providerDescription = StringResource(
            R.string.tab_provider_status_message_home,
            resourceArgs = listOf(R.string.system_webview)
          ),
          providerReason = if (isIncognito)
            StringResource(R.string.provider_web_view_incognito_reason)
          else StringResource(0),
          allowChange = !isIncognito
        )
      } else {
        val appName = application.appName(customTabProvider)
        CustomTabProviderInfo(
          iconUri = ApplicationIcon.createUri(customTabProvider),
          providerDescription = StringResource(
            R.string.tab_provider_status_message_home,
            listOf(appName)
          ),
          providerReason = StringResource(0)
        )
      }
    }.compose(schedulerProvider.poolToUi<CustomTabProviderInfo>())
      .untilCleared()
      .subscribe(providerInfoLiveData::setValue)
  }

  /**
   * Auto terminates the current [Observable] when `onCleared` occurs.
   */
  protected fun <T> Observable<T>.untilCleared(): Observable<T> = compose { upstream ->
    upstream.takeUntil(clearEvents)
  }

  @CallSuper
  override fun onCleared() {
    clearEventsRelay.accept(0)
  }
}
