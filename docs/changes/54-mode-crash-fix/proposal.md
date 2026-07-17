<!-- doc-init template version: v1.0 -->
# Proposal: 54-mode-crash-fix

- **Owner**: 需求官 on behalf of n374 (wu.nerd@gmail.com)
- **Reviewers**: 技术方案官, n374
- **创建日期**: 2026-07-16
- **状态**: Clarify（需求已收敛并经用户确认，待进入技术方案 Plan 阶段）
- **关联 Issue**: RAS-54「LB Bug 修复」
- **仓库 / 基线分支**: `arunkumar9t2/lynket-browser` @ `fix/54-mode-crash-fix`（基于已发布主线 `fork/main` = `git@github.com:n374/lynket-browser.git` 的 `main`，HEAD `a57031b1`）

## 1. Why（动机）

用户实测 Lynket Browser（LB，Android 浏览器 App）三种「打开链接」模式中，**只有原生气泡（Native Bubbles）模式可用，另外两种模式（Slide Over / Web Heads）一打开就崩溃**，导致这两种模式完全不可用。

崩溃根因已由问题定位阶段静态定位并经代码佐证（详见 §6）：项目 `targetSdk=35`（≥ Android 12 / API 31），而这两种模式创建 `PendingIntent` 时未显式带 `FLAG_IMMUTABLE`/`FLAG_MUTABLE`，Android 12+ 上创建即抛 `IllegalArgumentException`。唯一被修好的是原生气泡（代码里已带 `FLAG_IMMUTABLE`），这解释了「为何只有它可用」。

用户诉求明确：**先修好这两种模式的崩溃**，补充测试用例，在模拟器上验证通过，无问题即合并。这是确定性的纯 Bug 修复，价值直接（恢复两种核心交互模式的可用性）。

> 用户的另一诉求「让原生气泡也能复用外部浏览器登录态」经确认属**行为/架构变更**（需把原生气泡从内置 WebView 改造成走 Custom Tabs），已按用户指示**拆为独立 Issue** 承接，不在本 change 范围内（见 §3）。

## 2. What's Changing（高层变更）

| Capability | 变化类型 | 简述 |
|---|---|---|
| `link-open-modes` | ADDED | 新增「Slide Over 与 Web Heads 模式在 Android 12+ 上打开链接不崩溃、且原生气泡不回归」的可验收需求 |

**新增 capability**：
- `link-open-modes`：Lynket 通过其三种模式（Slide Over / Web Heads / Native Bubbles）打开并展示链接的能力。当前 `docs/` 无该 capability 的 living spec，故以 ADDED 形式引入其首批需求。

> 本阶段（需求官）**定义「做成什么样」（验收）**。崩溃根因已由上一阶段确证，本 proposal 据此圈定范围；但**具体改动落点清单、修复方式与测试实现由技术方案 / 开发阶段负责**，需以真实构建与模拟器复现为准。

## 3. Out of Scope（明确不做）

- **不做**「让原生气泡复用外部浏览器登录态 / 支持 Custom Tabs」——经用户确认属行为/架构变更，**已拆为独立 Issue** 承接，与本 change 解耦。
- **不改**原生气泡（Native Bubbles）现有可用行为，仅需保证其不被本次修复回归。
- **不新增**任何模式的新功能，不做交互/UI 改版。
- **不做** Lynket 整体架构重构或模块现代化（历史上未完成的现代化已被主线 revert，不在此重启）。
- **不改** `targetSdk`（保持 35）——修复方向是补全 `PendingIntent` 的可变性标志，而非回退 SDK。

## 4. Stakeholders

| 角色 | 关注点 | Review 必需 |
|---|---|---|
| n374（用户/发起人） | 两种模式在模拟器上不再崩溃、可正常打开链接；有 UT；无回归后合并 | 是 |
| 技术方案官 | 崩溃点清单是否穷尽、修复方式、UT 与模拟器验证方案、基线分支取舍 | 是（下阶段主理） |
| 开发官 / MR官 | 落地实现、UT 覆盖、模拟器验证、合并 | 后续阶段 |

## 5. Success Metrics（成功指标）

- **S1**：在 **Android 12+（API ≥ 31）模拟器**上，用 **Slide Over** 模式打开一个链接，**不再崩溃**，Custom Tab 正常打开。
- **S2**：在 **Android 12+ 模拟器**上，用 **Web Heads** 模式打开一个链接，**不再崩溃**，悬浮气泡（Web Head）正常出现。
- **S3（无回归）**：**Native Bubbles** 模式在 Android 10+ 上仍可正常打开链接，功能不被本次修复破坏。
- **S4（UT 覆盖）**：崩溃根因修复点具备**单元测试**覆盖，断言修复后创建的 `PendingIntent` 均显式带 `FLAG_IMMUTABLE`（或 `FLAG_MUTABLE`，视语义）。
- **S5（交付门槛）**：改动经**模拟器验证通过**（两种模式实际打开链接不崩溃），无回归后方可合并。

## 6. 崩溃根因（问题定位阶段结论，已在基线 `fork/main` 复核）

> 静态代码 + Android 官方文档 + 仓库自身注释三方佐证，无需设备即可确认；已在本 change 基线 `fork/main`（HEAD `a57031b1`）上逐条复核仍成立。

- **机制**：`targetSdk ≥ 31` 时，创建 `PendingIntent` 未指定 `FLAG_IMMUTABLE`/`FLAG_MUTABLE` 会抛 `IllegalArgumentException`。本项目 `ANDROID_TARGET_SDK=35`、`ANDROID_COMPILE_SDK=31`（`android-app/build-logic/src/main/kotlin/constants/Constants.kt:36,43`）。
- **已修好的对照（原生气泡不崩的原因）**：`bubbles/system/BubbleNotificationManager.kt:125-128` 已用 `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`，注释明确说明 targetSdk 31 起该 flag 为强制。
- **Slide Over 崩溃点**：`browsing/customtabs/CustomTabs.java` 多处 `PendingIntent` 仅传 `FLAG_UPDATE_CURRENT`（如 `:364/373/383/412/424/442/453/466/481/493`），且 `bottombar/BottomBarManager.java:130` 同样问题；这些位于 `CustomTabs.launch()` 打开链接的必经链路。
- **Web Heads 崩溃点**：`bubbles/webheads/WebHeadService.java:206-208` 前台服务通知里三个 `PendingIntent.getBroadcast(... FLAG_UPDATE_CURRENT)`，服务启动必经。
- **穷尽性**：全仓仅 `BubbleNotificationManager.kt` 一处带正确 flag，其余 `PendingIntent` 创建点均缺失。技术方案阶段须**穷举两条崩溃链路上（并建议全仓审计）所有 `PendingIntent` 创建点**逐一补齐，避免漏改仍崩。

## 7. Clarifications（由 Clarify 阶段填充）

### Q1: 三种模式里，哪些崩溃、哪个可用？
**A**: 可用的是 **Native Bubbles（原生气泡）**；崩溃的是 **Slide Over（Custom Tabs）** 与 **Web Heads（悬浮气泡）**。
**影响**: 本次修复目标锁定后两者，验收对这两种模式分别断言不崩溃（S1/S2），并保证 Native Bubbles 不回归（S3）。

### Q2: 崩溃修复与「登录态复用」是否一起做？
**A**: **拆开**。崩溃先修（本 change）；「让原生气泡复用外部浏览器登录态 / 支持 Custom Tabs」另开独立 Issue。
**影响**: 本 change 范围收敛为纯崩溃修复；登录态诉求解耦到新 Issue，避免把确定性 Bug 修复与架构变更耦合。

### Q3: 交付到什么程度？
**A**: **修好崩溃 + 补充测试用例 + 在模拟器上验证通过 + 无问题即合并**。
**影响**: 本链路需落地真实代码改动 + UT（S4）+ 模拟器验证（S5），而非仅出分析报告；验证环境为**模拟器**（非真机）。

### Q4: 基线分支用哪个？
**A**: 取已发布主线 `fork/main`（HEAD `a57031b1`，含原生气泡 targetSdk 方案）作为基线。
**影响**: 见 §8 风险——本地分支 `main-consolidated` 另有单测基础设施修复但未推送且落后主线 48 个提交，二者取舍与是否需要移植单测基建，交技术方案阶段裁定。

## 8. 风险

| 风险 | 可能性 | 影响 | 缓解 |
|---|---|---|---|
| 崩溃点未穷举，改了部分仍在别处崩 | 中 | 高 | 技术方案/开发阶段须全仓审计所有 `PendingIntent` 创建点，不止修抽样到的几处；用 grep 收口「无带 `FLAG_UPDATE_CURRENT` 却缺 IMMUTABLE/MUTABLE 的残留」 |
| 某处 `PendingIntent` 后续需被系统改写（如通知模板），错配 `FLAG_IMMUTABLE` 会引入功能问题 | 低-中 | 中 | 逐点判断可变性语义：本项目这些多为内部广播/跳转，默认 `FLAG_IMMUTABLE`；确有需可变的再用 `FLAG_MUTABLE`，由技术方案阶段判定 |
| 基线分支取舍：`fork/main`（主线、较新）单测基建可能不如本地 `main-consolidated`（含单测修复但落后 48 提交且未推送） | 中 | 中 | 技术方案阶段先在基线上跑一次现有 UT 套件确认可用；若单测基建缺失，评估从 `main-consolidated` 移植 `f5c395fa`/`b037698d` 或另修，确保 S4 可落地 |
| 模拟器难以完整复现 Web Heads 悬浮窗/前台服务权限场景 | 低-中 | 中 | 验证用 Android 12+ 模拟器，必要时授予悬浮窗权限；核心断言为「触发两种模式打开链接不再抛 `IllegalArgumentException` 崩溃」 |
| compileSdk=31 而 targetSdk=35 的构建环境约束（AGP 7.1.2 / JDK） | 低 | 中 | 沿用仓库既有构建配置，不在本 change 动 SDK/AGP；构建/验证按仓库现有流程 |

## 9. 初步圈定的相关代码模块（供技术方案阶段深入，非最终清单）

> 只读源码勘察，用于缩小范围。**修复清单以技术方案阶段全仓审计为准**。

- **Slide Over（Custom Tabs）路径**：`browsing/customtabs/CustomTabs.java`（`prepareChromerOptions()` 及各 `prepare*` 内多处 `PendingIntent`）、`browsing/customtabs/bottombar/BottomBarManager.java:130`、`browsing/customtabs/CustomTabActivity.kt`。
- **Web Heads 路径**：`bubbles/webheads/WebHeadService.java:206-208`（前台服务通知）及该服务内其它 `PendingIntent` 创建点。
- **对照（已正确、勿动）**：`bubbles/system/BubbleNotificationManager.kt:125-128`。
- **SDK 常量（勿动）**：`android-app/build-logic/src/main/kotlin/constants/Constants.kt`。
- **单测基建参考**：本地分支 `main-consolidated` 的 `f5c395fa`（修复单测 DI/Robolectric 基础设施）、`b037698d`（WebViewConfigurator 单测）。

## 10. 关联

- 验收标准（spec-delta）：[specs/link-open-modes/spec.md](./specs/link-open-modes/spec.md)
- 技术方案（待产出）：`design.md`
- 拆出的独立需求：Bubble 支持 Custom Tabs / 复用外部浏览器登录态（新 Issue，见 RAS-54 评论转交记录）
- 仓库 / 分支：`arunkumar9t2/lynket-browser` @ `fix/54-mode-crash-fix`（基线 `fork/main`）

## 变更历史

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-07-16 | 初稿：需求收敛，用户确认「先修崩溃、补 UT、模拟器验证后合并」，登录态诉求拆独立 Issue | 需求官 |
