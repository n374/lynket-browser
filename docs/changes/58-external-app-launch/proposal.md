# RAS-58 · 让 LB 遇到 App 跳转链接时能跳转（含确认框）—— 需求

- **Owner**：需求官（by 需求官 on behalf of n374）→ 技术方案官（设计实现）
- **Reviewers**：编排官、n374
- **状态**：需求已收敛，待技术方案阶段设计实现
- **适用范围**：Lynket 浏览器 Android App，**LB 自有 WebView 承载的网页**（不含系统 Custom Tabs 承载，理由见「范围与边界」）
- **最后更新**：2026-07-17
- **关联 Issue**：RAS-58

## 背景与目标

**用户诉求**：在 Chrome 里浏览网页时，如果页面要跳转到另一个 App（通过 URL scheme / `intent://`），Chrome 会自动跳转（或弹提示）到目标 App。但在 LB 里，遇到这类非 http(s) 链接时「只会去尝试以 URI 的方式打开」——既不跳转 App，也不给任何提示。用户希望 LB 也能跳转，**并且更希望先弹一个确认框问是否跳转**。

**现状根因（已读源码确认）**：

| 承载方式 | 谁控制页面内跳转 | 现状 |
|---|---|---|
| 系统 Custom Tabs（CCT，通常是 Chrome） | CCT provider（Chrome）/ 系统 | Chrome 自身即会处理 `intent://`/自定义 scheme 的 App 跳转——这正是用户在"Chrome 里"看到的自动跳转 |
| **LB 自有 WebView 模式** | **LB 自己** | `WebViewActivity` 的 `WebViewClient` **没有实现 `shouldOverrideUrlLoading`**（`android-app/lynket/.../browsing/webview/WebViewActivity.kt:142-155`）。非 http(s) 链接（`intent://`、`weixin://`、`market://` 等）被当普通 URL 交给 WebView 加载 → WebView 无法处理该 scheme → 打不开、也不跳 App。这就是用户描述的"只尝试以 URI 方式打开" |

即：**用户遇到的"不跳转"是 WebView 模式的缺陷，而 WebView 恰是 LB 唯一能自己掌控、可以修的部分。**（详见「范围与边界」对 CCT 的说明。）

**目标（一句话）**：在 LB 自有 WebView 承载的网页里，检测到指向外部 App 的链接（非 http(s) scheme / `intent://`）时，**弹出确认框询问用户是否跳转**；用户确认后跳转到目标 App，并提供「记住选择」以便后续对同类跳转免打扰。

### 成功指标（可感知）

- 在 WebView 模式打开一个会触发 App 跳转的页面（如带 `intent://` 或 `weixin://` 的落地页），**不再静默失败**：要么弹确认框、要么（用户已记住选择时）直接跳转。
- 用户确认后能正确唤起已安装的目标 App。
- 目标 App 未安装时**不崩溃、不静默卡死**（兜底行为见下）。

## 需求决策（已与用户确认，2026-07-17）

1. 🟢 **默认行为 = 确认框 + 记住选择**。检测到 App 跳转链接时默认弹确认框（例："检测到要用『微信』打开，是否跳转？"）；确认框提供「记住选择 / 不再询问」，勾选后对**该目标 App（或该 scheme）**的后续跳转不再弹框、直接放行。默认不做静默自动跳转。
2. 🟡 **改造范围 = 先把 WebView 模式支持起来**（用户原话："现在的 WebView 肯定是不可以的，你先把它支持了"）。用户同时表达"希望 Custom Web（CCT）也能这么做"——但 CCT 存在平台硬边界，处理见「范围与边界」，本次不承诺 CCT 的 LB 确认框 UX。
3. 🟢 **目标 App 未安装的兜底**：用户判断"系统应会默认跳到 Market 的 scheme"，决定**本期不专门实现 Market 兜底**，留待模拟器实测验证。**但正确性底线不放宽**：未安装导致的 `ActivityNotFoundException` 必须被捕获、给用户明确提示且**绝不崩溃**（fail loud，不静默吞掉）。实测若发现系统并不会自动跳 Market，则在后续迭代补兜底逻辑（`intent://` 的 `browser_fallback_url` / `market://` 构造）。

## 范围与边界

### 本期交付（committed）

- **仅 LB 自有 WebView 承载的网页**：主入口是 `WebViewActivity`（`browsing/webview/`）。技术方案阶段需确认并覆盖其它同样基于 LB 自有 WebView 的承载面（如气泡 Web Heads、文章阅读模式 `ArticleActivity` 若也走 WebView），使同类页面行为一致——具体承载面清单由技术方案官核定。
- 触发条件：页面内导航目标为**非 http/https**、且能解析为一个可唤起的外部组件（含 `intent://` 解析、自定义 scheme 的 `ACTION_VIEW`）。
- 交互：确认框（含「记住选择」）→ 确认后 `startActivity` 跳转 → 未安装则明确提示且不崩溃。

### 明确排除（out of scope）

- ⚠️ **系统 Custom Tabs（CCT / "Custom Web"）模式的 LB 确认框 UX——本期不做，且受平台硬约束**：
  - CCT 页面由系统 CCT provider（Chrome）在其**独立进程**渲染，宿主 App（LB）通过 `CustomTabsIntent` 启动后**无法拦截其页面内导航**——`CustomTabsCallback.onNavigationEvent` 只提供导航**事件通知**（started/finished/failed），**不提供拦截或改写能力**（对照现状 `browsing/customtabs/CustomTabManager.java:178-181` 的空回调）。因此**LB 无法在 CCT 内插入自己的"确认框"并接管跳转**。
  - 好消息：CCT 模式下"跳转到 App"这件事**本就由底层浏览器（Chrome）原生处理**——用户在 CCT+Chrome 下通常已能自动跳转（这与用户在"Chrome 里"看到的行为一致）。所以 CCT 的**自动跳转能力不需要 LB 额外开发**；LB 无法提供的只是"自己的确认框 UX"。
  - 结论：把用户"希望 Custom Web 也能这么做"拆成两层——**自动跳转**：CCT+Chrome 已天然具备，无需 LB 改；**LB 自定义确认框**：CCT 内平台不允许，本期不承诺。若用户后续强需"所有承载面统一的确认框体验"，只能靠"把更多流量导到 WebView 模式"这类产品取舍来达成，属独立需求，另行评估。
- 不改 CCT / 系统层的任何跳转策略。
- 不做跳转链接的安全黑白名单治理（钓鱼 scheme 拦截等），本期只做"检测 → 询问 → 跳转"。

## 方案（需求层面，实现细节留技术方案阶段）

现状代码已有可复用资产，供技术方案官参考（非最终实现约束）：

- **拦截入口**：需在 WebView 的 `WebViewClient` 补 `shouldOverrideUrlLoading`（现状缺失，`WebViewActivity.kt:142-155`）。
- **确认框**：仓库已用 `MaterialDialog`（`about/changelog/Changelog.java`），可作确认对话框；如需展示"可选 App 列表"另有 `IntentPickerSheetView`（`shared/views/`，现由手动菜单项 `OpenIntentWithActivity` 使用）。
- **跳转 + 兜底样板**：`browsing/customtabs/callbacks/SecondaryBrowserReceiver.java:43-99` 已有"`startActivity` 包 try/catch、找不到则 fallback"的模式，可复用其容错骨架。
- **"记住选择"存储**：需要一处轻量持久化（按目标包名 / scheme 记住放行），技术方案阶段定存储位置与粒度。

## 验收

详细可验证条件见 `specs/external-app-launch/spec.md`。要点：

1. WebView 模式遇 App 跳转链接 → 弹确认框（含记住选择），不再静默失败。
2. 确认后正确唤起已安装目标 App。
3. 勾选"记住选择"后，同类跳转不再弹框、直接放行。
4. 目标 App 未安装 → 明确提示且**不崩溃**（正确性红线）。
5. CCT 模式行为不被本次改动破坏（回归）。

## 风险与回退

- ⚠️ **未安装兜底依赖系统默认行为**：本期不实现显式 Market 兜底，依赖"系统/intent 机制默认表现"。此为用户已知取舍，**须在模拟器实测确认**（见 spec 的实测项）；若实测发现不会自动跳 Market，补 `browser_fallback_url` / `market://` 处理。**无论如何，未安装绝不能崩溃**——这条不因"概率低"放宽。
- ⚠️ **承载面枚举风险**：LB 自有 WebView 可能有多个承载面（主浏览、气泡、文章模式），漏改会导致部分场景仍不跳。技术方案阶段须枚举并统一。
- ⚠️ **确认框滥扰**：某些页面可能频繁触发 scheme 跳转，确认框过多会烦人；"记住选择"是主要缓解手段，技术方案阶段需定义记住的粒度（按目标 App / 按 scheme / 按站点）。
- **回退**：本改动局限在 WebView 的 `shouldOverrideUrlLoading` 与确认框，风险面小；若出问题可让 `shouldOverrideUrlLoading` 对非 http(s) 直接 `return false`（恢复现状行为）即回退。

## 相关提交 / 分支

- 分支：`feat/58-external-app-launch`（基于本地主线 `main-consolidated`）
- 变更文档：`docs/changes/58-external-app-launch/`

## 变更历史

- 2026-07-17 需求官：与用户澄清并收敛需求（确认框+记住选择、聚焦 WebView、未安装兜底取舍），产出 proposal / spec，转技术方案阶段。
