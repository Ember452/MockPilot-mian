# WebSocket 鉴权

## 鉴权入口

`SaTokenWebSocketAuthService.isAuthorized(session, pathUserId)` 的判断顺序是：

1. 解析 token。
2. 校验 token 是否有效。
3. 解析 token 对应的 username。
4. 先尝试 username 与 path `userId` 直接相等。
5. 不相等时，再把 username 转成 userId，与 path `userId` 比较。

## token 提取优先级

WebSocket 请求参数目前按下面顺序取 token：

- `token`
- `Authorization`
- `authorization`
- `access_token`
- `satoken`

## 规范化规则

- 如果 token 以 `Bearer ` 开头，会自动裁掉前缀。
- path 里放的是 `userId`，但实现允许直接传 username 并先做一次比对。

## 为什么这里容易踩坑

- token 有效不代表 path `userId` 合法，二者必须匹配。
- WebSocket 不是从普通 HTTP 拦截器里自动拿登录态，所以不能假设 `@CurrentUser` 那套会自动生效。