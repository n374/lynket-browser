<!-- doc-init template version: v1.0 -->
# Lynket 文档规约 / Documentation Conventions

> 本文件是 **Lynket** 项目的文档规约入口。详细硬规则见 doc-init skill 的 `~/.claude/skills/doc-init/AGENTS.md`。
> 本文件只列项目特有的扩展约束，不复述 skill 已定义的内容。

## 项目特有扩展

### 必须落地的扩展制品

- 暂无强制扩展制品。后续若引入隐私/权限相关 capability，再按 constitution 触发 `docs/security/`。

### capability 命名约定

- 采用领域语义命名（如 `web-page-rendering`），不带版本后缀。

### change 命名说明（与 skill 约定的偏差，已记录）

- doc-init 默认要求 change slug 为不带编号的 kebab-case。
- 本仓库的 change 由 Multica 编排链路驱动，slug **带父 Issue 序号前缀**（如 `12-jandan-comment-load-fail`），用于贯穿「需求 → 技术方案 → 开发 → MR」多阶段与共享分支 `feat/issue-<seq>` 的跨阶段可追溯性。此为有意约定，非违规。

## 强制流程入口

- **新增 capability / 新功能**：建 `changes/<slug>/` 走 proposal → design → tasks → spec-delta → archive
- **架构决策**：写 `decisions/NNNN-<topic>.md`
- **修改 living spec**：必须经过一个 change 的 archive
- **写文档前**：doc-init skill 自动加载本文件 + skill AGENTS.md + constitution.md（如存在）

## 与 doc-init skill 的关系

| 位置 | 职责 | 谁维护 |
|---|---|---|
| `~/.claude/skills/doc-init/AGENTS.md` | 跨项目通用硬规则（EARS / 分工 / Owner 等） | doc-init skill 维护者 |
| 本文件（`docs/AGENTS.md`） | 项目特有扩展约束 | 项目 Tech Lead |
| `docs/overview/constitution.md` | 项目红线（待建） | 项目架构组 |
