<!-- doc-init template version: v1.0 -->
# Decisions Index（架构决策记录 / ADR）

> 本 section 记录 Lynket 项目的**架构决策**（Architecture Decision Records）。每条 ADR 一个不可变的决策快照。
>
> **编号规则**：4 位 `NNNN`，从 `0001` 起，**不跳号、不复用**。新增 ADR 前先在下表抢占编号。

## 编号锁定表 / 文档地图

| ADR | 标题 | 状态 | 日期 | 提要 |
|---|---|---|---|---|
| [0001](./0001-native-bubbles-require-targetsdk-30.md) | 原生气泡依赖 targetSdk ≤ 30，高 targetSdk 无自动原生气泡方案 | Accepted | 2026-07-04 | Android targetSdk 30+ 限制气泡自动展示；固定 `targetSdk=29` 保证原生气泡自动弹出；高 SDK 备选 Web Heads / 会话重构+用户授权。 |

## 角色入门

- 想改 `targetSdk` / 动气泡相关代码前：**必读 [ADR-0001](./0001-native-bubbles-require-targetsdk-30.md)**（上调 targetSdk 会破坏原生气泡）。
- 新增决策：在上表抢占下一个编号（`0002`…），按 doc-init skill 的 ADR 模板写（规约见 [docs/AGENTS.md](../AGENTS.md)）。
