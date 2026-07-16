<!-- doc-init template version: v1.0 -->
# Design: 54-mode-crash-fix

- **Owner**: 技术方案官 on behalf of n374 (wu.nerd@gmail.com)
- **Reviewers**: 开发官, n374, Codex（对抗式交叉评审）
- **状态**: Plan（含 §12 补充设计：Web Heads 第二处崩溃 / API 34 前台服务类型；已交叉评审，待开发官落地）
- **创建日期**: 2026-07-16
- **最后更新**: 2026-07-16（补 §12）
- **关联 Issue**: RAS-54「LB Bug 修复」
- **仓库 / 基线分支**: `n374/lynket-browser` @ `fix/54-mode-crash-fix`（HEAD 基线 `a57031b1`，远端分支 tip `189c167c`）
- **上游**: [proposal.md](./proposal.md) · [specs/link-open-modes/spec.md](./specs/link-open-modes/spec.md)

---

## 1. 概述与核心结论

一句话：修复 Slide Over / Web Heads 两模式在 Android 12+ 的崩溃，根因是 `targetSdk=35` 下这两条链路创建 `PendingIntent` 未带可变性标志。**但关键不是"统一补 `FLAG_IMMUTABLE`"——本设计经代码追证 + AndroidX 契约确认：这批崩溃点里 10 处必须用 `FLAG_MUTABLE`（Custom Tabs 由浏览器回填当前 URL / 被点击视图 id），只有 4 处该用 `FLAG_IMMUTABLE`。** 若无差别地全打 `FLAG_IMMUTABLE`，崩溃会消失但 Custom Tab 的操作按钮 / 菜单 / 底栏会静默失效（收不到 URL），构成正确性缺陷。

> ⚠️ **对 proposal §8 缓解措施的更正**：proposal 里"默认 `FLAG_IMMUTABLE`，确有需可变的再用 `FLAG_MUTABLE`"的取向是**反的**——实际多数崩溃点需要 `FLAG_MUTABLE`。本设计 §4 给出逐点权威分类，取代该默认取向。

---

## 2. 背景与目标

- **目标**：让 Slide Over（Custom Tabs）与 Web Heads（悬浮气泡）在 Android 12+ 打开链接**不崩溃且功能正常**（S1/S2）；Native Bubbles 无回归（S3）；崩溃点具 UT 覆盖（S4）；模拟器验证通过为合并门槛（S5）。
- **非目标**（继承 proposal §3）：不做原生气泡复用外部浏览器登录态（已拆 RAS-55）；不改 `targetSdk`（保持 35）；不做架构重构；不动 `BubbleNotificationManager.kt`（已正确）。

---

## 3. 根因与机制（已代码 + 官方契约双证）

### 3.1 崩溃机制
`targetSdk ≥ 31`（本项目 35，`Constants.kt:36/43` 为 `compileSdk=31 / targetSdk=35`）时，创建 `PendingIntent` 未显式指定 `FLAG_IMMUTABLE` 或 `FLAG_MUTABLE` 会抛 `IllegalArgumentException`。Slide Over 的 `CustomTabs.launch()` 与 Web Heads 的 `WebHeadService.onCreate()→startForeground()` 都在打开/启动的**必经路径**上创建这类 `PendingIntent`，故一触即崩。

### 3.2 为什么"统一 IMMUTABLE"是错的（正确性核心）
Custom Tabs 的操作按钮 / 菜单项 / 二级工具栏交给浏览器（Chrome 等）承载。浏览器在用户点击时通过 `PendingIntent.send(context, 0, fillInIntent)` **回填当前页 URL 到 intent 的 `data` 字段**（二级工具栏还额外回填被点击视图 id `EXTRA_REMOTEVIEWS_CLICKED_ID`）。回填要生效，`PendingIntent` 必须 `FLAG_MUTABLE`；用 `FLAG_IMMUTABLE` 则回填被丢弃。

- **代码内铁证**：本仓所有对应接收方都读 `intent.getDataString()` / `intent.data`（如 `SecondaryBrowserReceiver`、`ShareBroadcastReceiver`、`CopyToClipboardReceiver`、`OpenInChromeReceiver`、`FavShareBroadcastReceiver`、`ChromerOptionsActivity`），而 App 侧**从不给这些 intent 设 data**。Android 12 之前它们能工作，正是因为当时 `PendingIntent` 隐式可变、浏览器得以回填 URL。
- **官方契约**：AndroidX `CustomTabsIntent` / Chrome 官方 Custom Tabs 指南明确 `setActionButton` / `setSecondaryToolbarViews` 的 `PendingIntent` 须用 `FLAG_MUTABLE`，且要求其内嵌 intent 带**显式组件**以保证安全——本仓这些 intent 均 `new Intent(ctx, XxxReceiver.class)`，天然满足，无安全风险。

结论：**IMMUTABLE 只适用于"自包含、无需回填"的 `PendingIntent`；凡依赖浏览器回填 URL/点击 id 的，必须 MUTABLE。**

---

## 4. 关键变更点：逐点可变性分类（全仓审计，穷尽）

全仓审计（`grep -rE 'PendingIntent\.(getActivity|getBroadcast|getService)'`）命中 3 个待修文件共 **14 个创建点**；第 15 点 `BubbleNotificationManager.kt:128` 已正确（对照，勿动）。

| # | 位置 | 接收方及取值方式 | 应用 flag | 依据 |
|---|---|---|---|---|
| 1 | `CustomTabs.java:364` openBrowserPending | `SecondaryBrowserReceiver` 读 `getDataString()` | **MUTABLE** | 浏览器回填 URL |
| 2 | `CustomTabs.java:373` favSharePending | `FavShareBroadcastReceiver` 读 `getDataString()` | **MUTABLE** | 同上 |
| 3 | `CustomTabs.java:383` sharePending | `ShareBroadcastReceiver` 读 `getDataString()` | **MUTABLE** | 同上 |
| 4 | `CustomTabs.java:409` moreMenuPending | `ChromerOptionsActivity` 读 `intent.data`/`dataString`（:177/192/209） | **MUTABLE** | 同上（虽也放了 extra，但活动实际读 data） |
| 5 | `CustomTabs.java:424` pendingMin | `MinimizeBroadcastReceiver` 读 `getStringExtra(EXTRA_KEY_ORIGINAL_URL)`（:423 已显式设置） | **IMMUTABLE** | 不依赖回填 |
| 6 | `CustomTabs.java:442` pendingShareIntent | `FavShareBroadcastReceiver` 读 `getDataString()` | **MUTABLE** | 浏览器回填 URL |
| 7 | `CustomTabs.java:453` pendingBrowseIntent | `SecondaryBrowserReceiver` 读 `getDataString()` | **MUTABLE** | 同上 |
| 8 | `CustomTabs.java:466` serviceIntentPending（copy link） | `CopyToClipboardReceiver` 读 `getDataString()`（**忽略了 :465 设的 extra**） | **MUTABLE** | 同上 |
| 9 | `CustomTabs.java:481` openChromePending | `OpenInChromeReceiver` 读 `getDataString()` | **MUTABLE** | 同上 |
| 10 | `CustomTabs.java:493` openWithActivityPending | `OpenIntentWithActivity` 读 `getDataString()` | **MUTABLE** | 同上（`prepareOpenWith()` 当前被注释未调用，dead code，仍应一并修以防复启即崩） |
| 11 | `BottomBarManager.java:130` getOnClickPendingIntent | `BottomBarReceiver` 读 `EXTRA_REMOTEVIEWS_CLICKED_ID` + `dataString` | **MUTABLE** | 二级工具栏，回填点击 id + URL；immutable 会导致 `clickedId==-1` 直接 return，底栏全失效 |
| 12 | `WebHeadService.java:206` contentIntent | 隐式 `new Intent(ACTION_STOP_WEBHEAD_SERVICE)`（action=`"close_service"`，泛化），自包含 | **IMMUTABLE + `setPackage`** | 无回填；隐式 broadcast 应限定本包（见下方安全说明） |
| 13 | `WebHeadService.java:207` contextActivity | 隐式 `ACTION_OPEN_CONTEXT_ACTIVITY`，自包含 | **IMMUTABLE + `setPackage`** | 同上 |
| 14 | `WebHeadService.java:208` newTab | 隐式 `ACTION_OPEN_NEW_TAB`，自包含 | **IMMUTABLE + `setPackage`** | 同上 |
| — | `BubbleNotificationManager.kt:128` | 已 `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE` | 勿动 | 对照基准 |

**合计**：MUTABLE 10 处（1–4、6–11），IMMUTABLE 4 处（5、12–14）。所有 MUTABLE 点内嵌 intent 均带显式组件（`new Intent(ctx, XxxReceiver.class)`），满足"mutable PendingIntent 必须带显式组件"的安全前置。

> **安全补强（第 12–14 点，评审采纳）**：这三处的 base intent 是**泛化 action 的隐式 broadcast**（`"close_service"` 等），当前无 `setPackage`/`setComponent`。虽然对应接收方已 `RECEIVER_NOT_EXPORTED`（`WebHeadService.java:584`）使外部 App 收不到，但 `FLAG_IMMUTABLE` 只禁止 fill-in、不限定广播解析范围。**要求在设 `FLAG_IMMUTABLE` 前对三处 intent 追加 `.setPackage(getPackageName())`**，把隐式广播锁定本包，消除歧义并满足 Android 14+ 的隐式 intent lint。**注意隐式 intent 不能用 `FLAG_MUTABLE`**——`FLAG_MUTABLE + 隐式 Intent` 自 **Android 14 / API 34（targetSdk U+）** 起会抛异常，本项目 targetSdk=35 命中，故这三处只能 IMMUTABLE。

> 其余每处保留原有 `FLAG_UPDATE_CURRENT`，仅在其上 `or` 追加对应可变性标志（Kotlin `or` / Java `|`）。

---

## 5. 实现方案

### 5.1 修改落点
1. **`CustomTabs.java`**（Slide Over 主链路）：第 1–4、6–10 点按 §4 走 `PendingIntents.mutable(...)`（含 dead-code 的 :493）；第 5 点（minimize）走 `PendingIntents.immutable(...)`。
2. **`BottomBarManager.java:130`**：走 `PendingIntents.mutable(...)`。
3. **`WebHeadService.java:206–208`**：三处**先对 base intent `setPackage(getPackageName())`**，再走 `PendingIntents.immutable(...)`（见 §4 安全补强）。

### 5.2 集中助手（强制，评审采纳）
逐点内联易漏改/选错，且难以用测试锁定"每点选对 immutable/mutable"。**强制引入集中助手** `arun.com.chromer.util.PendingIntents`，所有 14 个创建点一律经它取 flag，不允许再出现裸 `FLAG_UPDATE_CURRENT`/裸 `FLAG_*` 常量：
  ```kotlin
  object PendingIntents {
    /** 自包含 intent（无需外部回填）用。FLAG_IMMUTABLE since API 23 (minSdk=23). */
    fun immutable(base: Int): Int = base or PendingIntent.FLAG_IMMUTABLE
    /** 需被浏览器回填 URL/点击 id 的 Custom Tabs intent 用。FLAG_MUTABLE since API 31；
     *  <31 隐式可变，无需追加。调用方必须保证 base intent 带显式组件（本仓均满足）。 */
    fun mutable(base: Int): Int =
      if (Build.VERSION.SDK_INT >= 31) base or PendingIntent.FLAG_MUTABLE else base
  }
  ```
- `FLAG_IMMUTABLE`：API 23 引入，`minSdk=23`，可直接 `or`。
- `FLAG_MUTABLE`：**API 31 才引入**，`compileSdk=31` 下常量可用；按 `SDK_INT >= 31` 条件追加，<31 不加（旧系统本就隐式可变，行为不变）。
- 助手为强制的原因：它是 §6 层二"逐点映射断言"与层一守卫的落点——没有统一入口，测试无法可靠地区分"某点该 mutable 却写成 immutable"。

### 5.3 不做
- 不改 `targetSdk` / `compileSdk` / AGP。
- 不改任何接收方逻辑（它们读 `getDataString()` 的现状恰是 MUTABLE 的理由，改动会扩大风险面）。
- 不重构 Custom Tabs / WebHeads 其它逻辑。

---

## 6. 测试策略（正确性导向，诚实对齐工具能力）

> **工具约束（关键）**：基线单测栈是 **Robolectric 4.3，最高只支持 SDK 28**（见 `LynketRobolectricSuite` 注释 `@Config(sdk=[23,28])`）。因此 **UT 无法在 SDK 31 真实框架里复现那个 `IllegalArgumentException`**——真实崩溃复现属于模拟器（S5），UT 只能断言"传给 `PendingIntent` 的 flag 正确"。测试设计据此分三层：

### 层一 · 源码扫描守卫测试，含逐点映射断言（对齐 S4 / Requirement-3，必做，评审强化）
一个纯 JVM 测试，读取 `CustomTabs.java`、`BottomBarManager.java`、`WebHeadService.java` 源码，做**两级**断言：
1. **穷尽性**：三文件内无任何裸 `PendingIntent.get{Activity,Broadcast,Service}(...)`——每个创建点都必须经 `PendingIntents.mutable(...)` / `PendingIntents.immutable(...)`。防漏改、防未来新增崩溃点。
2. **逐点映射（关键，防"全 IMMUTABLE 假绿"）**：**仅"含某个 flag"不够**，必须断言每点选对了 mutable/immutable——
   - 必须 `mutable(...)`：`CustomTabs.java` 的 openBrowser/favShare/share/moreMenu/pendingShare/pendingBrowse/copyLink/openChrome/openWith 九点 + `BottomBarManager.getOnClickPendingIntent`；
   - 必须 `immutable(...)`：`CustomTabs.java` 的 minimize + `WebHeadService` 三点；
   - 断言所有 `mutable(...)` 点的 base intent 带显式组件（`new Intent(<ctx>, <X>.class)`），且三个 WebHead 点在 `immutable(...)` 前调用了 `setPackage(...)`。

   实现可用锚点标记（如在助手调用旁加稳定标识/或按变量名+方法名匹配）使断言稳健。**这是拦住"消崩溃但功能静默失效"正确性缺陷的主守卫**。

### 层二 · 行为级 flag 断言测试（Robolectric `@Config sdk=[23]` + `ShadowPendingIntent`）
- **助手逻辑测试**（覆盖 SDK 分支）：用 `ReflectionHelpers.setStaticField(Build.VERSION::class, "SDK_INT", 31)` 断言 `PendingIntents.mutable(0) == FLAG_MUTABLE`、`SDK_INT=23` 时 `== 0`；`immutable(0) == FLAG_IMMUTABLE`。这是唯一能在不升 Robolectric 前提下验证"API 31 行为"的路径。
- **静态可直接调用点**：`BottomBarManager.getOnClickPendingIntent(ctx, url)` 是 public static → 直接调用，`shadowOf(pi).getFlags()` 断言**未含 `FLAG_IMMUTABLE`**（即可变）；`WebHeadService` 通知的三个 `PendingIntent` 断言**含 `FLAG_IMMUTABLE`**（若通知构建方法可在 Robolectric 下触达，否则退到层一逐点映射覆盖）。
- CustomTabs 私有方法不易直连；靠"助手逻辑测试 + 层一逐点映射守卫"联合锁定其正确性——层一逐点映射是 CustomTabs 正确性的实际保证，不能省。

### 层三 · 模拟器端到端验证（S1/S2/S3/S5，开发/验收阶段，本 runtime 无 SDK 无法执行）
在 **Android 12+（API ≥ 31）AVD**：
1. **Slide Over**：默认模式设 Slide Over → 打开链接 → 断言不崩溃 + Custom Tab 打开；**并点操作按钮/菜单（如"复制链接""在浏览器打开"）验证 URL 真的被带到**——即验证 MUTABLE 回填生效，而非只看"没崩"。这是把正确性闭环的关键断言。
2. **Web Heads**：授悬浮窗权限 → 打开链接 → 断言悬浮气泡出现、前台服务启动、不崩，通知内动作可点。
3. **Native Bubbles 回归**：Android 10+ 触发原生气泡打开链接冒烟，无退化。
4. 记录修复前/后 logcat 崩溃栈对照（NFR-1）。

> **对模拟器阶段的硬约束**：验收断言必须包含"操作按钮/菜单/底栏点击后 URL 到达接收方"，**不得只断言"App 未崩溃"**——否则会放过"IMMUTABLE 消崩溃但功能静默失效"的正确性缺陷。

---

## 7. 基线 / 分支与工具链决策

- **分支决策：留在 `fix/54-mode-crash-fix`（基线 `a57031b1`），不切 `main-consolidated`。** 原因：proposal §8 担心的"单测基建缺失、可能需从 `main-consolidated` 移植 `f5c395fa`/`b037698d`"——经核查**基线已含完整 Robolectric 基建**（`LynketTestApplication`、`LynketRobolectricSuite`、`TestAppComponent/TestAppModule`、`WebViewConfiguratorTest`、`DefaultTabsManagerTest` 等，均已 `git ls-files` 在案，即 spike 修复已合入主线）。故**该风险基本关闭**，无需 rebase 到落后 48 提交且未推送的 `main-consolidated`。
- **工具链已验证兼容**：本机以 **JDK 11 + Gradle 7.4.2 + AGP 7.1.2** 配置本项目，`build-logic` 编译、`:lynket` 配置均成功通过。
- **⚠️ 未闭合项（交开发/验收阶段）**：本 tech-plan runtime **无 Android SDK**，`:lynket:testDebugUnitTest` 因 "SDK location not found" 无法执行（非测试失败，是环境缺 SDK/AVD）。因此"基线 UT 套件实跑全绿"与全部模拟器验证须在具备 Android SDK + AVD 的开发/验收阶段完成。

---

## 8. 影响面

- **正向**：恢复 Slide Over / Web Heads 两模式可用（Android 12+）。
- **触及文件**：仅 `CustomTabs.java`、`BottomBarManager.java`、`WebHeadService.java`（+ 可选新增 `PendingIntents` 助手 + 新增测试）。不动接收方、不动 SDK 常量、不动原生气泡。
- **行为兼容**：<31 设备行为不变（MUTABLE 分支不追加标志，等价旧隐式可变；IMMUTABLE 自 API 23 起本就可用）。

---

## 9. 风险与回滚

| 风险 | 可能性 | 影响 | 缓解 |
|---|---|---|---|
| 误把需回填的 Custom Tabs 点设成 IMMUTABLE，崩溃消失但操作/菜单/底栏静默失效 | 中 | **高（正确性）** | §4 逐点分类为准；层一源码守卫 + 层三"URL 到达"断言双重拦截；评审已对每点核对接收方取值方式 |
| 漏改某个崩溃点，别处仍崩 | 中 | 高 | §4 基于全仓 `grep` 穷尽 14 点；层一守卫测试对三文件全量断言，防漏防未来回归 |
| 给 WebHeads 的隐式 intent 误设 MUTABLE | 低 | 中 | §4 标注隐式 intent 只能 IMMUTABLE；**Android 14 / API 34（targetSdk U+）起隐式+MUTABLE 直接抛异常**（本项目 targetSdk=35 命中） |
| WebHead 三处隐式 broadcast 未限定本包 | 低 | 中 | 接收方已 `RECEIVER_NOT_EXPORTED`，外部收不到；仍要求 `setPackage(getPackageName())` 后再 IMMUTABLE，锁定本包、过 Android 14+ lint |
| Robolectric 4.3 无法在 SDK31 复现崩溃，UT 给"假绿" | 中 | 中 | 明确 UT 只断言 flag 正确性；真实崩溃复现交模拟器（S5），并要求修复前先复现崩溃再验修复 |
| 本阶段未实跑 UT / 模拟器（无 SDK） | 高 | 中 | 已确认基建在源码在案、工具链兼容；开发/验收阶段必须实跑 UT 全绿 + 完成模拟器三项验证 |
| `FLAG_MUTABLE` 常量在 compileSdk=31 可用但 <31 设备语义 | 低 | 低 | 按 `SDK_INT>=31` 条件追加，避免旧设备传入 |

**回滚**：改动集中于 3 文件的 flag 追加 + 独立新增测试，`git revert` 单 commit 即可回退，无数据/协议迁移。

---

## 10. 验收要点（Definition of Done）

- [ ] §4 全部 14 点按分类改毕（**MUTABLE 10 / IMMUTABLE 4**），一律经 `PendingIntents` 助手，保留 `FLAG_UPDATE_CURRENT`。
- [ ] WebHead 三处（12–14）在 `immutable(...)` 前调用 `setPackage(getPackageName())`。
- [ ] 层一守卫测试通过：三文件无裸 `PendingIntent.get*`，且**逐点映射断言**（10 点 mutable、4 点 immutable、mutable 点带显式组件）全绿（S4 / Requirement-3）。
- [ ] 层二 flag 断言测试通过（助手 SDK 分支 + BottomBar/WebHeads 可达点）。
- [ ] 基线 UT 套件在具 SDK 环境实跑**全绿**（含既有测试无回归）。
- [ ] 模拟器（API≥31）：Slide Over / Web Heads 打开链接不崩溃**且操作按钮/菜单/底栏点击 URL 到达**（S1/S2）。
- [ ] 模拟器（API≥29+）：Native Bubbles 无回归（S3）。
- [ ] 修复前后 logcat 崩溃栈对照留档（NFR-1）。
- [ ] 无问题后合并（S5 门槛）。

---

## 11. 交叉评审结论

### 11.1 评审过程与结论
- **评审方**：Codex（对抗式，`codex exec -s read-only`），主 Agent 为技术方案官（CC）。
- **回合数**：1 回合达成一致。Codex 结论"需改"，提出 4 条问题；主 Agent 逐条独立核验，**4 条全部采纳**（无误判需驳回，故无需二轮对抗）。已核验的正确性主张（Custom Tabs 需 MUTABLE、接收方读 `dataString`/`EXTRA_REMOTEVIEWS_CLICKED_ID`、创建侧不设 data、全仓仅这 15 处、`BubbleNotificationManager` 已正确）Codex 独立复核成立。

| # | 严重度 | 问题 | 处置 |
|---|---|---|---|
| 1 | 高 | WebHead 三处隐式 broadcast 未 `setPackage`，`IMMUTABLE` 不限定广播解析范围 | 采纳：§4/§5.1 要求 `setPackage(getPackageName())` 后再 IMMUTABLE（接收方已 NOT_EXPORTED，暴露面本就低，属加固） |
| 2 | 高 | 文档 MUTABLE 计数自相矛盾（写 9，实为 10），DoD 会误导漏改 | 采纳：全文订正为 MUTABLE 10 / IMMUTABLE 4（§1/§4/§10） |
| 3 | 中 | 层一守卫仅查"含某 flag"，无法防"全 IMMUTABLE 假绿" | 采纳：§5.2 助手改为强制；§6 层一升级为**逐点映射断言**（哪点必 mutable/哪点必 immutable + mutable 点带显式组件） |
| 4 | 低 | "隐式+MUTABLE Android 12 会拒"版本错误 | 采纳：订正为 **Android 14 / API 34（targetSdk U+）**（§4/§9） |

> 无剩余分歧，方案定稿。

### 11.2 评审活性监控（合规记录）
外部评审由脚本 `run_codex_review.sh` 驱动：每 30s 检查输出文件长度，连续 5 分钟无新增即终止判失败，2 小时硬上限。本次 Codex 全程持续产出（读代码 + web 检索 AndroidX/PendingIntent 契约），正常退出（退出码 0），未触发卡死/超时。

---

---

## 12. 补充设计：Web Heads 第二处崩溃 —— API 34 前台服务缺 `foregroundServiceType`

> **背景**：开发官按 §1–§11 落地并在 Android 14 模拟器验证——**Slide Over 已彻底修复**（含"点击后 URL 到达接收方"，commit `9ce95363`）。但修好 Web Heads 的 PendingIntent 崩溃后**暴露出第二处崩溃**：`MissingForegroundServiceTypeException`。§1–§11 的根因分析被第一处崩溃"短路"而漏了它。用户已确认按 **A 方案**（本 issue 扩围一并修）。本节为该扩围的补充设计，**Slide Over 修复保持不动、作为基线**。

### 12.1 第二根因（已由开发官在 API 34 AVD 实测，logcat 独立信道确认）
```
android.app.MissingForegroundServiceTypeException: Starting FGS without a type  targetSDK=35
  at android.app.Service.startForeground(Service.java:775)
  at ...bubbles.webheads.OverlayService.onCreate(OverlayService.kt:53)   // startForeground(id, notification)  2 参版
  at ...bubbles.webheads.WebHeadService.onCreate(WebHeadService.java:234)
```
- **机制**：`targetSdk ≥ 34`（本项目 35）起，前台服务**必须**在 manifest 声明 `android:foregroundServiceType` 并持有对应 `FOREGROUND_SERVICE_*` 权限；否则 `startForeground()` 抛 `MissingForegroundServiceTypeException`。当前 manifest 只有 `FOREGROUND_SERVICE`、无类型（`AndroidManifest.xml:37`，服务块 `:91-106`）。
- **为何 §1–§11 漏判**：修复前 PendingIntent 崩在 `WebHeadService.getNotification()`，早于 `OverlayService.onCreate:53` 的 `startForeground`，把它挡在视线外；修好第一处才显形。属"修一处露一处"的典型，不是分析失误性质的遗漏。

### 12.2 崩溃点穷举（回应开发官决策点 3，全仓 FGS 审计）
全仓审计 `startForeground` / `startForegroundService` / WorkManager `setForeground`，**两个**服务会 promote 到前台，均 2 参 `startForeground`、均无类型 → API 34 上都会崩：

| # | 前台服务 | 促前台位置 | 触发条件 | 现状 |
|---|---|---|---|---|
| A | `WebHeadService`（← `OverlayService`） | `OverlayService.onCreate:53` | 打开 Web Heads 模式 | 🔴 本 issue 主症状 |
| B | `AppDetectService` | `AppDetectService.onCreate:101`（`ANDROID_OREO` 守卫） | `isAppBasedToolbar()` 或 `perAppSettings()` 开启时经 `ServiceManager.startAppDetectionService` 启动 | 🔴 **潜伏同类崩溃**，条件触发，必须一并修 |

> 其余排除（已核）：`KeepAliveService` 是 bound service、不促前台；`AppColorExtractorJob` 是 `JobService`；四个 `*Tile` 是 `TileService`；无 WorkManager 前台 worker；两处 `setForeground` 是 `View.setForeground`（UI）。**故只此 2 处需改，无遗漏。**

### 12.3 类型选型（回应决策点 1）：两服务均用 `specialUse`
- Web Heads 是悬浮气泡（`SYSTEM_ALERT_WINDOW` overlay），AppDetect 是轮询 usage-stats 检测前台应用——**都不属于** location/camera/mic/dataSync/mediaPlayback 等任何标准类型。API 34 的兜底类型是 **`specialUse`**（`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`）。
- **关键正确性核实（已查官方文档，非博客）**：`specialUse` 的**运行时前置条件为「无」**——只要声明 `FOREGROUND_SERVICE_SPECIAL_USE` 权限（normal 权限、安装即授予）即可，OS **不会**对 `specialUse` 抛 `ForegroundServiceTypeNotAllowedException`（该异常属 `systemExempted` 类型的 allow-list，与 `specialUse` 无关，网上多篇博客把二者混淆，已排除）。`specialUse` 的唯一额外约束是 **Google Play 上架审核**（需 `<property>` 说明用途），非 OS 崩溃项。
- **备选已否**：把两服务改造成非前台服务属行为/架构大改，超出崩溃修复范围，不做。

### 12.4 落地方式（回应决策点 2，compileSdk=31 / AGP 7.1.2 约束）
**核心洞察：只改 manifest、零代码改动即可消除崩溃**——因为 2 参 `startForeground(id, notification)` 在 API 34 会**继承 manifest 声明的类型**（官方文档明确："若调用未指定类型，则默认取 manifest 中声明的类型"）。两服务现用的正是 2 参版，故声明齐全后无需动 Kotlin/Java。

具体改动（`android-app/lynket/src/main/AndroidManifest.xml`）：
1. 新增权限：`<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />`（权限名是纯字符串，AAPT2 不校验 SDK，compileSdk=31 可编）。
2. 给 `AppDetectService`（`:91-94`）与 `WebHeadService`（`:99-102`）两个 `<service>` 各加 `android:foregroundServiceType`。
   - **⚠️ compileSdk=31 关键点**：枚举名 `specialUse` 是 API 34 才加入 `foregroundServiceType` 的 flag 名，compileSdk=31 的 `attrs.xml` **不认识该名字**，直接写 `="specialUse"` 大概率 AAPT2 报错。**改用数值字面量** `android:foregroundServiceType="0x40000000"`（`FOREGROUND_SERVICE_TYPE_SPECIAL_USE = 1<<30 = 0x40000000`）——AAPT2 对 flags 型属性接受整型字面量。这与仓库既有"高 SDK 用字符串/字面量 + `SDK_INT` 守卫"约定（RAS-38，`Constants.kt:39` 注释）一脉相承。低版本设备（<34）解析时未知 bit 被 mask 掉、无副作用；<29 属性本就被忽略。
   - 加 `tools:targetApi="34"`（或必要时 `tools:ignore="UnusedAttribute"`）避免 lint 噪声；lint 为 warning 不 fail 构建，属次要。
3. **默认加**（评审采纳，本项目带 `com.android.vending.BILLING` 权限、`AndroidManifest.xml:25`，有 Play 分发痕迹，故不设为可选）在两 `<service>` 内各加 `<property>` 声明用途：
   ```xml
   <!-- WebHeadService -->
   <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
             android:value="Floating overlay web bubbles (SYSTEM_ALERT_WINDOW) to keep tabs accessible" />
   <!-- AppDetectService -->
   <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
             android:value="Detects the current foreground app to drive per-app browsing-mode settings" />
   ```
   `<property>` 标签 API 31 引入、compileSdk=31 的 AAPT2 支持；**零运行时副作用**，仅 Play 审核读取。虽非 OS 崩溃项，但官方对 `specialUse` 明确要求在 `<service>` 内声明用途并经 Play review，故默认纳入以免上架被挡。use case 文案 Owner 可按实际调整。

> **不选显式 3 参 `startForeground`**：`ServiceCompat`（core `1.9.0-alpha03`，早于 Android 14）无 `SPECIAL_USE` 常量/34 感知；平台 3 参 `Service.startForeground(id,notif,type)` 虽 API 29 起可编，但要在 `OverlayService`+`AppDetectService` 两处加 `SDK_INT>=34` 守卫 + 字面量 `0x40000000`，**比 manifest-only 多两处代码、多风险且无收益**（2 参已继承 manifest 类型）。故不采纳。

### 12.5 构建/验证未闭合点（本 tech-plan runtime 无 Android SDK）
- ⚠️ **AAPT2 是否接受 `foregroundServiceType="0x40000000"`（数值字面量）需开发官在具 SDK 环境构建确认**。这是本方案唯一的构建面不确定项；数值字面量喂 flags 属性是业界常用绕过 compileSdk 落后的手法，风险低，但必须实编验证。若极端情况下 AAPT2 拒绝：退路是把该属性放进 `res/values` 覆盖或改用 3 参运行时方案（§12.4 末），二选一由开发官据实编报错定。
- **验收（Android 12+/14 模拟器，开发官执行）**：
  1. Web Heads 打开链接 → 前台服务正常启动、悬浮气泡出现、**不再崩 `MissingForegroundServiceTypeException`**（对照修复前后 logcat `-b crash`）。
  2. 开启 app-based toolbar / per-app settings 触发 `AppDetectService`，确认其 `startForeground` 在 API 34 不崩（点 B 回归）。
  3. Slide Over（commit `9ce95363` 基线）与 Native Bubbles **无回归**。

### 12.5b ⚠️ 已知相邻风险：`AppDetectService` 的后台启动崩溃（评审补充，非 `foregroundServiceType` 能修）
`Lynket.onCreate()`（Application）**无条件**调用 `ServiceManager.takeCareOfServices(applicationContext)`（`Lynket.kt:53`）；若 app-based toolbar / per-app settings 偏好开启，它经 `ContextCompat.startForegroundService()` 启动 `AppDetectService`（`ServiceManager.java:55`）。**这类"从后台启动前台服务"在 Android 12+ 若不满足豁免会抛 `ForegroundServiceStartNotAllowedException`**——这是**另一类崩溃**，`foregroundServiceType` 修不了它。
- **与本次修复的关系**：本次给 AppDetectService 补类型后，它才真正尝试进入前台，故这条后台启动限制**可能被同一"修一处露一处"效应带出**。
- **处置**：**列为已知相邻风险，不在本 change 强行扩围**（是否触发高度依赖启动时机/进程状态，且属独立问题）。**要求开发官在 API 34/35 验收时专门覆盖**："app-based toolbar 偏好开启 + 冷启动/后台进程被拉起"路径，观察 `AppDetectService` 是否抛 `ForegroundServiceStartNotAllowedException`。若复现 → 另开跟进 issue（参照 RAS-55 解耦），不阻塞 Web Heads 崩溃交付。

### 12.6 决策点 4（POST_NOTIFICATIONS / 通知不可见）——建议本 issue 不纳入
开发官提的"API 33+ 该前台服务通知 `importance` 低 / `POST_NOTIFICATIONS` 未授时通知不可见"**不是崩溃、不是正确性问题**（服务照常运行，仅通知视觉被抑制），属 UX 退化。为保持本 issue「崩溃修复 + 最小面」的收敛，**建议不纳入本 change**，另开跟进 issue 处理（与 RAS-55 类似解耦）。🟡 此点仍由用户/开发官最终定；若坚持纳入，我再补设计。

### 12.7 spec / 验收增量
本节新增一条 capability 需求（供归档时并入 living spec）：
- **Requirement（ADDED）**：WHEN 在 `targetSdk ≥ 34`（Android 14+）模拟器/设备上启动 Web Heads 或 App-Detection 前台服务，THE SYSTEM SHALL 正常进入前台（气泡/检测生效），而不因缺 `foregroundServiceType` 抛 `MissingForegroundServiceTypeException`。
- **覆盖测试**（开发阶段已落地）：模拟器 E2E（API 34 AVD `LyknetTest` 实测）——`WebHeadService`、`AppDetectService` 均 `dumpsys` 佐证 `isForeground=true types=40000000`、`startForegroundCount≥1`，logcat 无 `MissingForegroundServiceTypeException`；纯 UT 无法复现 FGS 类型校验（平台运行时行为，与 §6 同理靠模拟器闭环）。已同步 `specs/link-open-modes/spec.md`。

### 12.7b 验收补充（把 12.5b 纳入门槛）
在 §12.5 的三项验收基础上追加：**（4）** 开启 app-based toolbar / per-app settings 偏好后**冷启动 App**，确认 `AppDetectService` 在 API 34 既不抛 `MissingForegroundServiceTypeException`（本次修复目标），也观察是否抛 `ForegroundServiceStartNotAllowedException`（12.5b 的相邻风险，若抛则另开 issue）。

### 12.8 §12 交叉评审结论
- **评审方**：Codex（对抗式，read-only），主 Agent 技术方案官（CC）。脚本 `run_codex_review.sh` 活性监控（30s 采样 / 5min 无输出即终止 / 2h 上限）；本次 Codex 全程持续产出（读代码 + 官方文档核实 AAPT2/AOSP/FGS 契约），正常退出（码 0）。
- **回合数**：1 回合达成一致。结论"需改"，2 条 🟡 中级问题，主 Agent 独立核验后**全部采纳**（无误判驳回）。Codex 独立复核通过的主张：只 2 个 FGS 需修、2 参 startForeground 继承 manifest 类型、`specialUse` 运行时无前置、`0x40000000` 即 `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`、POST_NOTIFICATIONS 不纳入、与 `9ce95363` 无冲突。

| # | 严重度 | 问题 | 处置 |
|---|---|---|---|
| 1 | 中 | `<property PROPERTY_SPECIAL_USE_FGS_SUBTYPE>` 被写成"可选/仅 Play"，对带 Billing 的本项目偏轻 | 采纳：§12.4 改为**默认加**两服务的 property（零运行时副作用） |
| 2 | 中 | 漏了 `AppDetectService` 的**后台启动** FGS 崩溃面（`ForegroundServiceStartNotAllowedException`，非类型能修） | 采纳：新增 §12.5b 列为已知相邻风险 + §12.7b 纳入验收观察，若复现另开 issue |

> 唯一构建面不确定项（AAPT2 是否接受 `foregroundServiceType="0x40000000"`）Codex 认可"已列为构建验证项"的处理方式；本 runtime 无 SDK 无法实编，交开发官验证。

---

## 变更历史

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-07-16 | 初稿：逐点可变性分类（更正 proposal 的 IMMUTABLE 默认取向）、三层测试策略、基线/工具链决策、风险与验收 | 技术方案官 |
| 2026-07-16 | 经 Codex 对抗式评审（1 回合，4 条全采纳）：订正 MUTABLE 计数 9→10；WebHead 三点加 `setPackage`；助手强制 + 层一升级逐点映射守卫；隐式+MUTABLE 版本门槛订正为 API 34 | 技术方案官 |
| 2026-07-16 | 补 §12：Web Heads 第二处崩溃（API 34 前台服务缺 `foregroundServiceType`）补充设计——两 FGS 均用 `specialUse`、manifest-only 零代码、compileSdk=31 用数值字面量、AppDetectService 一并修；含官方文档核实 `specialUse` 无运行时前置 | 技术方案官 |
| 2026-07-16 | §12 经 Codex 对抗式评审（1 回合，2 条中级全采纳）：`<property>` 改默认加；新增 §12.5b/§12.7b 覆盖 `AppDetectService` 后台启动 `ForegroundServiceStartNotAllowedException` 相邻风险 | 技术方案官 |
