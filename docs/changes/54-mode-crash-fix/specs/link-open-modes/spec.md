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

**覆盖测试**:
- UT: `android-app/lynket/src/test/java/arun/com/chromer/util/PendingIntentFlagGuardTest.kt`（源码守卫，逐点断言 CustomTabs 链路 9 MUTABLE / 1 IMMUTABLE）+ `PendingIntentsTest.kt`（BottomBar 可达点断言非 immutable）。
- 模拟器 E2E（API 34 AVD 实测）：修复前崩 `CustomTabs.prepareCopyLink:466`（logcat 复现）；修复后打开 Chrome Custom Tab 不崩，点底栏"New tab"→ `NewTabDialogActivity dat=https://…`，URL 回填到接收方（证明 MUTABLE 生效）。

### Requirement: Web Heads 模式打开链接不崩溃
WHEN 用户在 `targetSdk ≥ 31`（Android 12+）的设备/模拟器上通过 **Web Heads（悬浮气泡）** 模式打开一个链接，THE SYSTEM SHALL 正常启动 `WebHeadService` 前台服务并显示悬浮气泡，而不因通知内 `PendingIntent` 缺少可变性标志抛出 `IllegalArgumentException` 崩溃。

#### Scenario: Web Heads 在 Android 12+ 打开链接
- **GIVEN** 运行环境为 Android 12+（API ≥ 31）模拟器，已授予悬浮窗权限，默认打开模式设为 Web Heads
- **WHEN** 用户触发打开任意一个链接
- **THEN** 悬浮气泡（Web Head）正常出现，前台服务正常启动，App 不崩溃，logcat 无 `PendingIntent` 相关 `IllegalArgumentException`

**覆盖测试**:
- UT: `PendingIntentFlagGuardTest.kt`（断言 WebHeadService 三处 IMMUTABLE + setPackage）。
- 模拟器 E2E（API 34 AVD 实测）：修复前崩 `WebHeadService.getNotification:206`（logcat 复现）；补 §12 前台服务类型后，`WebHeadService` `isForeground=true types=40000000 startForegroundCount=1`，不再崩 `MissingForegroundServiceTypeException`。
> 注：Web Heads 崩溃含两处根因——PendingIntent（§1–§11）与 FGS 类型（§12），二者均已修复并验证，见下方「Web Heads / App-Detection 前台服务在 API 34 声明 foregroundServiceType」需求。

### Requirement: PendingIntent 可变性标志全链路补齐
WHERE 本 change 修复崩溃，THE SYSTEM SHALL 保证 Slide Over 与 Web Heads 两条打开链路上**所有** `PendingIntent` 创建点都显式指定 `FLAG_IMMUTABLE` 或（确需系统改写时）`FLAG_MUTABLE`，不遗留任何仅带 `FLAG_UPDATE_CURRENT` 而无可变性标志的创建点。

#### Scenario: 崩溃链路无残留缺标志的 PendingIntent
- **GIVEN** 本 change 修复改动已应用
- **WHEN** 审计 `CustomTabs.java`、`BottomBarManager.java`、`WebHeadService.java` 等两条链路涉及文件的全部 `PendingIntent` 创建点
- **THEN** 每一处都显式带 `FLAG_IMMUTABLE` 或 `FLAG_MUTABLE`；对每个改为 `FLAG_MUTABLE` 的点，都有明确的「需系统改写」理由

**覆盖测试**: `android-app/lynket/src/test/java/arun/com/chromer/util/PendingIntentFlagGuardTest.kt`（纯 JVM 源码扫描守卫：断言 `CustomTabs.java`/`BottomBarManager.java`/`WebHeadService.java` 内无裸 `PendingIntent.get*`，且逐点映射 10 MUTABLE / 4 IMMUTABLE、MUTABLE 点带显式组件、WebHead 三点 setPackage）+ `PendingIntentsTest.kt`（助手 SDK 分支断言）。

### Requirement: Native Bubbles 模式无回归
WHILE 本 change 的崩溃修复已合入，THE SYSTEM SHALL 保持 **Native Bubbles（原生气泡）** 模式在 Android 10+ 上打开链接的既有可用行为不被破坏。

#### Scenario: 修复后原生气泡仍可用
- **GIVEN** 本 change 修复改动已应用，运行环境为 Android 10+（原生气泡支持环境）
- **WHEN** 用户通过 Native Bubbles 模式打开链接
- **THEN** 原生气泡按既有预期正常弹出并展示内容，无新增崩溃或行为退化

**覆盖测试**: 模拟器 E2E（API 34 AVD 实测）：Native Bubbles 模式打开链接不崩溃、无回归（走系统气泡通知、不启前台服务，不受本次两处修复影响）；`:lynket:testDebugUnitTest` 全套 24 项 0 失败（含既有 `DefaultTabsManagerTest` 等）。

### Requirement: Web Heads / App-Detection 前台服务在 API 34 声明 foregroundServiceType
WHEN 在 `targetSdk ≥ 34`（Android 14+）的设备/模拟器上启动 `WebHeadService`（Web Heads 悬浮气泡）或 `AppDetectService`（前台应用检测）前台服务，THE SYSTEM SHALL 正常进入前台运行，而不因缺少 `android:foregroundServiceType` 抛出 `MissingForegroundServiceTypeException`。

#### Scenario: 两前台服务在 Android 14 正常促前台
- **GIVEN** 运行环境为 Android 14（API 34）模拟器，manifest 已为两服务声明 `specialUse` 类型并持有 `FOREGROUND_SERVICE_SPECIAL_USE` 权限
- **WHEN** 触发 Web Heads 打开链接（启动 `WebHeadService`）或开启 per-app / app-based toolbar 后启动 `AppDetectService`
- **THEN** 两服务 `startForeground` 成功（`dumpsys` 显示 `isForeground=true types=40000000`），App 不崩溃，logcat 无 `MissingForegroundServiceTypeException`

**覆盖测试**: 模拟器 E2E（API 34 AVD 实测）：`WebHeadService` 与 `AppDetectService` 均 `isForeground=true types=40000000`、`startForegroundCount≥1`，logcat 无 `MissingForegroundServiceTypeException`。纯 UT 无法复现平台 FGS 类型校验（运行时行为，与 §6 同理靠模拟器闭环）。相邻风险 `ForegroundServiceStartNotAllowedException`（后台启动）本次未复现（应用处前台时启动被允许），按 design §12.5b 若后续复现则另开 issue。

## 3. 非功能需求（NFR）

### NFR-1: 模拟器验证（交付门槛）
- **类别**: 可靠性 / 交付质量
- **目标指标**: 在 **Android 12+ 模拟器**上，Slide Over 与 Web Heads 两模式打开链接的崩溃率降为 0（修复前可稳定复现崩溃，修复后稳定不崩）
- **测量方式**: 模拟器手动复现 + 验证：修复前触发两模式可稳定崩溃，修复后两模式均正常打开链接
- **验收测试或监控项**: 已在 **Android 14（API 34）AVD `LyknetTest`** 完成修复前/后 logcat 崩溃栈对照——Slide Over 前崩 `CustomTabs.prepareCopyLink:466`、Web Heads 前崩 `WebHeadService.getNotification:206`（PendingIntent）及补 §12 前的 `MissingForegroundServiceTypeException`；修复后两模式均正常打开、`dumpsys` 佐证前台服务 `types=40000000`。验证记录见开发测试阶段结果评论。
- **不达标后果**: 阻断交付（未在模拟器验证两模式均不崩溃前不得合并）

### NFR-2: 单元测试覆盖（交付门槛）
- **类别**: 可维护性 / 回归防护
- **目标指标**: 崩溃根因修复点具备 UT，断言修复后 `PendingIntent` 带正确可变性标志
- **测量方式**: 运行项目 UT 套件，存在针对修复点的断言并通过
- **验收测试或监控项**: `android-app/lynket/src/test/java/arun/com/chromer/util/PendingIntentFlagGuardTest.kt`（3 用例）+ `PendingIntentsTest.kt`（4 用例，Robolectric）；`:lynket:testDebugUnitTest` 全套 24 项 0 失败。
- **不达标后果**: 阻断交付（S4 未满足不得合并）

## 关联

- 关联 proposal: [../../proposal.md](../../proposal.md)
- 关联 design: ../../design.md（待产出）
- 关联 ADR: 暂无（如需记录「PendingIntent 可变性策略」可由技术方案阶段补 ADR）
