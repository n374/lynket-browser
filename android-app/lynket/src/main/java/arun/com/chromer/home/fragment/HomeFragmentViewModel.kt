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

package arun.com.chromer.home.fragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arun.com.chromer.data.Result
import arun.com.chromer.data.history.HistoryRepository
import arun.com.chromer.data.website.model.Website
import dagger.hilt.android.lifecycle.HiltViewModel
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import javax.inject.Inject

/**
 * Legacy ViewModel for HomeFragment (XML-based UI).
 *
 * Migrated to Hilt: Uses @HiltViewModel annotation for automatic ViewModel injection.
 * Retains RxJava 1.x for now (will be migrated to Flows in future phase).
 *
 * Note: Modern Compose UI uses ModernHomeViewModel instead.
 * Most functionality is commented out but retained for backward compatibility.
 */
@HiltViewModel
class HomeFragmentViewModel
@Inject
constructor(
  private val historyRepository: HistoryRepository
) : ViewModel() {
  private val recentsLoaderSubject: PublishSubject<Int> = PublishSubject.create()

  val recentsResultLiveData = MutableLiveData<Result<List<Website>>>()

  val subs = CompositeSubscription()

  init {
    /* subs.add(recentsLoaderSubject.asObservable()
             .onBackpressureLatest()
             .concatMap {
                 historyRepository
                         .recents()
                         .compose(Result.applyToObservable())
                         .compose(SchedulerProvider.applyIoSchedulers())
             }
             .observeOn(AndroidSchedulers.mainThread())
             .subscribe { recentsResultLiveData.postValue(it) })*/
  }

  fun loadRecents() {
    recentsLoaderSubject.onNext(0)
  }

  override fun onCleared() {
    subs.clear()
  }
}
