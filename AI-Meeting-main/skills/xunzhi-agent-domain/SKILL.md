---
name: xunzhi-agent-domain
description: AI-Meeting 通用 Agent 业务知识 Skill。用于处理通用 Agent 会话创建、SSE 聊天、历史消息、会话归属、Agent 属性管理、文件上传与业务场景绑定；当需求命中 `/api/xunzhi/v1/agents/**`、`/api/xunzhi/v1/agent-properties/**` 或通用 Agent 会话行为时使用。
---

# xunzhi-agent-domain

当需求属于“通用 Agent 会话层”，而不是面试专属链路时，使用这个 Skill。

## 使用顺序

- 再看 `references/object-dictionary.md`，确认会话、消息、配置、归属对象的真相源。
- 先看 `references/module-map.md`，分清通用 Agent 和面试 Agent 的边界。
- 再看 `references/session-flow.md`，确认建会话、聊天、分页、结束的调用链。
- 涉及场景名与 Agent 名称绑定时看 `references/agent-binding.md` 和 `references/generated-agent-scene-map.md`。
- 涉及易错边界时看 `references/gotchas.md`。

## 关键入口

- `admin/src/main/java/com/hewei/hzyjy/xunzhi/agent/api/AgentController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/agent/api/AgentFileController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/agent/api/AgentPropertiesController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/agent/service/impl/AgentMessageServiceImpl.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/agent/application/BusinessAgentScene.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/agent/application/BusinessAgentResolver.java`

## 必守约束

- `sessionId` 不是展示字段，它是会话主键语义；历史查询必须做归属校验。
- 通用 Agent 会话和面试主链路要保持边界清晰。
- 所有带用户上下文的会话查询都必须保留归属校验。
- SSE 聊天必须有有效 `sessionId`，不能跳过会话存在性和归属校验。
- 场景绑定名不是展示文案，而是实际运行时选择哪个 Agent 的入口。

## 参考资料

- `references/object-dictionary.md`
- `references/module-map.md`
- `references/session-flow.md`
- `references/agent-binding.md`
- `references/generated-agent-scene-map.md`
- `references/gotchas.md`
- `scripts/extract_agent_scene_map.py`
