# RAS-55 · 路径 2 真机 Spike 执行结果

- **Owner**：开发官（实现 + 真机取证）
- **状态**：**结论 = 可行**（两组合独立复现，均三条全绿）
- **执行日期**：2026-07-16
- **执行方式**：本机 Android 模拟器（google_apis arm64），adb 脚本化触发 + `adb input tap` 模拟点击展开气泡
- **上游**：`design.md` · `specs/native-bubble-external-browser/spec.md`

## 一句话结论

原生气泡展开时，用 activity context 且不带 `FLAG_ACTIVITY_NEW_TASK` 启动「所选外部浏览器（Chrome）的 Custom Tab」，**系统 UI 确实把这个跨 UID 的 CCT 渲进了气泡浮窗**——保留浮窗形态（多窗口/非全屏）、由 Chrome 自身进程渲染、并**共享 Chrome 的 cookie 与 localStorage**。**路径 2 在实测的两个「Android 版本 × Chrome 版本」组合上均可行。**

## 结论矩阵（两组合，均 targetSdk=29）

| Android | Chrome | 浮窗形态 | 引擎(dumpsys 决定性) | cookie 共享 | localStorage 共享 | UA 无 wv | 判定 |
|---|---|---|---|---|---|---|---|
| 16 (API 36) | 134.0.6998.135 | 🟢 multi-window `Rect(42,369-1038,2127)` 非全屏 | 🟢 `com.android.chrome/…customtabs.CustomTabActivity` uid=10169，气泡 task t65 top/resumed | 🟢 `CK-mrng3ymh-4b0r3exy` == 基线 | 🟢 `LS-mrng3yp1-898b5422` == 基线 | 🟢 | **可行** |
| 14 (API 34) | 113.0.5672.136 | 🟢 multi-window `Rect(42,361-1038,2155)` 非全屏 | 🟢 同上 uid=10146，气泡 task t51 top/resumed | 🟢 `CK-mrnghzrh-xbe35731` == 基线 | 🟢 `LS-mrngi03g-5kh0olcj` == 基线 | 🟢 | **可行** |

> Android 15 未测（该机未装 API 35 系统镜像）。按 spec「禁单点外推」：以上仅证明**这两个组合**可行，且证明**平台并未全面封死**路径 2；未测组合需各自复现。

## 决定性证据（dumpsys，非 UA 弱信号）

两组合中 `dumpsys activity activities` 均显示同一 task 内：
- Hist#0 `arun.com.chromer.debug/…BubbleCctShellActivity`（rootOfTask=true，Lynket 薄壳）
- Hist#top `com.android.chrome/org.chromium.chrome.browser.customtabs.CustomTabActivity`（**Chrome 自身 uid**，topResumedActivity/mFocusedApp）
- task `mode=multi-window`，Chrome CCT `mWindowingMode=multi-window` 且 `mBounds` 为气泡浮窗矩形（非 `0,0-1080,2400` 全屏）

薄壳日志：`[BUBBLE_SPIKE] pkg=com.android.chrome flags=0x0`（**launch 实际 flags=0x0，确认未带 NEW_TASK**）→ `[BUBBLE_SPIKE] launched CCT`（无 fallback、无异常）。

## 存储共享证据（独立信道：托管服务器 access log beacon）

稳定版 Chrome 不把页面 `console.log` 转发到 logcat，故 spec 原定的 `logcat BUBBLE_PROBE` 抓不到令牌。改用探针页新增的 `GET /beacon?...` 让令牌落在 `python3 -m http.server` 的 access log（独立于设备的第三方可查信道）：

| 组合 | 独立浏览器直开(baseline) | 气泡打开(bubble) | 对照组(内置 WebView) |
|---|---|---|---|
| A16 | ck=`CK-mrng3ymh-4b0r3exy` ls=`LS-mrng3yp1-898b5422` wv=0 | **相同** | ck=`CK-mrngdr6v-isedo5sl`(不同) ls=不可用 **wv=1** |
| A14 | ck=`CK-mrnghzrh-xbe35731` ls=`LS-mrngi03g-5kh0olcj` wv=0 | **相同** | — |

**对照组（实验组的 A/B 反证，A16 实测）**：把气泡目标切回内置 `EmbeddableWebViewActivity`，cookie 令牌**不同**、localStorage 不可用、UA **含 wv**——证明取证方法能区分「共享 Chrome 会话」与「App 内置 WebView」，实验组的「相同」不是测量假象。

## 真机执行沉淀（写入 `assets/capture.sh`）

1. 原生气泡须显式放行：`settings put secure notification_bubbles 1` + `cmd notification set_bubbles <pkg> 1` + `cmd notification set_bubbles_channel <pkg> BUBBLE_NOTIFICATION_CHANNEL_ID_v2 true`（channel 首次 showBubbles 才建，故先触发一次再设 channel 再重触发）。否则通知降级为普通通知（`isBubble=false`）。
2. 令牌取证走 http.server beacon，不走 logcat（稳定版 Chrome 不转发 console）。
3. 气泡不自动展开，须 `adb input tap` 命中边缘圆标；首次点击可能只关掉 "Chat using bubbles" 教学气泡。

## 结论与后续

- **路径 2 可行**（实测两组合，dumpsys + 独立 beacon + 对照组三方坐实）。但仍是**未被官方承诺**的行为，依赖系统把跨 UID CCT 渲进气泡 TaskView；Chrome/系统更新可能使其退化为全屏（退化后 ≈ Web Heads，属可接受降级）。
- **建议**：立 feature 把薄壳正式化（设置开关、关闭 CCT 后的 ghost-tab 与返回栈处理、塌全屏优雅回退、多后端浏览器兼容、补 Android 15 及更多 Chrome 版本矩阵）。spike 分支本身不合入产品。

## 诚实校准

- 「气泡内渲染者是 Chrome 的 CCT」= dumpsys 显示 Chrome 自身 uid 的 CustomTabActivity（决定性），非仅凭 UA。
- 「共享登录态」以**零登录探针的 cookie/localStorage 令牌严格相等**为证：证明同源存储在普通导航下与独立 Chrome 连续（登录态可复用的必要且强指示）；真实 `HttpOnly`/服务端 session 全量复用由「渲染者即 Chrome CCT，天然带 Chrome 完整 profile」收口。用户已明确「不一定需要登录」。
