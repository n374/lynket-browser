<!-- doc-init template version: v1.0 -->
# Capability Delta: web-page-rendering

- **Change**: 12-jandan-comment-load-fail
- **Owner**: 需求官 on behalf of wu.nerd
- **基于 living spec 版本**: 无（该 capability 首次引入，`docs/specs/web-page-rendering/spec.md` 尚未存在，将于本 change 归档时由本 delta 合并生成）

> Delta 按 Requirement 粒度区分动作。本 change 仅含 ADDED。
> 设计阶段允许「覆盖测试」写 `TBD(<描述>)`，归档前必须替换为真实路径。
> 验收以**与对照浏览器（独立运行的 Chrome Beta）行为一致**为基准；复现/验证环境为 **Android 17 真机**。

## ADDED Requirements

### Requirement: 第三方异步评论区加载一致性（基线页）
WHEN 用户经 Lynket 打开包含「JS 异步拉取第三方接口渲染」的评论区的网页，且该页在对照浏览器（独立 Chrome Beta）中评论区能正常加载，THE SYSTEM SHALL 使该评论区在 Lynket 中同样完成加载并渲染出评论内容，而非停留在「加载失败，点击重新加载」。

#### Scenario: jandan 文章页评论区在 Lynket 正常加载
- **GIVEN** 设备为 Android 17，网络可正常访问 jandan 及其评论接口，且独立 Chrome Beta 打开 `https://jandan.net/t/6169986` 时评论区正常加载
- **WHEN** 用户经 Lynket（当前复现配置：以 Chrome Dev/Beta 为 Custom Tabs provider）打开 `https://jandan.net/t/6169986`
- **THEN** 页面下方评论区完成加载并渲染出评论内容，不出现「加载失败，点击重新加载」占位

**覆盖测试**: TBD(真机端到端验证: Android 17 上经 Lynket 打开基线页，断言评论区渲染成功且无加载失败占位)

### Requirement: 动态内容加载与对照浏览器一致（同类站点泛化）
WHEN 用户经 Lynket 打开任意「依赖 JavaScript 异步拉取第三方接口渲染主要/动态内容」的页面，THE SYSTEM SHALL 使该动态内容的加载结果与对照浏览器（独立 Chrome Beta，相同网络环境）一致，不因 Lynket 自身的渲染/启动/会话配置导致额外的加载失败。

#### Scenario: 同类站点动态内容加载与对照浏览器一致
- **GIVEN** 选定 ≥1 个同类站点（依赖 JS 异步第三方接口渲染动态内容；具体站点由技术方案阶段圈定），且其在独立 Chrome Beta 中动态内容正常加载
- **WHEN** 用户经 Lynket 打开该站点对应页面
- **THEN** 其动态内容加载成功，结果与对照浏览器一致

**覆盖测试**: TBD(真机抽样验证: 对技术方案阶段选定的同类站点逐一断言动态内容加载成功)

### Requirement: 既有渲染路径无回归
WHILE 本次修复改动已合入，THE SYSTEM SHALL 保持既有打开/渲染路径（Custom Tabs 正常网页、Article 文章模式、Web heads 后台加载）的原有功能不被破坏。

#### Scenario: 修复后既有路径功能正常
- **GIVEN** 本 change 的修复改动已应用
- **WHEN** 分别经 Custom Tabs 打开普通网页、经文章模式打开可提取文章、经 Web heads 后台加载链接
- **THEN** 三者均按既有预期正常工作，无新增崩溃或加载失败

**覆盖测试**: TBD(回归验证: 既有打开路径冒烟用例 + 相关模块既有 UT 全绿)

### Requirement: 根因改动须有单元测试覆盖
WHERE 本次根因修复落在可单测的配置/启动参数层（如 WebSettings 或 CustomTabsIntent 的构造），THE SYSTEM SHALL 为该改动提供单元测试，断言修复后的关键配置/参数取值符合预期。

#### Scenario: 修复点具备 UT 断言
- **GIVEN** 技术方案阶段定位的根因修复点位于可单测层
- **WHEN** 运行项目单元测试套件
- **THEN** 存在针对该修复点的 UT，且断言修复后的配置/参数取值正确并通过

**覆盖测试**: TBD(单元测试: Robolectric/JUnit 断言修复点的配置或意图构造，路径随技术方案确定)

## 3. 非功能需求（NFR）

### NFR-1: 真机验证（交付门槛）
- **类别**: 可靠性 / 交付质量
- **目标指标**: 在 **Android 17 真机**上，基线页（`https://jandan.net/t/6169986`）评论区加载成功率达成预期（与对照浏览器一致）
- **测量方式**: 真机手动复现 + 验证：修复前可稳定复现「加载失败」，修复后稳定加载成功
- **验收测试或监控项**: TBD(真机验证记录: 修复前/后对比截图或录屏 + 验证环境说明)
- **不达标后果**: 阻断交付（开发/MR 阶段不得在未真机验证通过前合入）

## 关联

- 关联 proposal: [../../proposal.md](../../proposal.md)
- 关联 design: ../../design.md（待产出）
- 关联 ADR: 暂无
