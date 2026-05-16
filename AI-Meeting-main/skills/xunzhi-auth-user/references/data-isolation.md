# 数据隔离

## 会话隔离原则

- Agent 会话历史查询会先走 `conversationOwnershipService.requireOwnedConversation(sessionId, userId)`。
- Agent 全量分页历史会先拿当前用户拥有的 `sessionIds`，再分页查这些会话的数据。
- 面试会话查询会先走 `interviewSessionService.requireOwnedSession(sessionId, userId)`。

## 为什么必须做这层

- `sessionId` 本身不是权限凭证。
- 只要接口支持传入 `sessionId`，就必须在服务层再次校验归属。
- 只在 controller 层做登录校验不够，因为会话 ID 是跨请求携带的业务标识。

## 排障时的一个判断

- 如果用户“能登录但看不到自己的历史”，优先检查归属服务和用户 ID 传递。
- 如果用户“看到了别人的数据”，优先检查是否有绕过归属校验的查询分支。