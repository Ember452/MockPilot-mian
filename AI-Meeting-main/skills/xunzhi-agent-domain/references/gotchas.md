# Agent 域易错点

## 最常见误区

- 不要把面试链路里的 Agent 调用当成通用聊天来改。
- 不要绕过 `conversationOwnershipService` 做历史查询。
- 不要假设所有聊天都能用空 `sessionId` 发起，当前实现要求 `sessionId` 非空。
- 不要只改场景中文名不改绑定配置，场景解析是运行时逻辑。
- 不要忽略文件上传后的资产消费链路，上传只是第一步。

## 现象到检查点

| 现象 | 第一检查点 | 常见原因 |
| --- | --- | --- |
| SSE 聊天一开始就失败 | `sessionId`、会话归属、AgentId 解析 | session 不存在或不属于当前用户 |
| 会话列表查得到，历史查不到 | `requireOwnedConversation(...)` | 查询用户和会话 owner 不匹配 |
| 历史分页为空 | `listOwnedSessionIds(userId)` | 当前用户没有任何归属会话 |
| 明明配了场景却解析不到 Agent | `BusinessAgentResolver` 候选名顺序 | 配置名、默认名、别名都没命中 |
| 上传成功但后续链路用不了 | 文件资产持久化和消费侧 | 只做了上传，没有打通消费流程 |

## 一个实用判断

- 如果现象发生在 `/agents/sessions/{sessionId}/chat`，先查会话归属，再查模型调用。
- 如果现象是“切换用户后能看到别人的会话”，优先怀疑归属校验绕过。
- 如果现象是“场景绑定偶发 fallback”，优先检查配置名与实际 Agent 记录是否一致。
