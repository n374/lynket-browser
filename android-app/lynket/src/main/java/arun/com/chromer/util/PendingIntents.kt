/*
 *
 *  Lynket
 *
 *  Copyright (C) 2026 Arunkumar
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

package arun.com.chromer.util

import android.app.PendingIntent
import android.os.Build

/**
 * Central helper for the mutability flag every [PendingIntent] must carry once `targetSdk >= 31`.
 *
 * Background (RAS-54): on Android 12 (API 31) with `targetSdk >= 31`, creating a `PendingIntent`
 * without explicitly passing either [PendingIntent.FLAG_IMMUTABLE] or [PendingIntent.FLAG_MUTABLE]
 * throws `IllegalArgumentException`. This project runs `targetSdk = 35`, so every creation point
 * crashes unless it goes through one of the two helpers below.
 *
 * The choice between the two is a correctness decision, not a formality:
 * - [immutable] — self-contained intents that need no external fill-in (they carry every extra they
 *   depend on). Safe and preferred by the platform.
 * - [mutable] — Custom Tabs action-button / menu / secondary-toolbar intents. The browser (Chrome
 *   etc.) fills the currently viewed URL into the intent's `data` (and the clicked view id for the
 *   secondary toolbar) at click time via `PendingIntent.send(..., fillInIntent)`. That fill-in is
 *   silently dropped for an immutable `PendingIntent`, so the receivers — which all read
 *   `getDataString()` — would get a null URL and the action would fail silently. These MUST be
 *   mutable. Callers must guarantee the base intent carries an explicit component (`new Intent(ctx,
 *   XxxReceiver.class)`), which is the security precondition for a mutable `PendingIntent`; every
 *   call site in this repo satisfies it.
 *
 * All 14 crash-prone creation points in `CustomTabs`, `BottomBarManager` and `WebHeadService` route
 * through here; the `PendingIntentFlagGuardTest` fails the build if any bare
 * `PendingIntent.get{Activity,Broadcast,Service}(...)` reappears in those files. See
 * `docs/changes/54-mode-crash-fix/design.md` §4/§5.2.
 */
object PendingIntents {

  /**
   * Adds [PendingIntent.FLAG_IMMUTABLE] to [base]. Use for self-contained intents that never rely
   * on the browser filling anything in. `FLAG_IMMUTABLE` exists since API 23 (`minSdk = 23`), so it
   * is always safe to `or` in.
   */
  @JvmStatic
  fun immutable(base: Int): Int = base or PendingIntent.FLAG_IMMUTABLE

  /**
   * Adds [PendingIntent.FLAG_MUTABLE] to [base] on API 31+. Use for Custom Tabs intents that must
   * receive the browser's URL / clicked-id fill-in.
   *
   * `FLAG_MUTABLE` was only introduced in API 31 (the constant compiles because `compileSdk = 31`),
   * so it is added only when `SDK_INT >= 31`. Below 31 a `PendingIntent` is implicitly mutable, so
   * omitting the flag preserves the exact pre-Android-12 behaviour.
   */
  @JvmStatic
  fun mutable(base: Int): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) base or PendingIntent.FLAG_MUTABLE else base
}
