<!-- doc-init template version: v1.0 -->
# Design: 54-mode-crash-fix

- **Owner**: 技术方案官 on behalf of n374 (wu.nerd@gmail.com)
- **Reviewers**: 开发官, n374, Codex（对抗式交叉评审）
- **状态**: Plan（技术方案已定稿，含交叉评审结论，待进入开发阶段）
- **创建日期**: 2026-07-16
- **最后更新**: 2026-07-16
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

## 变更历史

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-07-16 | 初稿：逐点可变性分类（更正 proposal 的 IMMUTABLE 默认取向）、三层测试策略、基线/工具链决策、风险与验收 | 技术方案官 |
| 2026-07-16 | 经 Codex 对抗式评审（1 回合，4 条全采纳）：订正 MUTABLE 计数 9→10；WebHead 三点加 `setPackage`；助手强制 + 层一升级逐点映射守卫；隐式+MUTABLE 版本门槛订正为 API 34 | 技术方案官 |
