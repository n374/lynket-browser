<!-- doc-init template version: v1.0 -->
# Design: 12-jandan-comment-load-fail

- **Owner**: 技术方案官 on behalf of wu.nerd
- **Reviewers**: 编排官, wu.nerd
- **创建日期**: 2026-06-29
- **基于 proposal**: [proposal.md](./proposal.md)
- **Related spec**: [specs/web-page-rendering/spec.md](./specs/web-page-rendering/spec.md)
- **Constitution check**: 已检查 `docs/overview/`，本项目当前**无 `constitution.md`**（docs 体系首次建立，仅有 README/AGENTS 骨架），故无红线条目可冲突。本设计未引入架构重构、未破坏既有渲染路径，符合 proposal 的「仅围绕动态内容加载一致性」边界。
- **交叉评审**: 与 Codex 双线并行产方案 + 两轮对等评审收敛，结论见 §8。

---

## 1. 概述

**问题**：经 Lynket 的 **Custom Tabs 路径（provider=Chrome Dev/Beta）** 打开 `https://jandan.net/t/6169986`，由前端 JS 异步拉第三方接口渲染的**评论区**显示「加载失败」；而**同一 Chrome 引擎**下，独立 Chrome Beta 打开同页正常。

**本设计的核心结论（诚实声明，关键）**：在「Custom Tabs（CCT）」这一已确认复现路径下，**纯代码静态分析无法单独判定根因**——因为页面由外部 Chrome 渲染，Lynket 在普通 CCT 路径上**几乎不改请求**（无自定义 Header / UA / Referer / incognito；普通路径不触发 `mayLaunchUrl` 预取）。因此「同引擎、独立正常、CCT 异常」的差异必然落在**少数 Lynket 可影响的启动上下文**或**Chrome/平台对 CCT 与独立标签页的差异**上，二者只能靠**真机三路径对照 + Chrome 侧网络诊断**来分类。

故本设计采取「**判定路径 + 各分支最小修法 + 验收门槛**」的交付姿态，而非硬押一个未经真机证据支撑的根因（押根因会触碰「禁止幻觉修改」红线）。开发官据 §6 实验结果选定分支落地。这一姿态经 Codex 两轮评审确认为本场景的正确做法。

> 注：Lynket 默认渲染路径**确为 CCT**（`articleMode` 默认 false、`useWebView` 默认 false），与用户复现配置一致；见 `DefaultTabsManager.shouldUseWebView` (`tabs/DefaultTabsManager.kt:343-349`) 与 `Preferences.java` 默认值。

---

## 2. 架构与方案

### 2.1 渲染路径与代码事实（静态分析已确认）

| 路径 | 入口 | 关键事实 | 与本问题关系 |
|---|---|---|---|
| **Custom Tabs（CCT，默认/复现路径）** | `CustomTabActivity.kt` → `CustomTabs.launch()` | 构建的 `CustomTabsIntent` 仅 `setPackage` + `KEEP_ALIVE` extra；**无 Referer / Header / UA / incognito**（`customtabs/CustomTabs.java:213-233, 262-276`）。页面 100% 由外部 Chrome 渲染。 | **主复现路径**，Lynket 可改面窄 |
| **内置 WebView**（fallback / 用户显式开 / incognito 强制） | `WebViewActivity.kt` | **仅** `settings.javaScriptEnabled=true`（`webview/WebViewActivity.kt:167`）；`domStorageEnabled` / `databaseEnabled` 未开；无 `CookieManager.setAcceptThirdPartyCookies`；无 `mixedContentMode`；无自定义 UA。在 **targetSdk 31**（`build-logic/.../Constants.kt:38`）下，WebView 默认：DOM Storage 关、第三方 Cookie 拒绝、mixed content `NEVER_ALLOW`。 | **真实缺陷**，但**不解释 CCT 主复现**；见下 |
| **文章模式（Article）** | `RxParser.kt` + `WebsiteUtilities.java`（Crux + JSoup 静态抓取，固定 iPad UA） | **不执行页面 JS**，结构上不可能渲染 JS-async 评论。默认关闭（`ARTICLE_MODE` 默认 false）。 | 非根因，仅用于排除误路由 |

**关键路由细节**：
- `openBrowsingTab()` 构造 CCT/WebView 意图时**只透传** `data / website / toolbarColor / incognito`，**未透传入站 `Intent.EXTRA_REFERRER`**（`tabs/DefaultTabsManager.kt:319-330`）——这影响下文修 A 的可行性。
- `shouldUseWebView()`：**incognito（含全局隐身）会强制走 WebView**（`tabs/DefaultTabsManager.kt:343-349`），从而落入上表的「WebView 缺陷」。
- CCT 启动失败时 `CUSTOM_TABS_FALLBACK` 兜底到 **WebViewActivity**（`customtabs/CustomTabs.java:97-110`）——即 CCT 失败的兜底路径也踩同一个 WebView 缺陷。
- 普通 CCT 的 `getSession()` **无条件复用** `WebHeadService.getTabSession()`（`customtabs/CustomTabs.java:296-306`）；web-heads 路径会对该 session 调 `mayLaunchUrl` 预取（`bubbles/webheads/WebHeadService.java:389-392` → `customtabs/CustomTabManager.java:117-130`）。这是 Lynket 一个**明确可控、可能造成差异**的点。

### 2.2 根因假设排序（Lynket 可控性 + 证据 + 置信度）

> 排序原则：**代码可确认的、Lynket 明确可控的差异优先**；浏览器实现层不确定的假设靠后。经 Codex 评审调整（H3 上调、H1 下调）。

| # | 假设 | 类型 | 置信度 | 证据 / 机制 |
|---|---|---|---|---|
| **H3** | **web-heads 静态 session / `mayLaunchUrl` 预取污染**：普通 CCT 复用了 web-head 预取过的 session，预取阶段的缓存/cookie/token 状态被后续可见 CCT 继承，导致评论接口拿到异常上下文 | **Lynket 可修** | 中 | `CustomTabs.java:296-306` 无条件取 `WebHeadService.getTabSession()`；`WebHeadService.java:389-392` 调 `mayLaunchUrl`。**这是代码可确认、Lynket 完全可控的差异**——比 H1 更"硬" |
| **H1** | **CCT 启动 referrer / app 发起方身份**与独立 Chrome 直开不一致（Chrome 可能看到 `android-app://arun.com.chromer`），页面 JS / 评论接口按 `document.referrer` / `Referer` / `Sec-Fetch-*` 改变行为或风控分流 | **Lynket 可部分影响** | 低-中（**降级**） | `CustomTabs.java:213-233` 不设 `EXTRA_REFERRER`。**风险**：`Intent.EXTRA_REFERRER` 官方语义只保证「Activity 级发起方」，**不保证**会变成网页可见的 HTTP `Referer` / `document.referrer`——修 A 可能落空，须真机验证三者是否真变化 |
| **H2** | **平台层差异**：Android 17 + Chrome 对 CCT 与独立标签页采用不同的存储分区 / 第三方 Cookie / Tracking Protection 策略，评论接口的第三方请求在 CCT 被 Chrome 拦截 | **平台不可修** | 中 | Lynket CCT 无独立 cookie/网络栈，由 Chrome 驱动并共享浏览器状态。若成立，Lynket 无法"修 Chrome"，**触发回退** |
| **H4** | **误路由 / provider 不一致**：实际未走预期 CCT（落到 WebView / Article / incognito），或 Lynket-CCT 用的 Chrome 包/版本与"对照独立 Chrome"不是同一个 | **Lynket 可修 / 可排除** | 低 | `getCustomTabPackage` 按偏好选 stable/local/beta/dev（`CustomTabs.java:152-167`）；incognito 强制 WebView（见上）。若误入 WebView，§2.1 的 WebView 缺陷即直接致评论失败 |

### 2.3 分支修复方案（按 §6 实验结果二选一/组合落地）

> 每个分支都给「落点 + 改法 + 是否可单测」。**不预先全量改**——避免在未命中的分支上引入无谓改动（红线：禁止制造无效改动 / 安全语义放宽）。

**修 A —— H1 命中（CCT referrer/启动上下文）**
- **前置**：先按 §6 确认入站 Intent 是否带 `EXTRA_REFERRER` / `EXTRA_REFERRER_NAME` / `EXTRA_ORIGINATING_URI`，并用 DevTools 确认 `document.referrer` / 接口 `Referer` 是否真随之变化。**若不变化则 H1 落空，不做修 A。**
- **落点**：`tabs/DefaultTabsManager.kt:319-330`（透传入站 referrer 到 `CustomTabActivity`）+ `customtabs/CustomTabs.java:213-233`（`openCustomTab()` 给 `customTabsIntent.intent` 设 `Intent.EXTRA_REFERRER`）。
- **改法**：保留并透传上游 referrer；必要时加「CCT 兼容」开关，按**目标 URL 自身 origin** 设置 referrer。**禁止**用 `Browser.EXTRA_HEADERS` 塞 `Referer`（官方明确：标准 HTTP 头不可经此设置，会被忽略）。
- **可单测**：是（断言 intent extras）。

**修 B —— H3 命中（web-head session / 预取隔离）**
- **落点**：`customtabs/CustomTabs.java:296-306`（`getSession`）、`bubbles/webheads/WebHeadService.java:374-392`。
- **改法**：普通前台 CCT **不再隐式复用** web-head 静态 session，仅当确为「从 web-head 点击进入」时显式 `.withSession()`；隔离/修正 `mayLaunchUrl` 对前台 CCT 的影响。
- **可单测**：是（断言普通 CCT 不携带 web-head session 标记）。

**修 C —— WebView 加固（拆 C1/C2/C3，安全语义分级）**
> ⚠️ **措辞红线**：修 C **不是 CCT 主复现的根因修复**，**不得**作为主问题关闭条件。它只提升「CCT 失败兜底 / 用户显式选 WebView / incognito 强制 WebView」三类场景的兼容性，并泛化修同类 JS-async 站点在 WebView 下的加载。主问题关闭须以 CCT 路径命中修 A/B、或确认 H2 后产品回退策略通过为准。
- **落点**：`webview/WebViewActivity.kt:138-174`（建议抽 `WebViewConfigurator` 便于单测）。
- **C1（无条件做，低风险）**：`settings.domStorageEnabled = true`（minSdk 23 全可用）。这是 JS-async 内容最常见的失败点，低风险、可单测、泛化收益明确。
- **C2（仅实验命中且受控）**：`CookieManager.setAcceptThirdPartyCookies(webView, true)` —— **第三方 Cookie 是隐私语义变化**，仅在 DevTools 证实评论接口因 3PC 被拦时启用，且建议**域名级 / 开关控制**，不全局放开。
- **C3（仅确认依赖 HTTP 子资源时）**：`mixedContentMode = COMPATIBILITY_MODE` —— **不要** `ALWAYS_ALLOW`；仅当实测站点确依赖 HTTP 子资源时启用。
- **可单测**：是（Robolectric 断言各 WebSettings 取值）。

**回退分支 —— H2 命中（平台不可修）**
- **判定信号**：最小 AndroidX CCT sample 打同 URL **也失败** + `chrome://net-export` 显示 Chrome 在 CCT 模式因 Android 17 / Chrome 隐私策略阻断第三方 cookie/storage/credential。
- **动作**：按父 Issue **中/重档回退**，向用户重新对齐预期；产品兜底可提供「用 Chrome 打开」显式 `ACTION_VIEW`（复用 `OpenInChromeReceiver`）作为 workaround，而非声称已修复。

---

## 3. 关键决策

| 决策点 | 选择 | 备选 | 理由 | 关联 ADR |
|---|---|---|---|---|
| 设计阶段是否硬押单一根因 | **否**，给判定路径+分支修法 | 押 H1 直接改 referrer | CCT 由 Chrome 渲染，代码证据不足以定根因；硬押触碰「禁止幻觉修改」红线。Codex 两轮评审一致认同 | 暂无 |
| 修 C 是否无条件全量放开 | **拆 C1/C2/C3 分级** | 一次性开 domStorage+3PC+mixedContent | 3PC / mixed content 是隐私/安全语义变化（targetSdk 31 默认拒绝），无条件放开越界 | 暂无 |
| Referer 注入手段 | `Intent.EXTRA_REFERRER`（验证后） | `Browser.EXTRA_HEADERS["Referer"]` | 标准 HTTP 头不能经 EXTRA_HEADERS 设置，会被忽略 | 暂无 |
| 假设优先级 | H3 > H1 | H1 > H3 | H3 是代码可确认、Lynket 完全可控的差异；H1 依赖浏览器实现、可能落空 | 暂无 |

---

## 4. 影响分析

### 4.1 受影响的 capability

| Capability | 影响类型 | 需更新 spec |
|---|---|---|
| `web-page-rendering` | 实现（满足 spec-delta 既有验收项） | 否（spec-delta 已覆盖；归档前替换 TBD 覆盖测试为真实路径） |

### 4.2 受影响的接口 / 模块

| 模块 | 影响 | 兼容性 |
|---|---|---|
| `tabs/DefaultTabsManager.kt`（修 A） | 透传 referrer 到 CCT 意图 | 向后兼容（新增 extra，旧路径不受影响） |
| `customtabs/CustomTabs.java`（修 A/B） | 注入 referrer / 调整 session 复用 | 需回归 web-heads 点击进入路径 |
| `bubbles/webheads/WebHeadService.java`（修 B） | 预取 session 隔离 | 需回归气泡预热体验 |
| `webview/WebViewActivity.kt`（修 C） | 新增 WebSettings | 行为增强；C2/C3 涉及隐私/安全，需受控 |

### 4.3 受影响的运维

无监控/告警/SOP 变更。交付门槛是真机验证（见 §5、spec-delta NFR-1），非线上运维项。

---

## 5. 测试策略

| 测试类型 | 范围 | 关联 Requirement |
|---|---|---|
| 真机三路径对照实验（§6） | 定位根因、分类分支 | 基线页加载一致性 / 同类泛化 |
| 单元测试（Robolectric 4.3 + JUnit4） | 命中分支的修复点 | 「根因改动须有 UT 覆盖」 |
| 回归冒烟 | CCT 正常网页 / 文章模式 / web-heads | 「既有渲染路径无回归」 |
| 真机端到端验证（Android 17） | 修复前可稳定复现失败、修复后稳定成功；同类站点抽样 | NFR-1 真机验证 |

**UT 落点（按命中分支）**：
- 路由 UT：参考 `src/test/java/arun/com/chromer/tabs/DefaultTabsManagerTest.kt`，用 `shadowOf(application).nextStartedActivity` 断言「关 WebView/incognito/article 时启动 `CustomTabActivity`；开 incognito/useWebView 时启动 `WebViewActivity`」。
- 修 A：把 CCT 兼容 extra 抽成纯 helper，JUnit4 断言 `EXTRA_REFERRER`/`EXTRA_REFERRER_NAME` 写入、且**未**写 `Browser.EXTRA_HEADERS["Referer"]`。
- 修 B：断言普通 CCT 不携带 web-head session、仅 `fromWebHeads` 路径携带。
- 修 C：抽 `WebViewConfigurator`，Robolectric 构造 WebView 后断言 `domStorageEnabled`（及命中时的 3PC/mixedContent）取值。
- 基类参考 `src/test/java/arun/com/chromer/LynketRobolectricSuite.kt`（RxScheduler Trampoline + `clearPreferences()`）。
- 注：动态网络渲染本身难纯单测覆盖，端到端评论加载以真机验证补足（与 proposal 风险表一致）。

---

## 6. 决定性真机实验（开发官必做，本设计核心交付）

> 目的：用一次最小实验把 H1–H4 分类，锁定可修分支或确认回退。**先确认走的确实是 CCT**，再谈根因。

**前置确认（排除误路由 / provider 不一致，对应 H4）**：
- 看 Timber 日志 `CustomTabs.java:224 "Launched url: %s"` 确认确实走 `openCustomTab()`（而非 WebView/Article/web-heads）。
- 记录 `getCustomTabPackage()` 实选包名 + Chrome versionCode + 是否复用了 web-head session；确保「对照独立 Chrome」与「Lynket-CCT」是**同一个 Chrome 包/版本**。

**对照矩阵（同设备 Android 17、同网络、同 URL）**：
1. 独立 Chrome Beta 普通标签页打开 → 基线（应正常）。
2. Lynket-CCT，provider 固定 `com.chrome.beta`，**关闭** WebHeads / Article / AMP / Incognito / UseWebView / AggressiveLoading。
3. Lynket-CCT，**WebHeadService 活跃**（先弹气泡再打开）对照「不活跃」→ 区分 H3。
4. Lynket 强制 WebView 打开同 URL → 验证 §2.1 WebView 缺陷、为修 C 取证。
5. Article 模式打开 → 预期不渲染 JS 评论，仅排除误路由。

**Chrome 侧诊断**：
- 桌面 `chrome://inspect/#devices` 分别 inspect 普通 tab 与 CCT tab：比对 `document.referrer`、console 报错、Network 中**评论接口**的 URL/status/response、CORS / cookie-blocked reason。
- Chrome Beta `chrome://net-export` 抓两份 NetLog，比对评论接口请求头 `Referer` / `Origin` / `Cookie` / `Sec-Fetch-Site|Mode|Dest`。
- 临时实验开关（三选一单独验证哪个让 CCT 恢复）：禁用 web-head session / 禁用 `mayLaunchUrl` / 给 CCT 设 `EXTRA_REFERRER`。
- 最小 AndroidX CCT sample 打同 URL：也失败→偏平台（H2，回退）；正常→偏 Lynket（H1/H3）。

**判定 → 分支**：实验命中 H3→修 B；命中 H1（且 referrer 真生效）→修 A；命中 H4→修正路由 + 修 C；确认 H2→回退。修 C1（domStorage）无论如何作为 WebView 兜底基线落地，但**不计入主问题闭环**。

---

## 7. 红线检查

- [x] 本 change 是否触及 `constitution.md` 红线：项目**无 constitution.md**，无可触及红线。
- [x] 未破坏既有功能：分支修复均保留既有 CCT/WebView/Article/web-heads 路径，配套回归冒烟。
- [x] 未制造重复 / 未幻觉 API：所有引用代码坐标已 `Read` 核实；`EXTRA_REFERRER` / `EXTRA_HEADERS` / WebSettings 行为均按官方语义与 targetSdk 31 默认核对。
- [x] 不放宽安全语义：修 C 的 3PC/mixedContent 受控分级，不无条件放开。

---

## 8. Codex 交叉评审摘要

**协作方式**：与 Codex 双线并行盲产方案 → 两轮对等评审收敛（≤3 轮内达成，无高级别未决分歧，无需回退）。

**共识（直接合并）**：
- CCT 主复现**不可由代码静态定根因**，须真机三路径对照 + Chrome 侧 DevTools/NetLog 诊断分类。
- 设计阶段**不硬押根因**、给「判定路径 + 各分支最小修法 + 验收门槛」是本场景的正确做法。
- `Browser.EXTRA_HEADERS` **不能**塞标准 `Referer` 头。
- WebView 仅开 JS、domStorage 等默认关，是真实缺陷但**不解释 CCT 主复现**。
- 文章模式不执行 JS，非根因。

**采纳的 Codex 意见（已落入本设计）**：
1. **H1（referrer）降级**：`Intent.EXTRA_REFERRER` 官方只保证 Activity 级发起方语义，不保证网页 `Referer`/`document.referrer` 真变化 → 降为「低成本可验证假设」，实验须先验证 referrer 是否真传导（§2.2 H1、§2.3 修 A 前置）。
2. **「保留上游 referrer」可能无对象**：`DefaultTabsManager` 未透传入站 referrer（已核实 `:319-330`）→ 修 A 前先确认入站 Intent 是否带 referrer（§2.3 修 A 前置）。
3. **修 C 拆 C1/C2/C3**：domStorage 无条件做，3PC/mixedContent 受控分级（隐私/安全语义，targetSdk 31 默认拒绝）（§2.3 修 C）。
4. **修 C 措辞防误导**：明确「非 CCT 主问题修复、不作关闭条件」（§2.3 修 C 红线框）。
5. **H3 上调 + 实验加 web-head 活跃/不活跃维度**：H3 是代码可确认、Lynket 完全可控的差异，优先级置于 H1 之上（§2.2、§6 矩阵 3）。
6. **实验加 provider 包/版本一致性检查**（§6 前置确认）。
7. **WebView 默认描述按 targetSdk 31 条件化**（已核实 targetSdk=31，§2.1 表）。

**反驳 / 不采纳**：无实质反驳——Codex 两轮意见均为证据充分的细化，全部采纳或已在我方初稿覆盖。UA 白名单假设：双方一致认为对 CCT 主复现优先级低（CCT 即 Chrome UA），仅 WebView fallback 才可能涉及，故未单列为主假设。

**评审轮数**：2 轮收敛。

---

## 9. 风险

| 风险 | 缓解 |
|---|---|
| 根因落在 H2（平台不可修），Lynket 无法消除 | §6 用最小 CCT sample + NetLog 明确判定；命中即按父 Issue 中/重档回退，向用户重新对齐预期，不硬凑方案 |
| 修 A 的 `EXTRA_REFERRER` 对网页 `Referer` 落空 | 列为前置验证项，不生效则不做修 A，避免无效改动 |
| 修 C 的 3PC/mixedContent 放宽引入隐私/安全回退 | 拆分受控，仅实验命中才启用，且域名级/开关控制 |
| 「同类问题」边界扩散为长尾 | 以 spec-delta S2「抽样 ≥1 同类站点 + 与对照一致」收口 |
| 动态渲染难纯单测 | UT 聚焦可断言的配置/意图层；端到端以真机验证补足 |
| 真机验证依赖设备 + jandan 第三方接口在线 | 固定设备/网络，记录验证时接口状态，备用同类站点 |

---

## 关联

- 关联 proposal: [proposal.md](./proposal.md)
- 关联 spec-delta: [specs/web-page-rendering/spec.md](./specs/web-page-rendering/spec.md)
- 关联 ADR: 暂无
- 仓库 / 分支: `arunkumar9t2/lynket-browser` @ `feat/12-jandan-comment-load-fail`
