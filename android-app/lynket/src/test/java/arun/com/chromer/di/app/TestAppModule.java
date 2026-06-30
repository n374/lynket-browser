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
 * Test variant of {@link AppModule}. After the modernization, {@link AppModule} no longer takes an
 * {@code Application} in its constructor (the production graph @BindsInstance's it via the component
 * factory). The test graph is built via this module, so it both inherits AppModule's @Provides
 * bindings and supplies the {@code Application} instance the rest of the graph depends on.
 */
@Module(includes = ViewModelModule.class)
public class TestAppModule extends AppModule {

  private final Application application;

  public TestAppModule(Application application) {
    this.application = application;
  }

  @Provides
  @Singleton
  Application application() {
    return application;
  }
}
