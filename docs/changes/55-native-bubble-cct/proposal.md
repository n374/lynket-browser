# RAS-55 · 让原生气泡(Native Bubbles)支持外部浏览器 —— 真机 Spike 需求

- **Owner**：需求官（收敛需求）→ 技术方案官（设计 spike 实现）
- **状态**：需求已收敛，待技术方案阶段设计 spike 实现
- **适用范围**：Lynket 浏览器 Android App，仅原生气泡（Native Bubbles）渲染链路
- **最后更新**：2026-07-16
- **关联**：RAS-54（Slide Over / Web Heads 崩溃修复，`in_review`）、RAS-38（原生气泡前台浮出 spike）；本 issue 从 RAS-54 拆出

## 背景与目标

用户诉求：**在原生气泡（Android 系统 Bubbles）里用上「所选外部浏览器（如 Chrome）的登录态」**，且**保留气泡浮窗形态**（用户明确表示 Slide Over 全屏体验不如悬浮气泡）。

经三方交叉调研（Android 官方 Bubbles 文档 + Lynket 源码/manifest + Chromium 安全模型）与一次 Fable 5 对抗式复攻，结论收敛为：

- 「读外部浏览器 cookie 注入气泡内置 WebView」「跨 App Activity Embedding」「TWA 进气泡」「WebView 借 Chrome profile」——**四条注入/嵌入路径均被平台实锤封死**。
- **唯一未被平台禁止、值得一试的路径（下称"路径 2"）**：气泡展开的 Lynket 自有壳 Activity，用 **activity context 且不带 `FLAG_ACTIVITY_NEW_TASK`** 启动所选浏览器的 Custom Tab。官方 Bubbles 文档原文（已由需求官 WebFetch 复核）：

  > "When a bubble launches a new activity, the new activity will either launch **within the same task and the same bubbled window**, or in a new task in fullscreen, collapsing the bubble that launched it."
  > 落在"留在气泡窗"一支的条件正是：用 activity context 启动 + 不设 `FLAG_ACTIVITY_NEW_TASK`。

  Chrome 的 Custom Tab 默认"待在调用方的 task 里"。**若**系统 UI 真把这个跨 App 的 CCT 渲进气泡浮窗 → 气泡内即为真外部浏览器渲染 + 该浏览器登录态，且不破坏浮窗形态。

> ⚠️ **这是待验证假设，不是充分条件**：「activity context + 不带 `FLAG_ACTIVITY_NEW_TASK`」只是**必要**前提之一。跨 App 的 CCT 能否真的嵌进气泡窗，还受目标浏览器 CCT Activity 的 manifest（`launchMode` / `taskAffinity` / 是否 `allowEmbedded`）、系统 SystemUI 的 bubble/TaskView 对跨 UID Activity 的策略、CCT provider 自身附加 flags 等多重因素影响。Lynket 自己的 `EmbeddableWebViewActivity` 声明了 `allowEmbedded/resizeable/documentLaunchMode`，但**外部浏览器的 CCT Activity 未必满足同一嵌入语义**。因此本条只能真机实测证伪/证实。

**关键未知（本 spike 要钉死的点）**：系统气泡的 TaskView 面对**另一个 App(浏览器)的 Activity** 入栈时，是照常渲进浮窗，还是把气泡塌成全屏 / 拒绝。官方文档不区分同 App / 跨 App，也检索不到公开成功或失败案例——**只能真机实测**。

### 登录态验证的口径（诚实校准，已含 Codex 对抗评审意见）

- **决定性证据 ≠ 探针页**：判定「气泡内确由外部浏览器渲染」的**决定性证据是 `dumpsys activity activities`**——气泡展开后 resumed 的顶层 Activity 是**目标浏览器包的 Custom Tab Activity**、且位于气泡所在 task/window。探针页的 UA 仅为**辅助**信号（UA 可被改写，"无 wv"不能单独证明是 CCT）。
- **零登录探针只证"同源一方存储连续"**：探针页 cookie/localStorage 令牌相等，严格说只证明「同源、JS 可读存储在本次普通导航下与独立浏览器连续」，是登录态可复用的**必要且强指示**信号，但不直接等于「真实登录态（含 `HttpOnly`/服务端 session/`Secure`/SameSite）全部可复用」。用户已明确"不一定需要登录、写最简页面记录信息即可"，故**采用零登录探针 + dumpsys 包名/栈证据**组合作为本 spike 验收（见 spec）；若后续要对"真实登录态"下最终结论，可选加一个 HTTPS 受控站点的真实 session cookie 哨兵（本 spike 非必需）。
- **推断链的收口**：一旦 dumpsys 证明「渲染者就是目标浏览器的 CCT」，则按 Chromium 设计该 CCT 天然使用该浏览器的完整 profile（含登录态）——登录态复用由此成立，探针存储令牌连续性作为佐证。

### 本 spike 目标（唯一目标）

在真机 / 模拟器上验证并给出**可行 / 不可行**的铁证：从原生气泡展开时用「所选外部浏览器 + Custom Tab」渲染目标页，观察：
1. 页面是否**真由外部浏览器 CCT 渲染**（而非 App 内置 WebView）；
2. 是否**与该浏览器共享会话/cookie**（登录态可复用的前提）；
3. 是否**仍以气泡浮窗形态呈现**（不塌全屏）。

## 非目标

- 不在本 spike 里做正式功能化（设置开关、多站点适配、产品化 UI）——那是 spike 通过后的后续 feature。
- 不追求「compileSdk 对齐 35」等构建链迁移（沿用 RAS-38 结论：targetSdk 已满足门禁即可）。
- 不做「读取 Chrome cookie 注入 WebView」等已被实锤封死的路径。
- 不要求真实账号登录：登录态验证用下述**零登录探针页**代理（用户已确认"不一定需要登录"）。

## 方案（需求层面，实现细节留技术方案阶段）

### 待验证改动骨架（供技术方案官细化，非最终实现）

- 新增一个满足气泡嵌入声明（`allowEmbedded=true` / `resizeableActivity=true` / `documentLaunchMode=always`）的薄壳 Activity（可复刻现有 `CustomTabActivity.kt` 的 "启动 CCT + ghost-tab 自杀" 模式）。
- 把 `bubbles/system/BubbleNotificationManager.kt:115` 气泡 `viewIntent` 从 `EmbeddableWebViewActivity` 指向该薄壳。
- 薄壳 `onCreate` 内用 **activity context、不带 `FLAG_ACTIVITY_NEW_TASK`** 启动 `CustomTabsIntent`，`setPackage` 成用户所选后端浏览器（复用 `CustomTabs.getCustomTabPackage()`）。
- ⚠️ 注意去掉 Lynket 现有 `mergeTabs` 路径 `DefaultTabsManager.kt:292-295` 会附加的 `FLAG_ACTIVITY_NEW_DOCUMENT/NEW_TASK`，否则必然塌全屏。

### 基线分支

本 spike worktree 基于 `build/apk`（自带可出安装包的 CI，`.github/workflows/build-apk.yml`），与 RAS-38 spike 一致。若技术方案阶段判断需叠加 RAS-54 的崩溃修复，可 rebase 到 `fix/54-mode-crash-fix`。

## 验收（详细可验证条件见 spec，此处给要点）

探针页：`docs/changes/55-native-bubble-cct/assets/login-state-probe.html`（纯静态、零登录、本地可托管，含 cookie 写入复读校验、cookie/localStorage 令牌独立生成）。

**验收前置条件（必须先钉死，否则实验不可复现 / 假阴）**：目标后端浏览器**包名 + 版本号**固定；用**普通 profile、非隐身、未启用 ephemeral CCT**；实验前**清空探针 origin 的站点数据**从基线起测；独立浏览器与气泡两次打开必须**同一 URL / 同一 scheme / 同一 port**。托管建议：模拟器上 `python3 -m http.server`，emulator 内访问 `http://10.0.2.2:<port>/login-state-probe.html`（同源才可比对）。

**通过标准（三条全绿方算该"版本×浏览器"组合下路径 2 可行）**：
1. 🟢 **浮窗形态**：气泡展开后仍是浮窗（未塌全屏、未崩溃）。
2. 🟢 **引擎（决定性=dumpsys，非 UA）**：`dumpsys activity activities` 显示气泡内 resumed 顶层 Activity 是**目标浏览器包的 Custom Tab Activity**，且位于气泡所在 task/window（记录 package / activity / taskId / 与 Lynket 壳 Activity 的栈关系）。探针页 UA「无 wv」仅作辅助佐证。
3. 🟢 **存储共享**：cookie 令牌（复读校验 OK）与 localStorage 令牌**分别**与"独立浏览器直开"一致；两者分开记录（cookie 共享而 localStorage 不共享是可能情形）。

**证据要求（逐"版本×浏览器"留档）**：截图（气泡形态 + 探针页各项值）+ `adb logcat | grep BUBBLE_PROBE` + `dumpsys activity bubbles`（bubble 仍 expanded）+ `dumpsys activity activities`（CCT 包名落在气泡 task）。

**结论按矩阵分级（不得单点外推）**：对 Android 14 / 15 / 16（有 17 更好）× 目标浏览器版本逐格出「可行 / 不可行 / 具体失败态」。**只在某一格成功 ⇒ 结论是"仅该版本×该浏览器可行"，不得写成"路径 2 全面可行"**；单格成功仅证明"平台未全面封死"。

## 风险与回退

- ⚠️ **依赖未承诺行为**：即便 spike 通过，路径 2 依赖"文档未禁止但未保证"的跨 UID 气泡渲染；Chrome / 系统 UI 更新可能使其退化为全屏（退化后行为 ≈ Web Heads，属**可接受降级**，非正确性破坏）。技术方案阶段需设计"塌全屏则优雅回退"的兜底。
- ⚠️ **OEM / Chrome 版本差异**：某厂商 / 某 Chrome 版本可能表现不同；证据矩阵需覆盖多版本。
- **回退等价满足（spike 不通过时交用户选）**：
  - A. **Android 17 系统级"气泡化任意 App"**：用户长按 Chrome 图标即可把 Chrome 本体气泡化（零开发、完整登录态；仅 Android 17、且由用户手动触发，无第三方 API）。
  - B. **Web Heads**（RAS-54 修好后）：悬浮气泡图标 → 点开所选浏览器全屏、带登录态。
  - C. **气泡内置 WebView 自身持久登录**（登录一次持久；拿不到外部登录态，且 Google 系 OAuth 可能被 `disallowed_useragent` 拦）。

## 相关提交 / 分支

- 分支：`spike/55-native-bubble-cct`（基于 `build/apk`）
- 变更文档：`docs/changes/55-native-bubble-cct/`

## 变更历史

- 2026-07-16 需求官：收敛为"路径 2 真机 spike"需求，产出 proposal / spec / 零登录探针页，转技术方案阶段。
