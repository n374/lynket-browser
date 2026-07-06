/*
 * Lynket
 *
 * Copyright (C) 2019 Arunkumar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package arun.com.chromer.di.app;

import android.app.Application;

import javax.inject.Singleton;

import arun.com.chromer.di.viewmodel.ViewModelModule;
import dagger.Module;
import dagger.Provides;

/**
 * Test counterpart of {@link AppModule}. The production graph binds the {@link Application} via the
 * {@code @Component.Factory}'s {@code @BindsInstance} (see AppComponent); the test graph uses the
 * builder style, so we provide the Application from this module instead. Previously this relied on
 * {@code super(application)} + an AppModule Application provider that no longer exist, which broke
 * the unit-test Dagger graph ("Application cannot be provided").
 */
@Module(includes = ViewModelModule.class)
public class TestAppModule extends AppModule {

  private final Application application;

  public TestAppModule(Application application) {
    this.application = application;
  }

  @Provides
  @Singleton
  Application provideApplication() {
    return application;
  }
}
