# Spec · 原生气泡承载外部浏览器（路径 2 可行性）

- **Capability**：`native-bubble-external-browser`
- **Owner**：需求官 → 技术方案官
- **状态**：待验证（spike）
- **最后更新**：2026-07-16
- **上游需求**：`docs/changes/55-native-bubble-cct/proposal.md`

## 范围

仅验证「原生气泡展开时用所选外部浏览器的 Custom Tab 渲染，且保留浮窗形态、共享该浏览器会话」这一条路径的**技术可行性**。不含正式功能化。

## 验收前置条件（不满足则实验作废）

- 目标后端浏览器：固定**包名 + 版本号**并记录；用**普通 profile、非隐身、未启用 ephemeral Custom Tab**。
- 每次实验前**清空探针 origin 的站点数据**，从基线开始。
- 独立浏览器直开与气泡打开必须命中**同一 URL / 同一 scheme / 同一 host / 同一 port**（同源，否则 cookie/localStorage 不可比对）。
- 托管：模拟器上 `python3 -m http.server`，emulator 内 `http://10.0.2.2:<port>/login-state-probe.html`。

## 验收条件（可验证句式）

对**每个「Android 版本 × 浏览器版本」组合**分别判定；某组合判为「可行」当且仅当以下三条**同时成立**、各留独立证据：

1. **保留浮窗形态**：从原生气泡展开后，页面以**气泡浮窗**呈现，未塌全屏、未崩溃。
   - 证据：截图 + `dumpsys activity bubbles` 显示 bubble 仍 expanded。
2. **引擎 = 外部浏览器 CCT（决定性证据是 dumpsys，非 UA）**：`dumpsys activity activities`（或 `dumpsys activity top`）显示气泡内 resumed 的顶层 Activity 是**目标浏览器包的 Custom Tab Activity**，且处在**气泡所在 task/window** 内。
   - 证据：dumpsys 输出记录 **package / activity 名 / taskId / windowing 状态 / 与 Lynket 壳 Activity 的栈关系**；探针页 UA「不含 `wv`」仅作**辅助佐证**（UA 可被改写，缺失 `wv` 不能单独证明是 CCT）。
3. **同源存储共享（cookie 与 localStorage 分别判定）**：先用独立浏览器直开探针页得到 cookie 令牌 Xc（须显示"写入复读校验 OK"）与 localStorage 令牌 Xl；再用原生气泡打开**同一 URL**：
   - cookie：气泡内 `probe_ck` **严格等于** Xc 且复读校验 OK ⇒ cookie jar 共享；
   - localStorage：气泡内 `probe_ls` **严格等于** Xl ⇒ localStorage 共享；
   - 两者**分开记录**（cookie 共享而 localStorage 不共享属可能情形，不得互相顶替）。
   - 证据：两处截图 + 两条 `[BUBBLE_PROBE]` 日志的 `cookieToken`/`localStorageToken` 对比。

若三条不同时成立，记录**具体失败态**（塌全屏 / dumpsys 显示仍是 Lynket 内置 WebView Activity / 令牌不等 / cookie 复读失败 / 崩溃）与证据，该组合判「不可行」。

## 结论分级（禁止单点外推）

- 逐「Android 版本 × 浏览器版本」出「可行 / 不可行 / 失败态」矩阵。
- **只要有一格三条全绿** ⇒ 证明"平台未全面封死"，但结论只能写成"**仅该版本×该浏览器组合可行**"，**不得**表述为"路径 2 全面可行"。
- 全矩阵皆不可行 ⇒ 判「路径 2 不可行」，回退按 proposal「风险与回退」交用户选 A/B/C。
- 覆盖 Android 14 / 15 / 16；若设备支持 Android 17，额外记录一组（顺带实测回退项 A「系统级气泡化任意 App」是否可用）。

## 明确的边界语义

- **可以失败，但要 fail loud**：任何一条不达标都必须如实记录为失败，**禁止**把"塌全屏但页面能打开"粉饰为"部分成功"——那不是本 spike 要的浮窗内渲染。
- **弱信号不得当决定性证据**：引擎判定以 **dumpsys 的浏览器 CCT 包名 + 气泡 task 栈**为准，探针页 UA「无 wv」只作辅助；「无 wv」单独出现**不得**判为成功。
- **正确性红线**：存储共享以 **cookie / localStorage 令牌各自严格相等**为准，且 cookie 须复读校验通过；令牌不等或校验失败即判"未共享"，不得因"看起来像登录了"降级放行。cookie 与 localStorage **分别判定、互不顶替**。
- **口径诚实**：本 spike 的存储共享证明的是"同源一方存储在普通导航下连续"，是登录态可复用的**必要且强指示**；"真实登录态全量复用"的最终收口来自 dumpsys 证明"渲染者即目标浏览器 CCT"（CCT 天然带该浏览器完整 profile）。
- 探针页为**验收测试资产**（零登录、纯静态），非产品代码；正式功能化时不随产品发布。

## 关联未知与后续

- 若「可行」→ 另立 feature：把薄壳 Activity 正式化、加设置开关、处理关闭 CCT 后的 ghost-tab 自杀与返回栈、多后端浏览器兼容。
- 若「不可行」→ 关闭路径 2，走回退等价满足（proposal A/B/C）。
