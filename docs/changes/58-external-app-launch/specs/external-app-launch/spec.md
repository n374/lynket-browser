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
- **可唤起**：通过 `PackageManager` 能解析到至少一个可处理该 `Intent` 的组件（对应已安装 App）。
- **记住选择**：用户在确认框勾选后，对**同一放行键**（技术方案阶段定义，建议为"目标包名"或"scheme"）的后续跳转不再弹框、直接放行的持久化偏好。

## 验收条件（可验证句式，EARS/SHALL）

> 每条 Scenario 挂 `TBD(<测试路径>)` 占位，测试实现由技术方案/开发阶段补齐。

### AC-1 检测并拦截 App 跳转链接
- **WHEN** LB 自有 WebView 承载的页面发起一次页面内导航，**AND** 该导航目标是 App 跳转链接（非 http(s) scheme 或 `intent://`），**THE** WebView 层 **SHALL** 在 `shouldOverrideUrlLoading` 中拦截该导航（返回"已接管"），**AND SHALL NOT** 把该链接继续当普通 URL 交给 WebView 加载。
- 反例（现状缺陷，必须消除）：非 http(s) 链接被 WebView 直接加载导致"打不开也不跳转"。
- `TBD(WebView shouldOverrideUrlLoading 单测：intent:// / 自定义 scheme 命中拦截；http(s) 不拦截)`

### AC-2 http(s) 链接行为不变
- **WHEN** 导航目标是 `http`/`https` 链接，**THE** 系统 **SHALL** 保持现状行为（正常在 WebView 内加载），**SHALL NOT** 弹确认框。
- `TBD(WebView shouldOverrideUrlLoading 单测：http(s) 透传不拦截)`

### AC-3 默认弹确认框
- **WHEN** 拦截到一个**可唤起**的 App 跳转链接，**AND** 该放行键**未**被"记住选择"放行，**THE** 系统 **SHALL** 弹出确认框，**AND** 确认框 **SHALL** 表明将要跳转的目标（尽可能展示目标 App 名 / scheme），**AND SHALL** 提供「确认跳转」「取消」两个动作，**AND SHALL** 提供「记住选择 / 不再询问」勾选项。
- `TBD(UI/交互测试：可唤起跳转链接触发确认框且含记住选择项)`

### AC-4 确认后跳转
- **WHEN** 用户在确认框点「确认跳转」，**THE** 系统 **SHALL** 以 `startActivity` 唤起目标 App 对应的 `Intent`。
- `TBD(仪器测试/手测：确认后正确唤起已安装目标 App)`

### AC-5 取消不跳转
- **WHEN** 用户点「取消」，**THE** 系统 **SHALL NOT** 跳转，**AND SHALL** 让用户停留在当前页面，不影响后续浏览。
- `TBD(交互测试：取消后无跳转、页面可继续浏览)`

### AC-6 记住选择后免打扰
- **WHEN** 用户在确认框勾选「记住选择」并确认，**THEN** 对**同一放行键**的后续 App 跳转，**THE** 系统 **SHALL** 直接放行跳转、**SHALL NOT** 再弹确认框。
- `TBD(存储/交互测试：勾选后同类跳转不再弹框直接跳)`

### AC-7 目标 App 未安装：fail loud 不崩溃（正确性红线）
- **WHEN** 用户确认跳转但目标 App **未安装**（`startActivity` 抛 `ActivityNotFoundException` 或 `PackageManager` 解析不到组件），**THE** 系统 **MUST** 捕获异常、给出明确的"未安装 / 无法打开"提示，**AND MUST NOT** 崩溃，**AND MUST NOT** 静默吞掉（无任何反馈）。
- 说明：本期**不要求**实现显式 Market 兜底（用户取舍），但**未安装绝不能崩溃或静默**——此条为正确性底线，不因概率低降级。
- `TBD(单测/仪器测试：注入未安装场景，断言不崩溃且有明确提示)`

### AC-8 CCT 承载不被破坏（回归）
- **WHEN** 用户以系统 Custom Tabs 模式打开网页，**THE** 本次改动 **SHALL NOT** 改变或破坏 CCT 模式既有的跳转行为（CCT 内跳转仍由 provider 处理）。
- `TBD(回归验证：CCT 模式打开含 App 跳转的页面，行为与改动前一致)`

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
- "记住选择"的放行粒度（按包名 / scheme / 站点）与清除入口，技术方案阶段定稿。
