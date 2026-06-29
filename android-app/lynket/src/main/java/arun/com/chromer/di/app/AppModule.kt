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

package arun.com.chromer.di.app

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import arun.com.chromer.browsing.icons.DefaultWebsiteIconsProvider
import arun.com.chromer.browsing.icons.WebsiteIconsProvider
import arun.com.chromer.di.viewmodel.ViewModelModule
import arun.com.chromer.settings.Preferences
import arun.com.chromer.util.RxEventBus
import arun.com.chromer.util.viemodel.ViewModelFactory
import com.afollestad.rxkprefs.rxkPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arunkumar.android.dagger.viewmodel.DefaultViewModelsBuilder
import javax.inject.Singleton

@Module(
  includes = [
    ViewModelModule::class,
    DefaultViewModelsBuilder::class
  ]
)
open class AppModule {
  /**
   * Legacy Dagger graph bridge for the in-progress Dagger->Hilt migration (PR176).
   * Some @HiltViewModels (e.g. HomeActivityViewModel, BrowsingViewModel) inject
   * `@ApplicationContext Application`; when they are also reachable from the legacy
   * @Component (AppComponent), that graph only @BindsInstance's the *unqualified* Application.
   * Bridge the Hilt-qualified binding to the unqualified instance so the legacy graph also links.
   */
  @Provides
  @Singleton
  @ApplicationContext
  internal fun providesApplicationContextApplication(application: Application): Application = application

  @Provides
  @Singleton
  internal fun providesPreferences(application: Application): Preferences =
    Preferences.get(application)

  @Provides
  @Singleton
  internal fun providersRxkprefs(application: Application) = rxkPrefs(application)

  @Provides
  @Singleton
  internal fun rxEventBus(): RxEventBus = RxEventBus()

  @Provides
  @Singleton
  internal fun websiteIconProvider(defaultWebsiteIconsProvider: DefaultWebsiteIconsProvider): WebsiteIconsProvider {
    return defaultWebsiteIconsProvider
  }

  @Provides
  @Singleton
  internal fun viewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory {
    return factory
  }
}
