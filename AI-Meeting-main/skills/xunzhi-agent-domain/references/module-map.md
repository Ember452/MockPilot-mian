# Agent 模块映射

## 这个域负责什么

- 创建通用 Agent 会话。
- 通过 SSE 发起流式聊天。
- 查询历史消息和分页列表。
- 上传与 Agent 对话相关的文件资产。
- 管理 Agent 属性配置。
- 把业务场景 code 解析到实际 Agent 配置。

## 不负责什么

- 面试题提取、答题评分、追问推进不属于这个域。
- 面试会话恢复、雷达图、简历预览不属于这个域。
- 实时转写、TTS、WebSocket 推送不属于这个域。

## 典型协作关系

- `agent` 与 `conversation` 协作完成历史消息持久化与归属校验。
- `agent` 与 `auth` 协作完成 `@CurrentUser` 注入和会话归属校验。
- `agent` 与 `ai-runtime` 通过场景绑定、Agent 配置和线程池间接耦合。
- `agent` 与 `interview` 共享“Agent 场景绑定”能力，但业务边界不同。