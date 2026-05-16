---
name: xunzhi-auth-user
description: AI-Meeting 鉴权与用户上下文 Skill。用于处理登录态、token、`@CurrentUser`、当前用户解析、管理员判断、WebSocket 鉴权和按用户维度的数据隔离；当需求涉及 `/api/xunzhi/v1/users/**`、Sa-Token、会话归属或 WebSocket token 解析时使用。
---

# xunzhi-auth-user

## 使用顺序

- 再看 `references/object-dictionary.md`，确认身份对象和服务对象的真相源。
- 先看 `references/current-user.md`，确认当前用户是如何注入控制器的。
- 再看 `references/login-and-permission.md`，确认登录接口和管理员判断语义。
- 涉及 WebSocket 时看 `references/websocket-auth.md`。
- 涉及会话归属和用户隔离时看 `references/data-isolation.md`。

## 关键入口

- `admin/src/main/java/com/hewei/hzyjy/xunzhi/auth/infrastructure/web/CurrentUserArgumentResolver.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/auth/infrastructure/satoken/SaTokenCurrentUserService.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/auth/infrastructure/satoken/SaTokenLoginSessionService.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/auth/infrastructure/websocket/SaTokenWebSocketAuthService.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/auth/infrastructure/satoken/SaTokenPermissionService.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/user/api/UserController.java`

## 必守约束

- `userId` 是归属校验主键，`username` 只是身份展示和登录标识。
- `@CurrentUser` 的支持类型是明确受限的，不能随意扩展调用姿势。
- 带用户上下文的会话和历史查询必须做归属校验。
- WebSocket 鉴权必须同时校验 token 有效性和 path `userId` 一致性。
- `isAdmin` 判断依赖权限服务，不能用前端传参替代。

## 参考资料

- `references/object-dictionary.md`
- `references/current-user.md`
- `references/login-and-permission.md`
- `references/websocket-auth.md`
- `references/data-isolation.md`
