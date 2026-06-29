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
package dev.arunkumar.android

import dagger.Module
import dagger.Provides
import dev.arunkumar.android.rxschedulers.SchedulerProvider
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Singleton

/**
 * Dagger module that provides RxJava schedulers for the application.
 */
@Module
class AppSchedulersModule {
  @Provides
  @Singleton
  fun provideSchedulerProvider(): SchedulerProvider = object : SchedulerProvider {
    override val ui: Scheduler
      get() = AndroidSchedulers.mainThread()

    override val io: Scheduler
      get() = Schedulers.io()

    override val computation: Scheduler
      get() = Schedulers.computation()

    override val pool: Scheduler
      get() = Schedulers.io()
  }
}
