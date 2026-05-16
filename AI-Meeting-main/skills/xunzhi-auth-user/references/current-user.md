# 当前用户

## `@CurrentUser` 支持哪些参数类型

`CurrentUserArgumentResolver` 当前只支持三类：

- `String`：返回当前用户名。
- `Long/long`：返回当前用户 ID。
- `UserContext`：返回 `{userId, username}`。

如果传了其他类型，会直接抛：

- `@CurrentUser only supports String, Long/long and UserContext`

## 当前主体模型

- `SaTokenCurrentUserService.getCurrentPrincipal()` 会返回 `CurrentPrincipal(userId, username)`。
- `requireCurrentPrincipal()` 在未登录时会抛 `User is not logged in`。
- `getCurrentUserId()` 本质上是先拿 username，再查 userId。

## 注入链路

1. 请求先通过登录态校验。
2. 控制器方法参数遇到 `@CurrentUser` 时进入 `CurrentUserArgumentResolver`。
3. 解析器从当前主体中取 `userId` / `username` / `UserContext`。
4. 业务层继续使用 `userId` 做归属校验，不能停在 controller 注入层。

## 用户 ID 缓存

- `getUserIdByUsername` 会把 username -> userId 缓存到 Redis。
- 缓存 key 前缀：`xunzhi:user:id:`。
- 命中缓存后会续期 30 分钟。

## 一个实用判断

- 能拿到 `username` 但看不到会话，优先查 `userId` 解析和归属校验。
- WebSocket 不会自动走这套解析链，所以不能假设 `@CurrentUser` 会生效。
