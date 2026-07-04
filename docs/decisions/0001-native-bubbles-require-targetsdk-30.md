<!-- doc-init template version: v1.0 -->
# ADR-0001: 原生气泡（Native Bubbles）依赖 targetSdk ≤ 30，高 targetSdk 无自动原生气泡方案

- **状态**: Accepted
- **日期**: 2026-07-04
- **Owner（决策者）**: n374
- **Reviewers**: 开发官（调研 + 真机/模拟器实测）
- **关联 change**: [`12-jandan-comment-load-fail`](../changes/12-jandan-comment-load-fail/proposal.md)
- **影响 capability**: 原生气泡浏览（native-bubbles，暂无独立 living spec）

## 1. 上下文

Lynket 的「原生气泡」= Android 11+（API 30）的**系统会话气泡**（Conversation Bubbles），用于从其他 App 点开链接时把网页浮成一个系统气泡。

现代化重写（上游 PR #176，已并入本 fork master）把 `:lynket` 的 `targetSdk` 从 **29 提升到 35**。此后**原生气泡不再自动弹出**——用户反馈「打开看不到气泡」。

排查（含真机 Pixel 8 Pro / Android 17 与稳定 A16/API36 模拟器实测）确认：这不是代码 bug，而是 **Android 平台在 targetSdk 30+ 收紧了原生气泡的自动展示规则**。气泡代码本身（长效会话快捷方式 + `Person` + `MessagingStyle` + `FLAG_IMMUTABLE` + 可冒泡渠道）是正确的，在 targetSdk ≤ 30 下工作正常。

## 2. 决策

**把 `:lynket` 的 `targetSdk` 固定在 `29`**，以保证原生气泡在「从外部 App 打开链接」的核心场景下**全自动弹出、无需用户任何操作**。

**高 targetSdk（31+）下不提供自动原生气泡**——经充分调研与实测，对 Lynket 的使用场景**没有可行的自动方案**。

## 3. 理由

### Android 官方规则（targetSdk 30+）

一条通知只有满足下面**之一**才会自动变成气泡（[官方文档](https://developer.android.com/develop/ui/views/notifications/bubbles)、[会话文档](https://developer.android.com/develop/ui/views/notifications/conversations)）：

1. **发通知那一刻 app 在前台**；或
2. 该会话被标记为**重要会话**（用户长按设为优先）；或
3. 用户在通知栏**手动点过「以气泡显示」**。

Lynket 的核心场景是「从别的 App（Telegram 等）点链接」——那一刻前台是别的 App、不是 Lynket，条件 1 天然不满足；条件 2/3 都要用户手动操作一次。而 targetSdk 29 走的是**旧规则**（有 `MessagingStyle`+`Person` 即弹，不看前台/用户），ts30+ 把这条收掉了。[Android 15 行为变更清单](https://developer.android.com/about/versions/15/behavior-changes-15)**无针对气泡的新增限制** → 这是 ts30+ 通用规则，非 ts35 特有。

### 实测证据（A16 / API 36 稳定模拟器，同一二进制只改 targetSdk）

| 尝试（targetSdk 35） | 结果 |
|---|---|
| 默认触发 | ❌ 不弹 |
| 前台触发（已确认 app 为 top-activity） | ❌ 不弹 |
| app 级气泡 = ALL（`cmd notification set_bubbles ALL`） | ❌ 不弹 |
| 渠道级允许气泡（`set_bubbles_channel ... true`，`mAllowBubbles` 强设为 1） | ❌ 不弹 |
| **同一二进制降到 targetSdk 29** | ✅ 弹出并自动展开（`Stack bubble count: 1`, `isBubble=true`, `mAllowBubble=true`） |

额外关键发现：targetSdk 35 下这条通知**连通知栏都不出现**（`notify()` 成功但活动通知数 = 0），意味着连「让用户手动点一次」这条路也被堵死——通知都不显示，用户无从点起。

## 4. 后果

- **正面**:
  - 原生气泡**全自动、零用户操作、UX 最佳**；
  - 与之相关的 WebView 渲染 / 气泡崩溃等问题在此基线上一并稳定（见关联 change）。
- **负面**:
  - `targetSdk` 停留在 29，**无法上架 Google Play**（Play 现要求 `targetSdk ≥ 34`）；
  - 享受不到高 targetSdk 带来的平台新特性与更严格的安全默认。
- **中立**:
  - 对 **fork / 自用 / 侧载分发** 场景无实际影响（本仓库正是此定位）。

## 5. 备选方案

| 方案 | 优点 | 缺点 | 为什么不选 |
|---|---|---|---|
| **A. 固定 targetSdk 29**（选中） | 原生气泡全自动、无需用户操作 | 不能上架 Play、无高 SDK 特性 | 最契合 fork/自用定位，UX 最好 |
| B. 升 targetSdk + 会话重构 + 用户一次性授权 | 理论上是官方 intended 路径 | 需 `setConversationId`/`LocusId` 重构；每个会话要用户点一次「以气泡显示」；浏览器多气泡 UX 不匹配；**实测当前通知在 ts35 直接不显示，重构后能否成未证明** | 代价大、需用户操作、结果不确定；仅作为「未来必须升 targetSdk」时的探索项 |
| C. 升 targetSdk + Web Heads 悬浮气泡 | `TYPE_APPLICATION_OVERLAY` 自绘，**不受 targetSdk 限制、100% 能弹**，Lynket 现成有 | 不是系统原生气泡；需「显示在其他应用上层」权限 | 作为高 targetSdk 下的兜底，非用户想要的「原生」气泡 |

## 6. 实施

- 落地 change: [`12-jandan-comment-load-fail`](../changes/12-jandan-comment-load-fail/)，commit `0f78f0b4` 将 `:lynket` 的 `targetSdk` 由 35 改回 29（`android-app/lynket/build.gradle.kts`）。
- 代码坐标（供追溯）:
  - `android-app/lynket/build.gradle.kts` → `targetSdk = 29`（**不得随意上调**，上调会破坏原生气泡）；
  - `android-app/lynket/src/main/java/arun/com/chromer/bubbles/system/BubbleNotificationManager.kt` → 气泡通知构建。
- 验收标准: 在 API 30+ 设备/模拟器、原生气泡模式下，从外部打开链接能自动浮出气泡（`dumpsys activity service com.android.systemui` 中 `Stack bubble count ≥ 1`、通知 `isBubble=true`）。

## 7. 修订历史

| 日期 | 状态变更 | 摘要 |
|---|---|---|
| 2026-07-04 | → Accepted | 初次记录：因 Android targetSdk 30+ 限制，固定 targetSdk=29 保证原生气泡；高 targetSdk 无自动方案，备选 Web Heads / 会话重构。 |
