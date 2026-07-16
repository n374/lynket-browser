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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Layer-1 source-scan guard for RAS-54 (design §6). Pure JVM, no Robolectric — Robolectric 4.3
 * caps at SDK 28 and cannot reproduce the API-31 `PendingIntent` crash, so this test locks in the
 * *decision* at the source level instead: every crash-prone `PendingIntent` creation point must go
 * through [PendingIntents], and — critically — each point must pick the correct mutability.
 *
 * Why the per-point mapping matters: naively making everything `FLAG_IMMUTABLE` also stops the
 * crash, so a "does it contain some flag" check would pass while silently breaking every Custom Tab
 * action button / menu / bottom bar (the browser's URL fill-in is dropped). This test is the main
 * guard against that "crash gone but function silently dead" correctness defect, and against future
 * regressions that add a bare `PendingIntent.get*`.
 */
class PendingIntentFlagGuardTest {

  // Statement ends with ';', so [^;]* stays within one Java statement — anchors var → wrapper.
  private fun mutablePoint(varName: String) =
    Regex("""$varName\s*=\s*PendingIntent\.get\w+\([^;]*PendingIntents\.mutable\(""")

  private fun immutablePoint(varName: String) =
    Regex("""$varName\s*=\s*PendingIntent\.get\w+\([^;]*PendingIntents\.immutable\(""")

  private val rawGet = Regex("""PendingIntent\.get(Activity|Broadcast|Service)\(""")
  private val anyMutable = Regex("""PendingIntents\.mutable\(""")
  private val anyImmutable = Regex("""PendingIntents\.immutable\(""")

  /** Collapse newlines/indentation so multi-line calls (e.g. the moreMenu getActivity) match. */
  private fun normalized(relPath: String): String {
    val file = resolve(relPath)
    // Strip the static import line so it isn't mistaken for a creation point / bare flag.
    val body = file.readText().lineSequence()
      .filterNot { it.trimStart().startsWith("import ") }
      .joinToString("\n")
    return body.replace(Regex("""\s+"""), " ")
  }

  @Test
  fun customTabs_everyPointWrapped_andMappedCorrectly() {
    val src = normalized(CUSTOM_TABS)

    // 1:1 wrapping — no bare PendingIntent.get* survives.
    val raw = rawGet.findAll(src).count()
    val wrapped = anyMutable.findAll(src).count() + anyImmutable.findAll(src).count()
    assertEquals("Every PendingIntent.get* in CustomTabs must be wrapped by PendingIntents.*", raw, wrapped)
    assertEquals("CustomTabs must have exactly 10 creation points", 10, raw)
    assertEquals("CustomTabs must have 9 mutable points", 9, anyMutable.findAll(src).count())
    assertEquals("CustomTabs must have 1 immutable point (minimize)", 1, anyImmutable.findAll(src).count())

    // No bare FLAG_UPDATE_CURRENT — every flag literal must sit inside a PendingIntents.* wrapper.
    assertNoBareFlag(src, CUSTOM_TABS)

    // Per-point mapping (the anti "all-immutable false green" guard).
    val mustBeMutable = listOf(
      "openBrowserPending", "favSharePending", "sharePending", "moreMenuPending",
      "pendingShareIntent", "pendingBrowseIntent", "serviceIntentPending",
      "openChromePending", "openWithActivityPending"
    )
    mustBeMutable.forEach { v ->
      assertTrue("$v must use PendingIntents.mutable(...) (browser fills the URL into intent data)",
        mutablePoint(v).containsMatchIn(src))
    }
    assertTrue("pendingMin (minimize) must use PendingIntents.immutable(...) — it reads the explicit extra",
      immutablePoint("pendingMin").containsMatchIn(src))

    // Explicit-component safety: every mutable target's base intent is new Intent(activity, X.class).
    listOf(
      "SecondaryBrowserReceiver", "FavShareBroadcastReceiver", "ShareBroadcastReceiver",
      "ChromerOptionsActivity", "CopyToClipboardReceiver", "OpenInChromeReceiver", "OpenIntentWithActivity"
    ).forEach { cls ->
      assertTrue("Mutable PendingIntent target $cls must be an explicit-component intent",
        src.contains("new Intent(activity, $cls.class)"))
    }
  }

  @Test
  fun bottomBar_singlePoint_isMutable() {
    val src = normalized(BOTTOM_BAR)
    assertEquals("BottomBarManager must have exactly 1 PendingIntent creation point", 1, rawGet.findAll(src).count())
    assertEquals("BottomBar point must be mutable (browser fills clicked-id + URL)", 1, anyMutable.findAll(src).count())
    assertEquals("BottomBar point must NOT be immutable", 0, anyImmutable.findAll(src).count())
    assertNoBareFlag(src, BOTTOM_BAR)
    assertTrue("BottomBar base intent must be explicit-component new Intent(context, BottomBarReceiver.class)",
      src.contains("new Intent(context, BottomBarReceiver.class)"))
  }

  @Test
  fun webHeadService_threeNotificationPoints_immutable_withSetPackage() {
    val src = normalized(WEBHEAD)
    assertEquals("WebHeadService must have exactly 3 PendingIntent creation points", 3, rawGet.findAll(src).count())
    assertEquals("All 3 WebHead points must be immutable (self-contained, implicit action)", 3, anyImmutable.findAll(src).count())
    assertEquals("WebHead must have 0 mutable points — implicit intents cannot be MUTABLE (throws on targetSdk>=34)",
      0, anyMutable.findAll(src).count())
    assertNoBareFlag(src, WEBHEAD)

    // Each of the three must scope its implicit broadcast to this package before going immutable.
    listOf("contentIntent", "contextActivity", "newTab").forEach { v ->
      val stmt = Regex("""$v\s*=\s*PendingIntent\.getBroadcast\([^;]*setPackage\(getPackageName\(\)\)[^;]*PendingIntents\.immutable\(""")
      assertTrue("$v must call setPackage(getPackageName()) before PendingIntents.immutable(...)",
        stmt.containsMatchIn(src))
    }
  }

  private fun assertNoBareFlag(src: String, label: String) {
    // Every FLAG_UPDATE_CURRENT literal must be the argument of a PendingIntents.mutable/immutable call.
    val allFlags = Regex("""FLAG_UPDATE_CURRENT""").findAll(src).count()
    val wrappedFlags = Regex("""PendingIntents\.(mutable|immutable)\((PendingIntent\.)?FLAG_UPDATE_CURRENT\)""")
      .findAll(src).count()
    assertEquals("$label: every FLAG_UPDATE_CURRENT must be inside a PendingIntents.* wrapper (no bare flag)",
      allFlags, wrappedFlags)
  }

  companion object {
    private const val PKG = "src/main/java/arun/com/chromer"
    private const val CUSTOM_TABS = "$PKG/browsing/customtabs/CustomTabs.java"
    private const val BOTTOM_BAR = "$PKG/browsing/customtabs/bottombar/BottomBarManager.java"
    private const val WEBHEAD = "$PKG/bubbles/webheads/WebHeadService.java"

    /**
     * Resolve a module-relative source path independent of the JUnit working directory: walk up
     * from user.dir until a base is found that actually contains the file.
     */
    private fun resolve(relPath: String): File {
      val userDir = System.getProperty("user.dir") ?: "."
      var dir: File? = File(userDir).absoluteFile
      while (dir != null) {
        val candidate = File(dir, relPath)
        if (candidate.isFile) return candidate
        val nested = File(dir, "lynket/$relPath")
        if (nested.isFile) return nested
        dir = dir.parentFile
      }
      fail("Could not locate source file $relPath from $userDir")
      error("unreachable")
    }
  }
}
