# Spike RAS-38：targetSdk 35 前台「隐身窗口」触发自动浮出气泡

> 状态：已完成（结论：**可行**）
> 分支：`spike/foreground-invisible-bubble`（基于 `build/apk`）
> 关联：RAS-15（原生 Bubble 高 targetSdk 失效的长链调查）、issue #170（Android 11+ 原生气泡修复）

## 概述

验证一条路径的可行性：**从别的 App 点链接 → Lynket 借一个透明前台 Activity 把气泡在前台发出并自动浮出 → 前台切换对用户几乎无感**，观感上就是「在原 App 上浮出一个 bubble」。

一句话结论：**可行**。补齐两道被忽视的门禁 + 把气泡「发通知」这一刻放回前台后，气泡能在高 targetSdk 下自动展开、悬于背景之上，并在承载 Activity 退场、Lynket 退后台后持久悬浮。

## 背景与两道真正的门禁

RAS-15 曾得出「高 targetSdk 前台各形态都失败」。**本 Spike 证实那是假阴性**，根因是两道门，而非平台不可能：

1. **通知权限门（targetSdk ≥ 33）**：全仓从未申请 `POST_NOTIFICATIONS`。ts33+ 下通知在 `checkDisqualifyingFeatures` 被整体 block，`dumpsys notification` 0 条——此前「前台各形态都失败」的实验大概率根本没测到「前台气泡」这一步。
2. **会话门（targetSdk ≥ R/30）**：AOSP `BubbleExtractor.process()` 要求 `record.isConversation()==true`；`NotificationRecord.isConversation()` 对 MessagingStyle 通知要求 shortcut 存在且 `!isOnlyBots(shortcut.persons)`。Lynket 把 shortcut 的 person 与 MessagingStyle 的 person 都设成了 **bot** → 被判非会话 → 气泡被拒。

此外还有一个**时机门**（非权限，而是平台策略）：

3. **前台时机门**：`BubbleMetadata.setAutoExpandBubble(true)` 只在**发通知那一刻发通知的 App 处于前台**时才被平台承认，否则静默降级为「收起的气泡」。Lynket 原流程在后台 pool 线程、且在承载 Activity `finish()` 之后才 post，故 auto-expand 一直被吞。

## 实现（关键变更点）

| 文件 | 变更 | 目的 |
|---|---|---|
| `build-logic/.../constants/Constants.kt` | `ANDROID_TARGET_SDK` 29 → 35 | 让 OS 的会话门 / 权限门生效（**注意：`compileSdk` 仍为 31**，见下方约束） |
| `lynket/.../AndroidManifest.xml` | 声明 `POST_NOTIFICATIONS`；给带 intent-filter 的 4 个 QS Tile 服务、ShareInterceptActivity、BrowserIntercept 别名补 `android:exported` | 过权限门；targetSdk≥31 强制显式 exported，否则 manifest merge 直接失败 |
| `lynket/.../home/HomeActivity.kt` | 首启运行时申请 `POST_NOTIFICATIONS` | ts33+ 硬门槛（权限串用字面量以兼容 compileSdk 31） |
| `lynket/.../bubbles/system/BubbleNotificationManager.kt` | shortcut person 与 MessagingStyle person `setBot(true)→(false)`；`setAutoExpandBubble(false)→(true)` | 过会话门 + 打开自动展开 |
| `lynket/.../browserintercept/BrowserInterceptActivity.kt` | native bubble 走透明前台宿主：在 `onCreate` **前台同步 post 气泡**，再 `postDelayed(finish, 800ms)` | 满足前台时机门，使 auto-expand 生效；原异步流程发通知时 App 已退后台 |

### 关键约束：compileSdk 仍为 31

本仓 AGP=7.1.2 / JDK11 / Gradle7.4.2，依赖是从源码 `publishToMavenLocal` 复活的 `base-android`。把 compileSdk 提到 35 需 AGP 8.6+/JDK17/Gradle8.7+ 的整链迁移，属独立工程、且极可能压垮这套脆弱构建。

**这不影响结论**：OS 的会话门 / `POST_NOTIFICATIONS` 门禁读的是 **manifest 里的 `targetSdk`（=35，aapt 实测确认）**，不是 compileSdk。用不到 API 32–35 的新 API（`POST_NOTIFICATIONS` 以字符串字面量 + `Build.VERSION.SDK_INT>=33` 守卫）。若需「编译期也对齐 SDK 35」，应另立迁移任务。

## 测试验证

证据来源：`dumpsys notification` / `dumpsys activity activities` / `logcat`（Bubbles）/ 截图。两处环境结论一致。

| 阶段 | 结论 | 关键证据 |
|---|---|---|
| **T0 能发气泡** | 通过 | 通知记录 `flags=LOCAL_ONLY\|BUBBLE`、`isBubble=true`、`mAllowBubble=true`、`shortcut=<url>`；SystemUI `Bubbles: Bubble.inflate()→doAdd→setSelectedBubbleInternal`。→ 会话门+权限门修复有效，RAS-15「前台假阴性」被独立证实 |
| **T1 持久性**（本 Spike 要钉死的关键未知） | 通过 | 气泡浮出后承载 Activity 已 finish、`topResumedActivity=Launcher`（Lynket 无 Activity resumed），气泡仍以 `FLAG_BUBBLE` 悬浮于 Launcher 之上，Lynket 进程存活以承载展开内容。→ Plan A（发完即 finish）成立，无需 Plan B 常驻 1px 窗口 |
| **自动展开** | 通过 | 改前 `autoExpand=false`（收起）；前台同步 post 后 `autoExpand=true`、`expansionChanged=true expanded=true`，截图为自动展开的浏览气泡（标题/URL/工具栏），全程未点击 |
| **T2 端到端** | 通过（含一处边界，见下） | 从源 App 触发链接 → 透明宿主对用户不可见（背景透出，无黑屏闪烁）→ 气泡自动展开悬于背景之上 |

### 验证环境
- 模拟器：Android 16 / API 36（app targetSdk 35）。
- 真机：Pixel 8 Pro / **Android 17 / API 37**（app targetSdk 35）——比 15 更严，`autoExpand=true`、`expanded=true` 同样成立，端到端可用。

## 已知风险 / 诚实边界

- **T2 的「零闪烁悬于原 App」只到「≤ 一次轻微过渡」**：实测经 `adb am start` 从 shell 拉起拦截器，落到独立 task，finish 后回退到 Launcher 而非源 App，出现约 1 秒级 Launcher 闪现。真实「App 内点链接」会保留调用方 task、大概率回到源 App，但**此点未在真实 App-内点击链路上闭环验证**，不能声称零打扰。
- 慢速模拟器负载高时偶发「展开后又收起」抖动，真机稳定展开；仍建议真机多场景复验。
- 自动展开路径仅覆盖 **BrowserInterceptActivity（外部链接拦截）**；App 内搜索栏开链接走的是另一条异步路径，未加前台同步 post，auto-expand 不保证。
- 首个气泡会触发系统「气泡教学」浮层一次，之后不再出现。

## 后续正式化建议

1. 决策：是否投入 AGP8/JDK17 整链迁移把 compileSdk 也提到 35（否则长期停在 compileSdk 31 + targetSdk 35 的组合）。
2. 若正式采用：把 App 内开链接路径也统一走前台同步 post，或抽出「前台气泡投递器」；补 `POST_NOTIFICATIONS` 被拒后的降级引导。
3. `finish` 延迟（当前 800ms）宜做成随「气泡已 inflate」的信号回调，而非固定延时。

## 相关提交

- `3a4fa6e9` 🧪 Spike: targetSdk35 前台隐身窗口触发自动浮出气泡（本报告对应的实现）

## 变更历史

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-07-05 | 初版：记录 Spike RAS-38 的门禁分析、实现与 T0/T1/T2 实测结论 | 开发官（Claude） |
