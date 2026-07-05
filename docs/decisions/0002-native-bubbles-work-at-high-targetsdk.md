# ADR-0002：原生气泡在高 targetSdk（35）下可用，无需固定 targetSdk ≤ 30

> 状态：**Accepted**（2026-07-05）
> 取代：**ADR-0001**（master 分支的「原生气泡依赖 targetSdk ≤ 30」结论，已被实测推翻）
> 依据：Spike RAS-38（见 `docs/spikes/RAS-38-foreground-invisible-bubble.md`）

## 背景

master 分支的 ADR-0001 结论为「高 targetSdk（31+）下不提供自动原生气泡，对 Lynket 场景无可行自动方案」，并据此把 targetSdk 固定在 29（靠 targetSdk ≤ 29 豁免 Android 的会话门）。

Spike RAS-38 在模拟器（API 36）与真机（Pixel 8 Pro / Android 17）上实测，**推翻了该结论**。

## 决策

**原生气泡在 targetSdk 35 下可正常显示与使用**，条件是补齐三道被忽视的门禁：

1. **会话门（≥ API 30）**：shortcut 与 MessagingStyle 的 `Person` 必须 **非 bot**（`setBot(false)`）。AOSP `NotificationRecord.isConversation()` 对 MessagingStyle 要求 shortcut person 非 `isOnlyBots()`，否则判为非会话、气泡被拒。
   - 真机 A/B 实测（同构建只翻 person）：`setBot(true)` → 通知 `flags=LOCAL_ONLY`（无 BUBBLE、不 inflate）；`setBot(false)` → `flags=LOCAL_ONLY|BUBBLE`、`isBubble=true`。
2. **通知权限门（≥ API 33）**：声明并运行时申请 `POST_NOTIFICATIONS`，否则 `notify()` 被静默丢弃。
3. **shortcut 会话绑定（≥ API 30）**：发布 long-lived sharing shortcut 并 `setShortcutId`，否则气泡被降级为普通通知。

配套：targetSdk ≥ 30 需给带 intent-filter 的组件补 `android:exported`；包可见性用 `<queries>`（或 `QUERY_ALL_PACKAGES`）恢复浏览器/CT 提供方枚举。

气泡默认收起、点击展开（`setAutoExpandBubble(false)`，按用户 UX 决策）；自动展开亦被验证可行（需前台同步 post），留档备用。

## 影响

- 本分支（spike，作为最终主线）采用 targetSdk 35 + 上述修复，气泡真机可用。
- ADR-0001 的「≤30 才行」不再成立；其「固定 targetSdk=29」的取舍仅适用于不修会话门的老实现。

## 待办 / 中期方案 TODO（现代化线，另行立项）

> 记录用户认可的中期方向，避免遗失。**不在当次合并里做**，需独立立项 + 真机闭环。

- [ ] **迁移到现代工具链**：以 master 的现代化地基（Kotlin DSL 构建 / JDK 17 / compileSdk 35 / 去 base-android / Hilt·Room·DataStore）为基座，替换 spike 现有的 legacy 构建（Groovy / JDK 11 / base-android 源码复活 / compileSdk 31）。动机：compileSdk 35 + 现代 AGP，长期可上架、可维护。
- [ ] **清理 master 的半成品**：master 用 `90e9ed2c` 把未完成的 Compose UI（`chromer/ui/**`、`MainActivity`、`Modern*` VM/Repo、DataStore 预置）从编译排除。迁移时应「完成」或「彻底删除」，不得作为死代码带入主线。
- [ ] **把 spike 的 targetSdk-35 气泡策略叠加到现代地基上**（master compileSdk 已是 35，无 spike 老地基的 AGP 限制），去掉 targetSdk 29 的兜底。
- [ ] **真机闭环回归**：现代地基首次本机出包 + 气泡/浏览全链路真机复验后，方可切主线。

## 变更历史

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-07-05 | 初版：取代 ADR-0001，记录高 targetSdk 气泡可用的门禁结论与中期现代化 TODO | 开发官（Claude）|
