# Spec · WebView 承载页的外部 App 跳转（检测 + 确认框 + 记住选择）

- **Capability**：`external-app-launch`
- **Owner**：需求官 → 技术方案官
- **状态**：待实现
- **最后更新**：2026-07-17
- **上游需求**：`docs/changes/58-external-app-launch/proposal.md`

## 范围

仅覆盖 **LB 自有 WebView 承载**的网页在页面内导航到**外部 App 链接**（非 http(s) scheme / `intent://`）时的处理。系统 Custom Tabs（CCT）承载**不在**本 capability 范围（平台无法拦截，见 proposal「范围与边界」）。

## 术语

- **App 跳转链接**：WebView 页面内触发的导航目标，其 scheme **非 `http`/`https`**（如 `intent://`、`weixin://`、`alipays://`、`market://`、`mailto:` 等），且能被解析为一个可由外部组件处理的 `Intent`。
- **可唤起**：该 `Intent` 经 `startActivity` 能实际唤起目标组件。（2026-07-17 技术方案阶段修正：原定义"`PackageManager` 能解析到组件"在 targetSdk 35 包可见性过滤下不可靠——已安装的 App 也可能解析不到；能否唤起的**唯一权威判定是 `startActivity` + 捕获 `ActivityNotFoundException`**，`PackageManager` 解析仅用于 best-effort 展示目标 App 名。详见 `../../design.md`「包可见性」节。）
- **记住选择**：用户在确认框勾选后，对**同一放行键**的后续跳转不再弹框、直接放行的持久化偏好。放行键粒度（技术方案阶段定稿）：目标包名优先（`intent://` 常带 package），无包名时按解析后数据 URI 的 scheme 兜底；只记"允许"，不记"拒绝"。

## 验收条件（可验证句式，EARS/SHALL）

> 每条 Scenario 挂 `TBD(<测试路径>)` 占位，测试实现由技术方案/开发阶段补齐。

### AC-1 检测并拦截 App 跳转链接
- **WHEN** LB 自有 WebView 承载的页面发起一次页面内导航，**AND** 该导航目标是 App 跳转链接（非 http(s) scheme 或 `intent://`），**THE** WebView 层 **SHALL** 在 `shouldOverrideUrlLoading` 中拦截该导航（返回"已接管"），**AND SHALL NOT** 把该链接继续当普通 URL 交给 WebView 加载。
- 反例（现状缺陷，必须消除）：非 http(s) 链接被 WebView 直接加载导致"打不开也不跳转"。
- 测试：`android-app/lynket/src/test/java/arun/com/chromer/browsing/webview/ExternalAppLinkResolverTest.kt`（分类拦截）、`android-app/lynket/src/test/java/arun/com/chromer/browsing/webview/ExternalAppInterceptingWebViewClientTest.kt`（两个 overload 行为一致，含 parse 失败仍拦截）

### AC-2 http(s) 链接行为不变
- **WHEN** 导航目标是 `http`/`https` 链接，**THE** 系统 **SHALL** 保持现状行为（正常在 WebView 内加载），**SHALL NOT** 弹确认框。
- 测试：`android-app/lynket/src/test/java/arun/com/chromer/browsing/webview/ExternalAppLinkResolverTest.kt`（`http, https and webview internal schemes pass through`）、`android-app/lynket/src/test/java/arun/com/chromer/browsing/webview/ExternalAppLaunchHandlerTest.kt`（`http url is not intercepted`）

### AC-3 默认弹确认框
- **WHEN** 拦截到一个解析成功（`Intent.parseUri` 未失败）的 App 跳转链接，**AND** 该放行键**未**被"记住选择"放行，**THE** 系统 **SHALL** 弹出确认框，**AND** 确认框 **SHALL** 尽可能展示将要跳转的目标（best-effort 取 App 名，取不到时展示 scheme / 包名，措辞不承诺精确目标），**AND SHALL** 提供「确认跳转」「取消」两个动作，**AND SHALL** 提供「记住选择 / 不再询问」勾选项。
- 说明（2026-07-17 技术方案阶段修正）：确认框**不以** `PackageManager` 预解析结果做门控——解析不到组件的链接同样弹框（包可见性下解析结果不可靠），"未安装"由 AC-7 的启动路径兜住。
- 测试：`android-app/lynket/src/test/java/arun/com/chromer/browsing/webview/ExternalAppLaunchHandlerTest.kt`（`unremembered external link shows dialog...`；解析不到组件同样弹框由 ExternalAppLinkResolverTest `unresolvable link still resolves as external...` 锁定）；记住选择勾选项 UI 见 `android-app/lynket/src/main/res/layout/dialog_external_app_launch.xml`（模拟器实测见下方实测项）

### AC-4 确认后跳转
- **WHEN** 用户在确认框点「确认跳转」，**THE** 系统 **SHALL** 以 `startActivity` 唤起目标 App 对应的 `Intent`。
- 测试：`android-app/lynket/src/test/java/arun/com/chromer/browsing/webview/ExternalAppLaunchHandlerTest.kt`（`confirming with remember persists choice and launches`）+ 模拟器实测（见下方实测项）

### AC-5 取消不跳转
- **WHEN** 用户点「取消」，**THE** 系统 **SHALL NOT** 跳转，**AND SHALL** 让用户停留在当前页面，不影响后续浏览。
- 测试：`android-app/lynket/src/test/java/arun/com/chromer/browsing/webview/ExternalAppLaunchHandlerTest.kt`（`unremembered external link shows dialog and does not launch`——未确认即不启动）

### AC-6 记住选择后免打扰
- **WHEN** 用户在确认框勾选「记住选择」并确认，**THEN** 对**同一放行键**的后续 App 跳转，**THE** 系统 **SHALL** 直接放行跳转、**SHALL NOT** 再弹确认框。
- 测试：`android-app/lynket/src/test/java/arun/com/chromer/browsing/webview/ExternalAppLaunchHandlerTest.kt`（`remembered allow key launches directly without dialog`）、`android-app/lynket/src/test/java/arun/com/chromer/settings/PreferencesExternalAppLaunchTest.kt`（存储含 copy-on-write 回归护栏）

### AC-7 目标 App 未安装：fail loud 不崩溃（正确性红线）
- **WHEN** 用户确认跳转但目标 App **未安装**（`startActivity` 抛 `ActivityNotFoundException` 或 `PackageManager` 解析不到组件），**THE** 系统 **MUST** 捕获异常、给出明确的"未安装 / 无法打开"提示，**AND MUST NOT** 崩溃，**AND MUST NOT** 静默吞掉（无任何反馈）。
- 说明：本期**不要求**实现显式 Market 兜底（用户取舍），但**未安装绝不能崩溃或静默**——此条为正确性底线，不因概率低降级。
- 测试：`android-app/lynket/src/test/java/arun/com/chromer/browsing/webview/ExternalAppLaunchHandlerTest.kt`（`ActivityNotFoundException fails loud...` 与 `SecurityException fails loud...`，异常路径必测）

### AC-8 CCT 承载不被破坏（回归）
- **WHEN** 用户以系统 Custom Tabs 模式打开网页，**THE** 本次改动 **SHALL NOT** 改变或破坏 CCT 模式既有的跳转行为（CCT 内跳转仍由 provider 处理）。
- 测试：代码级保证：`browsing/customtabs/` 零改动（diff 可查），改动收敛于 `browsing/webview/`；全量单测回归通过（44 tests, 0 failed）

## 需实测确认的项（模拟器 / 真机）

> 用户已同意"未安装兜底后续在模拟器验证"。以下项在开发/验收阶段实测并留证：

1. **未安装时系统默认行为**：目标 App 未装时，`intent://`（带 `package` / 带 `browser_fallback_url`）与纯自定义 scheme 各自的实际表现——系统是否会自动跳 Market / 打开 fallback 网页，还是直接 `ActivityNotFoundException`。据实测决定是否需要补显式兜底（`browser_fallback_url` / `market://` 构造）。
   - `TBD(模拟器实测记录：各 scheme 未安装表现 + 截图/日志)`
2. **承载面覆盖**：主浏览 WebView 之外，气泡 Web Heads / 文章模式等 LB 自有 WebView 承载面是否都命中同一拦截逻辑。
   - `TBD(逐承载面手测：确认拦截+确认框一致生效)`

## 明确的边界语义

- **可以失败，但要 fail loud**：未安装、解析失败等异常路径必须给用户明确反馈，**禁止**静默返回/静默吞异常，**禁止**崩溃。
- **不越权拦 http(s)**：只接管非 http(s) / `intent://` 的 App 跳转；普通网页导航一律不拦，避免破坏正常浏览。
- **CCT 边界诚实**：本 capability 不声称能在 CCT 内提供 LB 确认框；CCT 的自动跳转由 provider（Chrome）负责，非本次交付。
- **正确性红线**：AC-7 的"未安装不崩溃/不静默"为阻断项，验收必测异常路径，happy-path 全绿不等于通过。

## 关联未知与后续

- 若模拟器实测表明未安装不会自动跳 Market → 另立小迭代补显式兜底。
- 若用户后续要求"所有承载面（含 CCT）统一确认框体验" → 属独立需求，需产品取舍（如导流到 WebView 模式），另行评估。
- ~~"记住选择"的放行粒度（按包名 / scheme / 站点）与清除入口，技术方案阶段定稿。~~ 已定稿（2026-07-17）：包名优先、scheme 兜底、只记允许；清除入口本期仅 `Preferences` API，设置页 UI 留后续。见 `../../design.md`。
