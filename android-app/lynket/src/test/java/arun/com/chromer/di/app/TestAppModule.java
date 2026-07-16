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

import arun.com.chromer.di.viewmodel.ViewModelModule;
import dagger.Module;

/**
 * Test variant of {@link AppModule}. Application is bound into the graph via
 * {@link TestAppComponent.Factory}'s {@code @BindsInstance} (mirroring prod {@code AppComponent}),
 * so this module no longer takes an Application in its constructor. The old
 * {@code TestAppModule(Application)} called a {@code super(application)} constructor that ceased to
 * exist when prod migrated {@code AppModule} to the {@code @Component.Factory} + {@code @BindsInstance}
 * pattern — that drift had left the whole unit-test source set uncompilable (RAS-55 fix).
 * (Merge note: main's interim fix kept the builder style with a local {@code @Provides Application};
 * superseded by this factory pattern to match the merged TestAppComponent/LynketTestApplication.)
 */
@Module(includes = ViewModelModule.class)
public class TestAppModule extends AppModule {
}
