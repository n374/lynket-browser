# RAS-55 · 原生气泡承载外部浏览器 CCT —— Spike 技术方案

- **Owner**：技术方案官（本设计）→ 开发官（实现 spike + 真机取证）
- **状态**：设计完成，待开发阶段实现并执行
- **适用范围**：Lynket Android App，仅原生气泡（Native Bubbles）渲染链路；**spike 验证，不做产品化**
- **最后更新**：2026-07-16
- **上游**：`docs/changes/55-native-bubble-cct/proposal.md` · spec `specs/native-bubble-external-browser/spec.md`
- **关联**：RAS-54（Slide Over / Web Heads 崩溃修复）、RAS-38（原生气泡前台浮出 spike）
- **基线分支**：`spike/55-native-bubble-cct`（基于 `build/apk`，已含 #170 气泡修复：`FLAG_IMMUTABLE` + shortcut-backed bubble）

---

## 1. 概述与目标

**一句话**：新增一个满足气泡嵌入声明的**薄壳 Activity**替换气泡当前指向的内置 WebView（`EmbeddableWebViewActivity`），让它在气泡 task 内、用 **activity context 且不带 `FLAG_ACTIVITY_NEW_TASK`** 启动「所选外部浏览器的 Custom Tab」，真机验证系统 UI 会不会把这个**跨 App(跨 UID) 的 CCT** 渲进气泡浮窗——这是 proposal 收敛的「路径 2」唯一未被平台实锤封死的路。

本 spike 的产物是**可行/不可行的铁证矩阵**，不是可上线功能。三条验收（浮窗形态 / dumpsys 引擎证据 / 存储令牌共享）以 spec 为准，本设计只解决「怎么把这条链路实现出来、怎么可复现地取证」。

## 2. 非目标

- 不做产品化（设置开关、多站点适配、返回栈打磨、ghost-tab 自杀的正式处理）——留 spike 通过后的 feature。
- 不迁移 `compileSdk 35`；沿用 RAS-38 结论，`targetSdk` 满足门禁即可（见 §7 风险中对 targetSdk=29 的专门讨论）。
- 不碰已被实锤封死的四条路径（读 cookie 注入 WebView / 跨 App Activity Embedding / TWA / WebView 借 profile）。
- 不改动 Slide Over / Web Heads / 正常 CCT 打开链路（红线 #1）。

## 3. 关键事实（已核实源码，非推断）

| 事实 | 证据 |
|---|---|
| 气泡展开目标由 `viewIntent` 决定，API 30+ 经 shortcut 的 `.setIntent(viewIntent)` 承载 | `BubbleNotificationManager.kt:115`（viewIntent）、`:151`（shortcut.setIntent）、`:166` shortcut-backed metadata |
| 气泡目标当前是 `EmbeddableWebViewActivity`（内置 WebView 子类，无外部登录态） | `BubbleNotificationManager.kt:41,115`；`EmbeddableWebViewActivity.kt` = `WebViewActivity()` 空子类 |
| 气泡可嵌入的前提是 Activity 声明 `allowEmbedded/resizeableActivity/documentLaunchMode` | manifest `:180-184` 仅 `EmbeddableWebViewActivity` 有这三项；`CustomTabActivity`(`:173`) 没有 |
| 正常 CCT 打开路径 = 直接 `CustomTabs.launchUrl(activity, uri)`，默认**不加** `NEW_TASK` | `CustomTabActivity.kt:42-45` → DI `customTabs().forUrl().launch()` → `CustomTabs.java:213-223` `openCustomTab`（builder 无 NEW_TASK） |
| 所选后端浏览器包名解析入口 | `CustomTabs.java:152 getCustomTabPackage()` 读 `Preferences.customTabPackage()` |
| **会塌全屏的雷**：`DefaultTabsManager.openBrowsingTab` 的 mergeTabs 分支会附加 `NEW_DOCUMENT/NEW_TASK` | `DefaultTabsManager.kt:332-338`；薄壳**必须绕开**此路径，直连 `CustomTabs` |
| 当前基线 `targetSdk=29 / compileSdk=31` | `build-logic/.../constants/Constants.kt:36-38` |

> 结论：现有 `CustomTabActivity` 已经是「activity context + 不带 NEW_TASK 启动 CCT」的现成范式，**唯一缺的是气泡嵌入声明**。薄壳 = `CustomTabActivity` 的启动逻辑 + `EmbeddableWebViewActivity` 的嵌入声明。

## 4. 架构设计

### 4.1 数据流对比

```
现状（内置 WebView，无外部登录态）：
  气泡展开 → shortcut.viewIntent → EmbeddableWebViewActivity(本App WebView) → 独立 Cookie，塞不进 Chrome 登录态

路径 2 spike（本设计）：
  气泡展开 → shortcut.viewIntent → BubbleCctShellActivity(薄壳, 声明 allowEmbedded)
            └─ onCreate: CustomTabs.launchUrl(activity, url, setPackage=所选浏览器)  ← 不带 NEW_TASK
               └─?→ 系统 UI 把跨 App CCT 渲进气泡 task/window ?  ← 【本 spike 要钉死的未知】
                     成功 = 气泡内真 Chrome 渲染 + Chrome 登录态，且保留浮窗
                     失败 = 塌全屏（气泡 collapse）/ 拒绝 / 崩溃
```

### 4.2 组件清单

| 组件 | 动作 | 说明 |
|---|---|---|
| `BubbleCctShellActivity`（新增，spike-only） | 新建 | 薄壳；继承 `BrowsingActivity`，复刻 `CustomTabActivity` 的 CCT 启动，但**不复制 ghost-tab 自杀**（见 §5.2） |
| `AndroidManifest.xml` | 新增 activity 声明 | 带 `allowEmbedded=true / resizeableActivity=true / documentLaunchMode=always`，镜像 `EmbeddableWebViewActivity`；`exported=false` |
| `BubbleNotificationManager.kt` | 改 viewIntent 目标 | 由编译期开关在 `EmbeddableWebViewActivity`（对照组）↔ `BubbleCctShellActivity`（实验组）间切换；**同一处同时影响 `:115` viewIntent 与 `:151` shortcut.setIntent** |
| spike 开关 | 新增 | `BuildConfig`/常量，默认指向实验组；保留对照组以便同机 A/B（见 §5.3） |

## 5. 实现细节

### 5.1 薄壳 Activity（核心）

薄壳继承 `BrowsingActivity`（拿到 `activityComponent`），但**不整体复用 `CustomTabActivity` 的 `customTabs().forUrl().launch()` facade**——该 facade 藏了两个会制造假阳的副作用（见下 ⚠️），spike 需要一段**可控、可 log、无 fallback 掩盖**的最小 CCT 启动：

```
// 伪代码：薄壳 onCreate（savedInstanceState==null 时）
val pkg = CustomTabs.getCustomTabPackage(this)          // 所选后端浏览器；null 即判失败并 loud log
val cct = CustomTabsIntent.Builder(/* session = */ null).build()  // ⚠️见 5.1(c)：显式 null session
cct.intent.setPackage(pkg)
Timber.d("[BUBBLE_SPIKE] pkg=%s flags=0x%x", pkg, cct.intent.flags)  // ⚠️见 5.1(b)：打实际 flags
try { cct.launchUrl(this, uri) }                         // this=activity context，无 NEW_TASK
catch (e) { Timber.e("[BUBBLE_SPIKE] launch failed, NO fallback: %s", e); /* 判失败，不回退 WebView */ }
```

要点（每条对应一个已核实的假阳/塌全屏雷）：
- (a) **必须直连显式 `CustomTabsIntent`**，禁止走 `DefaultTabsManager.openBrowsingTab`（mergeTabs 分支 `DefaultTabsManager.kt:332-338` 加 `NEW_DOCUMENT/NEW_TASK`，必然塌全屏）；**不追加任何 flag**。
- (b) **launch 前把 `cct.intent.flags` 打进 log**（`[BUBBLE_SPIKE]`），消掉「androidx.browser 是否默认加 flag」的黑箱——不靠读库自证。字节码轻量核验已提示 `launchUrl` 仅 `setData`+`startActivity`、无 `FLAG_*` 常量，但**以真机 log 的 flags 为准**。
- (c) ⚠️ **显式传 `null` session**：`CustomTabs.getSession()`（`CustomTabs.java:296-305`）会优先复用 `WebHeadService.getTabSession()`，该 session 绑的是 WebHead 预热的浏览器、**未必等于 `customTabPackage`**——若 session 与 `setPackage` 不同源，CCT 可能落到 session 的 provider，静默覆盖被测浏览器，造成结论指错对象。故 spike 用 `Builder(null)`。
- (d) ⚠️ **禁止 CCT fallback 掩盖失败**：`CustomTabs.openCustomTab`（`CustomTabs.java:213-231`）在无包/异常时 fallback 到 Lynket `WebViewActivity`，会把「CCT 起不来」伪装成「页面打开了」。薄壳自实现 launch，**捕获异常即判失败、loud log、绝不回退内置 WebView**。
- 主题沿用 `Theme.AppCompat.Translucent`（同 `CustomTabActivity`）。

### 5.2 ⚠️ 关键分歧点：薄壳**移除全部自动 `finish()`**（相对 `CustomTabActivity` 的蓄意偏离）

`CustomTabActivity` 有**两处**会销毁 Activity 的自杀逻辑，薄壳**两处都要移除**（只移一处不够）：
1. `onAttachedToWindow` 置 `isLoaded=true` → `onResume` 时 `finish()`（`CustomTabActivity.kt:53-63`）；
2. **`onCreate` 中 `savedInstanceState != null` → `finish()`**（`CustomTabActivity.kt:41-46`）——气泡内薄壳若因配置变更/进程回收被**重建**，这条会直接 finish。

理由（正确性/可观测性）：薄壳是气泡 task 的**根 Activity**，任何自动 finish 都可能**销毁气泡 task**、破坏 `dumpsys` 取证的稳定 expanded 态，制造假阴/不可复现。**决策**：薄壳不写任何自动 `finish()` 分支（除非明确记录为失败态）。代价是 CCT 关闭后可能露出空白薄壳——spike 可接受，productization 再处理。此点是对「照抄 `CustomTabActivity`」的明确补丁，避免开发官全盘复制。

### 5.3 气泡目标切换开关（保对照组）

在 `BubbleNotificationManager` 顶部引入编译期常量（如 `private val BUBBLE_TARGET = BubbleCctShellActivity::class.java`，或 `BuildConfig.BUBBLE_CCT_SPIKE` 分支）：
- `:115` viewIntent 与 `:151` shortcut `.setIntent(viewIntent)` **必须同步指向同一目标**（shortcut-backed 是 API 30+ 生效路径，只改一处会造成实验/对照错配——这是易漏点）。
- 保留 `EmbeddableWebViewActivity` 分支作**对照组**：同一 APK 下先跑对照组确认气泡本身正常出浮窗（隔离「气泡坏了」与「CCT 嵌不进」两类失败），再切实验组。

### 5.4 触发气泡的前置设置（供取证脚本）—— 必须隔离干扰源

⚠️ **不要用 `openWebHeads` 常规链路触发气泡**：`DefaultTabsManager.openWebHeads`（`:371` 一带）在弹出 native bubble **之后**，若 `aggressiveLoading && !fromMinimize && !shouldUseWebView`，会**另外**调 `backgroundLoadingStrategy[CUSTOM_TAB].prepare()` + `openBrowsingTab()` 起一个**正常 CCT / 全屏 tab**。这会让 dumpsys/logcat/探针令牌可能来自那个正常 CCT 而非气泡内 CCT，直接**污染实验**（假阳或假阴）。

因此触发气泡必须满足：
- **首选：加一个 debug-only 入口，只调 `BubbleNotificationManager.showBubbles(BubbleLoadData(探针URL))`**，绕开 `openWebHeads` 的 aggressive-loading 副作用。
- 若仍走常规链路，取证前**强制**并记录：`aggressiveLoading=false`、`mergeTabs=false`、`useWebView=false`、`nativeBubbles=true`（`rxPreferences.nativeBubbles`）、`customTabPackage=被测浏览器包名`（`Preferences.customTabPackage()`）。
- **每格实验前清残留**（`BubbleNotificationManager.kt:144` shortcutId=URL、`showBubble` 会二次 notify、channel/shortcut 有缓存）：清通知 + 清 dynamic shortcut，或对每次实验用**带唯一 query 的 URL**，避免实验组/对照组切换时命中旧 shortcut/通知。

### 5.5 新增 Activity 的 Dagger 注入（否则编译/运行失败）

`ActivityComponent`（`di/activity/ActivityComponent.kt`）**逐 Activity 显式声明 `inject(...)`**（无泛型入口，已核实 `:65 inject(customTabActivity)` 等）。薄壳必须补 `fun inject(a: BubbleCctShellActivity)`，并在薄壳 `override fun inject(activityComponent) { activityComponent.inject(this) }`——否则 DI 缺失、编译或运行期崩。

## 6. 测试验证（可复现取证协议）

严格执行 spec 的三条通过标准与「验收前置条件」。**决定性引擎证据是 `dumpsys activity activities`（浏览器 CCT 包名落在气泡 task），探针 UA「无 wv」仅辅助**；存储共享以 cookie 与 localStorage 令牌**各自严格相等**为准、cookie 须复读校验 OK、两者分开记录。

### 6.1 取证脚本（脚本化取证，不靠人工等待烧 token）

由开发官落一个 `docs/changes/55-native-bubble-cct/assets/capture.sh`（spike 资产，不随产品发布），逐「Android 版本 × 浏览器版本」跑：

```
# 前置：清站点数据（从基线起测）
adb shell pm clear <被测浏览器包名>         # 只清被测浏览器的探针 origin 数据，勿清错
# 1) 独立浏览器直开探针页（显式 -p，回读 resolved Activity，避免其实打开了默认浏览器）
adb shell am start -a android.intent.action.VIEW -p <被测浏览器包名> \
  -d "http://10.0.2.2:<port>/login-state-probe.html"   # 记录 am start 输出 + 浏览器版本 + resolved activity
adb logcat -d | grep BUBBLE_PROBE   # 记 cookieToken/localStorageToken 基线
# 2) 走 debug 入口（§5.4）触发气泡打开同一 URL，展开气泡
# 3) 决定性引擎证据：保存【完整】原文，勿只 grep -A3（会丢 taskId/windowingMode/栈关系→假阳）
adb shell dumpsys activity activities > dump_activities_<ver>_<browser>.txt
adb shell dumpsys activity top        > dump_top_<ver>_<browser>.txt
adb shell dumpsys window containers   > dump_window_<ver>_<browser>.txt
adb shell dumpsys activity bubbles    > dump_bubbles_<ver>_<browser>.txt   # 断言 bubble 仍 expanded
#    脚本化断言（三者交叉）：气泡所在 task/window 内的 top/resumed Activity 是【被测浏览器包的 CCT】，
#    且与 Lynket 薄壳同一 taskId、windowingMode 为 bubble/freeform 而非 fullscreen。
# 4) 探针辅助信号 + 存储令牌 + spike launch 的实际 flags
adb logcat -d | grep -E "BUBBLE_PROBE|BUBBLE_SPIKE"
# 5) 截图（气泡形态 + 探针页各值）
adb exec-out screencap -p > shot_<ver>_<browser>.png
```

- 每 30s 轮询 dumpsys/logcat 输出长度判活性（对齐 CLAUDE.md「外部进程监控」纪律）；此处指真机取证时若脚本挂起的兜底，非评审进程。
- **fail loud**：塌全屏 / dumpsys 顶层仍是 Lynket 内置 WebView Activity / `[BUBBLE_SPIKE]` 打出 fallback 或异常 / 令牌不等 / cookie 复读失败 / 崩溃 —— 逐项如实记为失败态，禁止把「塌全屏但页面能打开」或「fallback 到内置 WebView」粉饰成部分成功（spec 边界语义）。

### 6.2 结论矩阵（禁单点外推；含 targetSdk 维度）

按 spec 并**追加 targetSdk 维度**：逐「Android 14/15/16(/17) × 浏览器版本 × targetSdk」出「可行/不可行/失败态」。
- **任一格三条全绿 ⇒ 只能写「仅该组合可行」**，不得表述为「路径 2 全面可行」。
- ⚠️ **全矩阵不可行时的收口边界**：当前基线 `targetSdk=29`，低 targetSdk 在高版本 OS 走兼容模式，系统对**跨 UID 气泡嵌入**的策略可能因 targetSdk 而异。故若在 `targetSdk=29` 下全失败，**只能结论「当前 Lynket targetSdk=29 下路径 2 不可行」，不得写成「路径 2 平台不可行」**；要下「平台不可行」的更强结论，**必须至少再跑一个 targetSdk 提升的诊断变体**（仅诊断复测、不纳入本 spike 交付、不做完整 compileSdk 迁移）复现失败后方可。
- 每格必留：截图 + 完整 `dumpsys activity activities/top/window containers/bubbles` + `logcat BUBBLE_PROBE|BUBBLE_SPIKE`，并记录**被测浏览器包名+版本、Android 版本、targetSdk、am start resolved activity**。

### 6.3 编译验证

薄壳 + manifest 改动需 `assembleDebug` 编过（CI `build-apk.yml` 出可安装 debug APK）；对照组/实验组两态都要能装能触发气泡。

## 7. 风险与回滚

| 风险 | 级别 | 应对 |
|---|---|---|
| **依赖未承诺行为**：跨 UID CCT 能否嵌进气泡窗，官方文档不区分同/跨 App，无保证 | 🔴 结构性 | 本就是 spike 要证伪/证实的核心；即便通过也依赖未承诺行为，Chrome/系统更新可能退化为全屏（退化后 ≈ Web Heads，属**可接受降级**，非正确性破坏） |
| **targetSdk=29 可能改变结论并被过度外推**：低 targetSdk 在高版本 OS 走兼容模式，气泡/TaskView 对跨 UID Activity 策略可能因 targetSdk 而异；全失败易被误写成「平台不可行」 | 🟡 结论边界 | §6.2：结论加 targetSdk 维度；`targetSdk=29` 全失败只能结论「targetSdk=29 不可行」，要下「平台不可行」须先跑 targetSdk 提升诊断变体复现（仅诊断、不做完整迁移） |
| **触发链副作用污染实验**：`openWebHeads` 的 aggressive-loading 会另起正常 CCT/全屏 tab，令令牌/dumpsys 指错对象 | 🟡 假阳/假阴 | §5.4：用 debug 入口直调 `showBubbles()`，或强制 `aggressiveLoading/mergeTabs/useWebView=false` 并记录 |
| **CCT fallback / WebHead session 污染**：fallback 到内置 WebView 伪装成功；复用 WebHead session 覆盖被测浏览器 | 🟡 假阳 | §5.1(c)(d)：显式 `null` session、禁 fallback、异常即判失败并 loud log |
| 薄壳自动 `finish()` 销毁气泡 task 破坏取证（两处：onResume + savedInstanceState≠null） | 🟡 正确性/可观测 | §5.2 移除**全部**自动 finish |
| dumpsys 判据太弱（`grep -A3` 丢 taskId/windowingMode/栈）→ 假阳 | 🟡 假阳 | §6.1 保存完整 dumpsys 原文并脚本化断言同 bubble task top==目标 CCT |
| 新增 Activity 漏配 Dagger `inject` → 编译/运行失败 | 🟡 实现完整性 | §5.5 补 `ActivityComponent.inject(BubbleCctShellActivity)` |
| 只改 `:115` 漏改 `:151` shortcut.setIntent → API 30+ 实验/对照错配 | 🟡 假阴 | §5.3 两处同步同一目标；对照组先验证气泡本身正常 |
| 误走 `openBrowsingTab` mergeTabs 加 `NEW_TASK` 必塌全屏 | 🟡 假阴 | §5.1(a) 强制直连显式 `CustomTabsIntent` |
| shortcut/通知缓存残留污染实验/对照切换 | 🟢 | §5.4 每格清通知+dynamic shortcut 或唯一 query URL |
| OEM/浏览器差异 | 🟡 | 证据矩阵多版本覆盖 |

**回滚**：纯 spike 分支、加法改动（新增薄壳 + 开关切回对照组即恢复现状），不影响主干；spike 结论产出后该分支不合入产品。

**spike 不通过的等价满足**（交用户选，来自 proposal）：A. Android 17 系统级「气泡化任意 App」；B. Web Heads（RAS-54 修好后）；C. 气泡内置 WebView 自身持久登录。

## 8. 影响面

- 仅新增 1 个 spike-only Activity + 1 处 manifest 声明 + `BubbleNotificationManager` 气泡目标开关；**不触碰** Slide Over / Web Heads / 正常 CCT / 正常 WebView 打开链路。
- 对照组默认可切回 `EmbeddableWebViewActivity`，现有气泡行为零回归。

## 9. 评审结论（Codex 对抗式交叉评审）

本设计经 Codex 独立对抗式评审，首轮结论「**需改**」，提出 9 条假阳/假阴/实现完整性问题。逐条**源码核实后全部采纳**（无误判驳回），已并入 §5/§6/§7：

| # | 级别 | 问题 | 采纳落点 |
|---|---|---|---|
| 1 | 高 | `openWebHeads` aggressive-loading 另起正常 CCT 污染实验 | §5.4 debug 入口直调 `showBubbles()` / 强制关闭相关偏好 |
| 2 | 高 | `targetSdk=29` 全失败被过度外推为「平台不可行」 | §6.2 结论加 targetSdk 维度 + 诊断变体收口 |
| 3 | 高 | dumpsys `grep -A3` 太弱，丢栈关系→假阳 | §6.1 保存完整 dumpsys 并脚本化断言同 bubble task |
| 4 | 中 | CCT fallback 把失败伪装成功 | §5.1(d) 禁 fallback、异常判失败、loud log |
| 5 | 中 | 复用 `WebHeadService` session 覆盖被测浏览器 | §5.1(c) 显式 `Builder(null)` session |
| 6 | 中 | 只移 onResume 自杀不够，`savedInstanceState≠null→finish()` 也要禁 | §5.2 移除**全部**自动 finish |
| 7 | 中 | 新增 Activity 漏配 Dagger `inject` | §5.5 补 `ActivityComponent.inject(...)` |
| 8 | 低 | 基线浏览器 `am start` 未 `-p`、未回读 resolved | §6.1 显式 `-p` + 记录 resolved activity |
| 9 | 低 | shortcut/通知缓存需清理协议 | §5.4 每格清缓存或唯一 query URL |

采纳后无剩余分歧，评审达成一致（1 轮）。协作评审配对：CC（主）× Codex（评审）；Codex 侧自述其下游 `claude -p` 通道 `FailedToOpenSocket` 与本评审无关（本轮 Codex 作为 CC 的评审方已正常产出）。

## 10. 相关提交 / 分支

- 分支：`spike/55-native-bubble-cct`（基于 `build/apk`）
- 变更文档：`docs/changes/55-native-bubble-cct/`（proposal / spec / probe html / 本 design）

## 11. 变更历史

- 2026-07-16 技术方案官：产出 spike 技术方案（薄壳 Activity + 气泡目标开关 + 取证协议），经 Codex 对抗式评审后定稿，转开发阶段。
