# 鉴权与用户对象词典

## 1. 核心身份对象

| 对象 | 含义 | 真相源 | 说明 |
| --- | --- | --- | --- |
| `CurrentPrincipal` | 当前认证主体 | `Sa-Token` + `UserDO` | 真正的登录主体，不是前端传参 |
| `UserContext` | 控制器层上下文 | 请求上下文 | 用于接口层传递 `userId` 和 `username` |
| `UserDO` | 用户持久化实体 | MySQL `t_user` | 用户最终信息来源 |

## 2. 服务对象

| 对象 | 作用 | 真相源 | 说明 |
| --- | --- | --- | --- |
| `LoginSessionService` | 登录态服务 | `Sa-Token` | 登录、登出、取 token、取 loginId |
| `PermissionService` | 权限判断服务 | 权限系统 | 管理员判断不能前端自证 |
| `WebSocketAuthService` | WebSocket 鉴权服务 | 运行时鉴权逻辑 | 处理连接级鉴权 |
| `CurrentUserArgumentResolver` | 参数解析器 | Web MVC 解析链 | 把当前主体注入 controller |

## 3. 读写与校验矩阵

| 对象 | 谁产生 | 谁消费 | 不能替代什么 |
| --- | --- | --- | --- |
| `token` | 登录态服务 | HTTP 接口、WebSocket 鉴权 | 会话归属证明 |
| `userId` | `CurrentPrincipal` / `UserDO` | 会话归属、数据隔离、路径校验 | 展示昵称 |
| `username` | 登录态、用户表 | 登录标识、展示信息 | 归属主键 |
| `isAdmin` | 权限服务 | 管理员接口、前端展示 | 任意接口的通行证 |

## 4. 生命周期

1. 登录后生成 token，建立登录态。
2. 普通 HTTP 接口通过 `@CurrentUser` 解析当前主体。
3. 会话查询、历史查询、结束会话等路径，继续用 `userId` 做归属校验。
4. WebSocket 单独做 token 提取和 path `userId` 匹配校验。
5. 登出后 token 失效，后续所有依赖当前主体的操作都必须失败。

## 5. 关键不变量

- `userId` 是归属校验主键。
- `username` 是登录和展示标识。
- `@CurrentUser` 只支持 `String`、`Long/long`、`UserContext`。
- WebSocket 鉴权必须同时验证 token 和 path `userId`。
- 管理员判断必须后端计算，不能由前端自证。

## 6. 常见误解

- token 有效不等于能访问任意会话。
- 只做登录检查，不做归属检查，等于没做隔离。
- `check-login` 返回的是登录态视图，不是权限证明。
- `username` 和 `userId` 偶尔值相同，不代表两者可以混用。
