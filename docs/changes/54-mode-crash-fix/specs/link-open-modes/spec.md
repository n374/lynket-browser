<!-- doc-init template version: v1.0 -->
# Capability Delta: link-open-modes

- **Change**: 54-mode-crash-fix
- **Owner**: 需求官 on behalf of n374 (wu.nerd@gmail.com)
- **基于 living spec 版本**: 无（该 capability 首次引入，`docs/specs/link-open-modes/spec.md` 尚未存在，将于本 change 归档时由本 delta 合并生成）

> Delta 按 Requirement 粒度区分动作。本 change 仅含 ADDED。
> 设计阶段允许「覆盖测试」写 `TBD(<描述>)`，归档前必须替换为真实路径。
> 验证环境：**Android 12+（API ≥ 31）模拟器**（用户明确指定「模拟器」而非真机）。原生气泡回归项在 **Android 10+** 环境验证。

## ADDED Requirements

### Requirement: Slide Over 模式打开链接不崩溃
WHEN 用户在 `targetSdk ≥ 31`（Android 12+）的设备/模拟器上通过 **Slide Over（Custom Tabs）** 模式打开一个链接，THE SYSTEM SHALL 正常构建并展示 Custom Tab，而不因 `PendingIntent` 缺少可变性标志（`FLAG_IMMUTABLE`/`FLAG_MUTABLE`）抛出 `IllegalArgumentException` 崩溃。

#### Scenario: Slide Over 在 Android 12+ 打开链接
- **GIVEN** 运行环境为 Android 12+（API ≥ 31）模拟器，默认打开模式设为 Slide Over
- **WHEN** 用户触发打开任意一个链接
- **THEN** Custom Tab 正常打开，App 不崩溃，logcat 无 `PendingIntent` 相关 `IllegalArgumentException`

**覆盖测试**: TBD(模拟器端到端: Android 12+ 触发 Slide Over 打开链接，断言无崩溃且 Custom Tab 出现；UT: 断言 CustomTabs 链路创建的 PendingIntent 均带 FLAG_IMMUTABLE/FLAG_MUTABLE)

### Requirement: Web Heads 模式打开链接不崩溃
WHEN 用户在 `targetSdk ≥ 31`（Android 12+）的设备/模拟器上通过 **Web Heads（悬浮气泡）** 模式打开一个链接，THE SYSTEM SHALL 正常启动 `WebHeadService` 前台服务并显示悬浮气泡，而不因通知内 `PendingIntent` 缺少可变性标志抛出 `IllegalArgumentException` 崩溃。

#### Scenario: Web Heads 在 Android 12+ 打开链接
- **GIVEN** 运行环境为 Android 12+（API ≥ 31）模拟器，已授予悬浮窗权限，默认打开模式设为 Web Heads
- **WHEN** 用户触发打开任意一个链接
- **THEN** 悬浮气泡（Web Head）正常出现，前台服务正常启动，App 不崩溃，logcat 无 `PendingIntent` 相关 `IllegalArgumentException`

**覆盖测试**: TBD(模拟器端到端: Android 12+ 触发 Web Heads 打开链接，断言无崩溃且悬浮气泡出现；UT: 断言 WebHeadService 通知创建的 PendingIntent 均带 FLAG_IMMUTABLE/FLAG_MUTABLE)

### Requirement: PendingIntent 可变性标志全链路补齐
WHERE 本 change 修复崩溃，THE SYSTEM SHALL 保证 Slide Over 与 Web Heads 两条打开链路上**所有** `PendingIntent` 创建点都显式指定 `FLAG_IMMUTABLE` 或（确需系统改写时）`FLAG_MUTABLE`，不遗留任何仅带 `FLAG_UPDATE_CURRENT` 而无可变性标志的创建点。

#### Scenario: 崩溃链路无残留缺标志的 PendingIntent
- **GIVEN** 本 change 修复改动已应用
- **WHEN** 审计 `CustomTabs.java`、`BottomBarManager.java`、`WebHeadService.java` 等两条链路涉及文件的全部 `PendingIntent` 创建点
- **THEN** 每一处都显式带 `FLAG_IMMUTABLE` 或 `FLAG_MUTABLE`；对每个改为 `FLAG_MUTABLE` 的点，都有明确的「需系统改写」理由

**覆盖测试**: TBD(静态检查/UT: grep 或 Lint 断言目标文件内无「缺可变性标志」的 PendingIntent 创建；对关键点补 UT 断言 flag 取值)

### Requirement: Native Bubbles 模式无回归
WHILE 本 change 的崩溃修复已合入，THE SYSTEM SHALL 保持 **Native Bubbles（原生气泡）** 模式在 Android 10+ 上打开链接的既有可用行为不被破坏。

#### Scenario: 修复后原生气泡仍可用
- **GIVEN** 本 change 修复改动已应用，运行环境为 Android 10+（原生气泡支持环境）
- **WHEN** 用户通过 Native Bubbles 模式打开链接
- **THEN** 原生气泡按既有预期正常弹出并展示内容，无新增崩溃或行为退化

**覆盖测试**: TBD(回归验证: Android 10+ 触发 Native Bubbles 打开链接冒烟；相关模块既有 UT 全绿)

## 3. 非功能需求（NFR）

### NFR-1: 模拟器验证（交付门槛）
- **类别**: 可靠性 / 交付质量
- **目标指标**: 在 **Android 12+ 模拟器**上，Slide Over 与 Web Heads 两模式打开链接的崩溃率降为 0（修复前可稳定复现崩溃，修复后稳定不崩）
- **测量方式**: 模拟器手动复现 + 验证：修复前触发两模式可稳定崩溃，修复后两模式均正常打开链接
- **验收测试或监控项**: TBD(模拟器验证记录: 修复前/后对比截图或录屏 + logcat 崩溃栈对照 + 验证环境说明)
- **不达标后果**: 阻断交付（未在模拟器验证两模式均不崩溃前不得合并）

### NFR-2: 单元测试覆盖（交付门槛）
- **类别**: 可维护性 / 回归防护
- **目标指标**: 崩溃根因修复点具备 UT，断言修复后 `PendingIntent` 带正确可变性标志
- **测量方式**: 运行项目 UT 套件，存在针对修复点的断言并通过
- **验收测试或监控项**: TBD(单元测试: Robolectric/JUnit 断言 CustomTabs / WebHeadService 链路 PendingIntent 的 flag)
- **不达标后果**: 阻断交付（S4 未满足不得合并）

## 关联

- 关联 proposal: [../../proposal.md](../../proposal.md)
- 关联 design: ../../design.md（待产出）
- 关联 ADR: 暂无（如需记录「PendingIntent 可变性策略」可由技术方案阶段补 ADR）
