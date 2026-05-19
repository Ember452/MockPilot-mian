---
name: xunzhi-change-playbook
description: AI-Meeting 变更剧本 Skill。用于处理跨接口、工作流、配置、缓存、运行时或多业务域的改动；当需求不是单文件微调，而是需要考虑影响面、校验顺序、回滚策略和跨层一致性时使用。
---

# xunzhi-change-playbook

## 使用顺序

- 先看 `references/cross-domain-checklist.md`，判断改动覆盖哪些层。
- 改面试接口时看 `references/interview-api-change.md`。
- 改配置时看 `references/runtime-config-change.md`。
- 改 workflow 时看 `references/workflow-change.md`。

## 核心原则

- 先定对象，再定契约，再定实现。
- 主记录、运行态缓存、快照、归档、接口 DTO 的改法不一样。
- 改动只要跨两层以上，就必须显式检查影响面和回滚点。

## 参考资料

- `references/cross-domain-checklist.md`
- `references/interview-api-change.md`
- `references/runtime-config-change.md`
- `references/workflow-change.md`
