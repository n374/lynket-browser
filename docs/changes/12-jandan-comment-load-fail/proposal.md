<!-- doc-init template version: v1.0 -->
# Proposal: 12-jandan-comment-load-fail

- **Owner**: 需求官 on behalf of wu.nerd
- **Reviewers**: 编排官, wu.nerd
- **创建日期**: 2026-06-29
- **状态**: Clarify（需求已收敛，待进入技术方案 Plan 阶段）

## 1. Why（动机）

用户在 Lynket Browser（LB，基于 Chrome Custom Tabs / WebView 的 Android 浏览器）中打开煎蛋文章页 `https://jandan.net/t/6169986` 时，页面下方的**评论区**只显示「加载失败，点击重新加载」，无法渲染评论内容；而在对照浏览器（独立运行的 **Chrome Beta**）中打开同一页面，评论区正常加载。

期望状态：在 Lynket 中打开此类页面时，评论区能与对照浏览器一致地正常加载与渲染。煎蛋评论区通过前端 JavaScript **异步拉取第三方评论接口**渲染——这是大量站点的通用模式，本问题大概率不是 jandan 独有，修复其根因可同时改善「同类站点」的浏览体验，因此值得现在做。

## 2. What's Changing（高层变更）

| Capability | 变化类型 | 简述 |
|---|---|---|
| `web-page-rendering` | ADDED | 新增「页面 JS 异步加载的动态内容（含第三方评论区）须与对照浏览器一致地成功加载」的可验收需求 |

**新增 capability**：
- `web-page-rendering`：Lynket 打开目标 URL 并渲染其内容（含 JS 异步加载的动态内容）的能力。当前 `docs/` 为首次建立，该 capability 此前无 living spec，故以 ADDED 形式引入其首批需求。

> 注：本阶段（需求官）**只定义"做成什么样"（验收）**，不锁定根因与具体改法。根因定位与实现路径由后续技术方案（design.md）/ 开发阶段基于**真机实复现**给出。父 Issue 明确要求「以实际代码与复现为准，禁止臆断」。

## 3. Out of Scope（明确不做）

- **不修**对照浏览器（独立 Chrome Beta / Chrome Dev）自身的行为——它是基线，不是被改对象。
- **不负责** jandan 服务端或其第三方评论接口本身的可用性问题（外部依赖）。
- **不新增**广告拦截 / 隐私拦截功能（现有代码库经核查并无此类拦截逻辑，不在本次引入）。
- **不重写** Article 文章模式的内容提取算法（Crux + JSoup），除非技术方案阶段的实复现证明它正是本问题根因。
- **不做** Lynket 的整体架构重构；仅围绕"动态内容加载一致性"这一问题域改动。

## 4. Stakeholders

| 角色 | 关注点 | Review 必需 |
|---|---|---|
| wu.nerd（用户/发起人） | 评论区在真机能正常加载；同类问题被一并修复 | 是 |
| 编排官 | 阶段衔接、链路推进 | 是 |
| 技术方案官 | 根因定位、修复方向可行性 | 是（下阶段主理） |
| 开发官 / MR官 | 落地实现、UT 覆盖、真机验证 | 后续阶段 |

## 5. Success Metrics（成功指标）

- **S1（主基线）**：在 Android 17 真机、相同网络环境下，经 Lynket 打开 `https://jandan.net/t/6169986`，评论区**完成加载并渲染出评论内容**，不再停留在「加载失败，点击重新加载」。
- **S2（同类泛化）**：对至少 1 个**同类站点**（依赖 JS 异步拉取第三方接口渲染主要内容）进行抽样验证，其动态内容加载结果与对照浏览器一致（由技术方案阶段确定具体抽样站点）。
- **S3（无回归）**：既有打开/渲染路径（Custom Tabs 正常网页、文章模式、Web heads）功能不被破坏。
- **S4（工程门槛）**：根因相关改动有**单元测试（UT）覆盖**；并通过**真机验证**（Android 17）。

## 6. Clarifications（由 Clarify 阶段填充）

### Q1: 复现时 Lynket 用的是哪条渲染路径？
**A**: 用户使用 **Chrome Dev 和 Chrome Beta** 作为浏览器（即 Custom Tabs 路径，转交外部 Chrome 渲染）；内置 WebView 路径是否同样有问题**用户未确认**。
**影响**: 确认的复现路径是 **Custom Tabs → 外部 Chrome**，而非内置 WebView。这与「同一 Chrome 引擎下，独立 Chrome 正常、经 Lynket 的 Custom Tab 异常」形成关键对比——技术方案阶段须据此优先排查 **Custom Tabs 与独立浏览器的会话/Cookie/存储/Referer 等差异**，并需在真机上跨三条路径（Custom Tabs / 内置 WebView / 文章模式）做对照以隔离根因。**不得**沿用需求澄清前「内置 WebView 配置缺失」的初判作为结论。

### Q2: 对照基线是哪个浏览器？
**A**: **独立运行的 Chrome Beta**（正常）。
**影响**: 验收以"与独立 Chrome Beta 行为一致"为判据（S1）。

### Q3: 修复范围是 jandan 单站还是同类问题？
**A**: **定位到根因，修复同类问题**（不局限 jandan 单站）。
**影响**: 实现取"治本"方向，验收基线锚定 jandan 页（S1）并抽样验证同类站点（S2）。

### Q4: 交付到什么程度？
**A**: **要真实修复，并 UT 覆盖，然后真机验证**。
**影响**: 本链路需落地真实代码改动 + UT + 真机验证，而非仅出分析报告（S4）。

### Q5: 设备/系统环境？
**A**: **Android 17**。
**影响**: 真机验证基线为 Android 17；第三方 Cookie / mixed-content / 存储分区等默认策略须以 Android 17 实际行为为准（该版本较新，不得照搬旧版本文档结论）。

## 7. 风险

| 风险 | 可能性 | 影响 | 缓解 |
|---|---|---|---|
| 复现路径是 Custom Tabs（外部 Chrome），Lynket 自身渲染由 Chrome 完成，**Lynket 侧可改的面较窄**（仅 Custom Tabs 启动/会话配置） | 中 | 高 | 技术方案阶段先真机隔离：根因究竟在 Custom Tabs 启动参数/会话、还是路由意外落到 WebView/文章模式；据此再定改动落点 |
| 根因可能是 Custom Tabs 第三方启动场景下的**会话/Cookie/存储分区**差异，受 Android 17 + Chrome 版本影响，Lynket 不一定能完全消除 | 中 | 高 | 若确证不可由 Lynket 侧修复，须回退到需求阶段重新对齐范围/预期（中/重档回退） |
| 「同类问题」边界不清，易扩散为无止境的兼容性长尾 | 中 | 中 | 以 S2「抽样 ≥1 同类站点 + 与对照浏览器一致」收口，具体站点由技术方案阶段圈定 |
| 真机验证依赖可复现设备 + jandan 第三方接口在线可用性（外部依赖波动） | 中 | 中 | 固定验证设备/网络；记录验证时的接口状态；必要时准备备用同类站点 |
| 动态网络渲染（WebView/Custom Tabs 行为）**难以纯单测覆盖** | 中 | 中 | UT 聚焦可断言的配置/启动参数层（如 Robolectric 断言 WebSettings / CustomTabsIntent 构造），端到端评论加载以真机验证补足 |
| Android 17 为较新版本，平台默认策略与现有文档/经验可能不一致 | 低-中 | 中 | 一切以 Android 17 真机实测为准，不臆断 |

## 8. 初步圈定的相关代码模块（供技术方案阶段深入，非结论）

> 以下为只读源码勘察结果，用于缩小排查面，**不代表已定位根因**。

- **Custom Tabs 启动配置（复现路径，优先排查）**：`CustomTabs.java`（`getCustomTabPackage` 选 provider、`openCustomTab`/`prepare` 构建 `CustomTabsIntent`，当前**未传 Referer/Headers**）、`CustomTabActivity.kt`、`CustomTabManager.java`（会话连接/预热）。
- **内置 WebView 路径（用户未确认是否同样受影响）**：`WebViewActivity.kt:138-174`（仅 `javaScriptEnabled=true`，**未显式开启** DOM Storage / 第三方 Cookie / mixed-content 策略 / 自定义 UA）、`EmbeddableWebViewActivity.kt`。
- **文章模式（疑似可排除，因其不执行页面 JS）**：`RxParser.kt`（Crux + `Jsoup.connect().get()` 静态抓取）、`WebArticleNetworkStore.java`、`WebsiteUtilities.java`（固定 iPad UA、无 Referer）。
- **Provider 选择 / 路由 / 偏好**：`DefaultTabsManager.kt`（含 `articleMode()` 判定）、settings 模块。
- **拦截/过滤**：经核查**未发现**任何 AdBlock / host 黑名单 / URL 拦截代码，故"拦截误伤评论接口"在本库基本可排除。

## 9. 关联

- 验收标准（spec-delta）：[specs/web-page-rendering/spec.md](./specs/web-page-rendering/spec.md)
- 技术方案（待产出）：`design.md`
- 仓库 / 分支：`arunkumar9t2/lynket-browser` @ `feat/12-jandan-comment-load-fail`
