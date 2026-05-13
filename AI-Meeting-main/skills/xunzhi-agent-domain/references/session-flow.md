# Agent 会话流程

## 一条主链路

通用 Agent 会话的核心链路是：创建会话 -> 发起聊天 -> 写消息历史 -> 分页查看 -> 结束会话。

## 1. 创建会话

- 接口：`POST /api/xunzhi/v1/agents/sessions`
- 入口：`AgentController.createSession`
- 默认场景：`BusinessAgentScene.GENERAL_AGENT_CHAT`
- 真正创建动作：`agentConversationService.createConversationWithTitle(...)`

### 这个阶段写什么

- 创建 `AgentConversation` 主记录。
- 首条消息可能用于生成 `conversationTitle`。
- 会话归属在这里就确定为当前 `userId`。

## 2. 发起聊天

- 接口：`POST /api/xunzhi/v1/agents/sessions/{sessionId}/chat`
- 控制器会把 `sessionId`、当前用户名写回 `UserMessageReqDTO`。
- `AgentMessageServiceImpl.agentChatSse(requestParam, userId)` 先做会话归属校验，再进入 SSE 流。
- 真正流式输出通过 `XingChenAIClient.chat(...)` 把 chunk 逐段发送给 `SseEmitter`。

### 这个阶段要守什么

- 没有有效 `sessionId` 不应该直接进聊天。
- 没通过归属校验，不能查历史，也不能聊天。
- `messageSeq` 是会话内顺序，不是全局顺序。

## 3. 查询历史

- 单会话历史：`/conversations/{sessionId}/messages`
- 分页历史：`/messages/history`
- 如果传了 `sessionId`，会先校验会话归属。
- 如果不传 `sessionId`，会先列出用户拥有的会话，再做分页查询。

### 这个阶段读什么

- 读 `AgentConversation` 判断会话是否存在、是否归属当前用户。
- 读 `AgentMessage` 做历史分页、倒序/正序展示。
- 读场景与 Agent 配置，用于补展示信息或继续对话。

## 4. 结束会话

- 接口：`PUT /api/xunzhi/v1/agents/conversations/{sessionId}/end`
- 结束动作：`agentConversationService.endConversation(sessionId, userId)`

### 这个阶段不要做什么

- 不要把结束会话理解成删除历史。
- 不要绕过归属校验直接按 `sessionId` 结束。

## 5. SSE 结束信号

- 正常结束时会发送名为 `end` 的事件，数据为 `[DONE]`。
- 如果流式发送中抛错，错误由统一 `errorHandler` 收口。

## 一个实用判断

- 创建失败，先看场景与归属。
- 聊天失败，先看 `sessionId` 与归属，再看模型调用。
- 历史为空，先看拥有的会话集合，再看消息持久化。
