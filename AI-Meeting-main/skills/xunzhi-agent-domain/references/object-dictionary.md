# Agent 域对象词典

## 1. 会话对象

| 对象 | 真相源 | 作用 | 谁写 | 谁读 |
| --- | --- | --- | --- | --- |
| `AgentConversation` | MongoDB `agent_conversation` | 通用会话主记录 | 创建会话、结束会话、标题回填 | 会话列表、详情页、历史查询 |
| `AgentMessage` | MongoDB `agent_message` | 通用消息历史主记录 | 聊天发送、模型回包 | 历史分页、详情回放 |
| `AgentConversationRespDTO` | 接口视图 | 会话列表和详情展示 | 控制器组装 | 前端列表页、详情页 |
| `AgentMessageHistoryRespDTO` | 接口视图 | 会话消息历史分页展示 | 查询接口 | 前端历史页 |
| `AgentSessionCreateReqDTO` / `RespDTO` | 接口契约 | 创建会话的最小输入输出 | 创建接口 | 前端创建页 |
| `UserMessageReqDTO` | 接口契约 | SSE 聊天请求 | 前端聊天入口 | 聊天流水线 |

## 2. 配置与场景对象

| 对象 | 真相源 | 作用 | 说明 |
| --- | --- | --- | --- |
| `AgentPropertiesDO` | MySQL `agent_properties` | 实际 Agent 配置记录 | `agentName` 是运行时解析入口，不是展示文案 |
| `BusinessAgentScene` | Java 枚举 | 业务场景目录 | 决定默认 Agent、候选 Agent 顺序 |
| `BusinessAgentBindingProperties` | `application.yaml` | 场景到 Agent 名称映射 | 变更后要同步配置索引 |

## 3. 归属与边界对象

| 对象 | 含义 | 关键点 |
| --- | --- | --- |
| `conversationOwnershipService` | 会话归属校验服务 | 所有历史查询和结束会话都要先过它 |
| `AgentMessagePersistencePort` | Agent 消息持久化接口 | 对话历史真相源不在 controller |
| `ConversationMessageHistoryService` | 历史读取适配层 | 只负责读，不负责改语义 |

## 4. 生命周期

1. `createSession` 创建 `AgentConversation`，首条消息可用于生成标题。
2. `agentChatSse` 写入 `AgentMessage`，并通过 SSE 逐段返回结果。
3. 历史查询先校验归属，再按会话分页读取消息。
4. 结束会话时只改变会话状态，不应破坏消息历史。
5. 文件上传只是资产进入流程的起点，后续还要看消费链路。

## 5. 关键不变量

- `sessionId` 必须属于当前用户。
- SSE 聊天必须先通过会话归属校验。
- `BusinessAgentScene` 的候选名顺序决定最终命中哪个 Agent。
- Agent 会话是通用对话，不要把面试逻辑塞进去。
- `AgentPropertiesDO.agentName` 是运行时解析关键，不是纯展示文案。

## 6. 常见误解

- `AgentConversation` 是会话真相源，不是临时缓存。
- `AgentMessageHistoryRespDTO` 只是视图对象，不是持久化对象。
- `messageSeq` 是会话内顺序，不是全局顺序。
- `messageCount` 是汇总指标，不是回写来源。
