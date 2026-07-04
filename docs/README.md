<!-- doc-init template version: v1.0 -->
# Lynket 文档 / Documentation

> 项目所有文档的入口与导航。详细规约见 [AGENTS.md](./AGENTS.md)。
>
> 本 `docs/` 体系为**最小骨架**起步（随 [change 12-jandan-comment-load-fail](./changes/12-jandan-comment-load-fail/proposal.md) 首次建立）。标注「待建」的区域将随后续 change 逐步补齐，避免一次性产出空壳文档。

## 文档地图

| 区域 | 路径 | 状态 | 说明 |
|---|---|---|---|
| 进行中变更 | [changes/](./changes/) | ✅ | proposal / design / tasks / spec-delta |
| Living spec | [specs/](./specs/) | ✅ | 各 capability 的 source of truth |
| 世界观 | overview/ | 待建 | 项目背景、技术栈、宪法、术语 |
| 架构 | architecture/ | 待建 | 总览、模块、数据流、技术选型 |
| 接口契约 | api/ | 待建 | 浏览器无对外服务接口，按需补 |
| 运维 SOP | operations/ | 待建 | 客户端 App，按需补 |
| 决策记录 | [decisions/](./decisions/) | ✅ | ADR（[ADR-0001](./decisions/0001-native-bubbles-require-targetsdk-30.md)：原生气泡 vs targetSdk） |
| 归档 | archive/ | 待建 | 已完成 change |

## 文档规约

所有文档规约见 [AGENTS.md](./AGENTS.md)。任何写文档的动作必须由 `doc-init` skill 主导。
