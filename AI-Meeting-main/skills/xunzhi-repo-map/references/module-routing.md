# 模块路由

## 模块职责

- `auth`：登录态、`@CurrentUser`、权限、WebSocket 鉴权。
- `agent`：通用 Agent 会话、SSE 聊天、Agent 属性、文件上传。
- `interview`：面试会话、题目提取、答题流水线、评分、追问、恢复、收尾。
- `media`：实时转写、WebSocket 推送、长文本 TTS。
- `conversation`：会话消息历史、流式会话消息持久化、会话归属。
- `ai`：模型接入、工具编排、统一 AI 调用能力。
- `shared`：通用 DTO、枚举、结果封装、公共约束。

## 典型切法

- 看到 `/api/xunzhi/v1/interview/**`，先去 `xunzhi-interview-domain`。
- 看到 `/api/xunzhi/v1/agents/**`，先去 `xunzhi-agent-domain`。
- 看到 `/api/xunzhi/v1/xunfei/**` 或 WebSocket 转写端点，先去 `xunzhi-media-domain`。
- 看到登录、token、`@CurrentUser`、管理员判断，先去 `xunzhi-auth-user`。
- 看到 `xunzhi-agent.ai-guard`、`xunzhi-agent.ai-singleflight`、`xunzhi-agent.flow-limit`，先去 `xunzhi-ai-runtime`。

## 边界判断

- 面试域改动常常会连到 `agent` 的场景绑定和 `media` 的转写输入，但主域仍是 `interview`。
- 只要改动的核心目标是“答题流程、评分结果、追问规则、会话状态”，就不要把它当成通用 Agent 改动。
- 只要改动的核心目标是“聊天记忆、普通对话、文件资产”，就不要把它当成面试链路。
- 只要改动的核心目标是“实时音频、推送、TTS”，就不要把它当成纯 REST 接口。

## 交接信号

- 需求同时涉及一个域的 API 和另一个域的配置，说明已经越过总路由层，应该切到对应领域 Skill。
- 需求同时涉及工作流 YAML、Java DTO、缓存键和状态机，说明应该切到 `xunzhi-change-playbook`。