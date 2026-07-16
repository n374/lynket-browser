# RAS-55 · 原生气泡用外部浏览器打开（复用登录态）—— Feature

- **Owner**：开发官
- **状态**：已实现（待 CI 编译验证 + 真机 UX 回归）
- **分支**：`feature/55-native-bubble-cct`（基于 spike 分支，含已验证的薄壳）
- **上游**：`proposal.md` · `design.md` · `SPIKE-RESULT.md`（spike 已真机证明路径 2 可行）
- **最后更新**：2026-07-16

## 一句话

把 spike 验证可行的「原生气泡展开时用所选外部浏览器的 Custom Tab 承载页面、复用其登录态」正式化为**用户可开关的功能**：默认关（零回归，气泡仍走内置 WebView），开启后原生气泡改由外部浏览器 CCT 渲染。

## 用户可见行为

- 设置位置：**浏览模式（Browsing Mode）页**新增「原生气泡」卡片，含开关「用外部浏览器打开气泡 / Open bubbles in external browser」。
- 仅 Android Q+ 显示该卡片（原生气泡的系统前提）；开关**仅在选中「原生气泡」浏览模式时可用**，其余模式置灰。
- 开启后：点开原生气泡 → 页面由所选后端浏览器（`Preferences.customTabPackage`）的 Custom Tab 在气泡浮窗内渲染，复用该浏览器的 cookie/登录态。
- 关闭（默认）：维持现状——气泡用 App 内置 WebView。

## 实现要点

| 关注点 | 做法 | 文件 |
|---|---|---|
| 偏好 | 新增 `bubble_external_browser_preference`（默认 false）；`BubbleNotificationManager` 读它决定气泡目标 | `RxPreferences.kt`、`BubbleNotificationManager.kt` |
| 目标切换 | `BubbleLoadData.useCctShell: Boolean? = null`（null=跟随偏好，非 null=显式覆盖）；薄壳↔内置 WebView 由同一 viewIntent 构造点切换，shortcut/pendingIntent 天然同步 | `NativeFloatingBubble.kt`、`BubbleNotificationManager.kt` |
| 薄壳 | `BubbleCctShellActivity`：activity context 且不带 `NEW_TASK` 启动 CCT；**ghost-tab 自杀**（`onAttachedToWindow`+`onResume`，同 `CustomTabActivity`）在 CCT 关闭后收掉空壳；无 provider/异常时**优雅回退**到可嵌入内置 WebView（保浮窗形态） | `BubbleCctShellActivity.kt` |
| 设置 UI | `NativeBubblePreferenceFragment` + `native_bubble_options.xml` + 浏览模式页卡片；开关随浏览模式联动置灰 | `NativeBubblePreferenceFragment.kt`、`BrowsingModeActivity.java`、`res/xml/native_bubble_options.xml`、`res/layout/activity_browsing_mode_content.xml` |
| 文案 | 三语（en / zh-rCN / zh-rTW） | `res/values*/strings.xml` |

## 相对 spike 的清理

- 删除 spike-only 编译期开关 `BubbleSpikeConfig.kt` → 换成真实偏好。
- 删除 spike-only 取证入口 `BubbleSpikeTriggerActivity.kt` 及其 manifest 声明、`ActivityComponent` inject。
- 薄壳由 spike 的「fail-loud、禁 fallback、移除全部 finish」改为产品语义「优雅回退 + ghost-tab 自杀」。

## 已知限制与后续

- ⚠️ **依赖平台未承诺行为**：跨 UID CCT 渲进气泡浮窗未被官方保证；Chrome/系统更新可能使其退化为全屏（退化后 ≈ Web Heads，页面仍正常，属可接受降级）。开启开关的文案已提示「部分设备上可能退化为全屏」。
- **待做**：真机 UX 回归（尤其 ghost-tab 自杀在气泡「折叠/再展开」交互下的手感）、多后端浏览器与更多 Android/Chrome 版本矩阵、塌全屏的显式检测与提示。

## 测试验证

- 编译/出包：CI `build-apk.yml`（`:lynket:assembleDebug`）验证。
- spike 阶段已在 Android 14×Chrome113 / 16×Chrome134 真机证明渲染链路与登录态共享（见 `SPIKE-RESULT.md`）；本 feature 主要变更为开关化 + ghost-tab/回退产品化。

## 变更历史

- 2026-07-16 开发官：spike → feature 正式化（偏好开关 + 设置 UI + 薄壳产品化 + 清理 spike 件）。由 Fable 5 子 Agent 实现主体，主 Agent 收尾（删除取证件、补文档）并验证。
