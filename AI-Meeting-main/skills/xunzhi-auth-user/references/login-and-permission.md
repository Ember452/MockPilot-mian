# 登录与权限

## 登录相关接口

- `POST /api/xunzhi/v1/users/login`
- `GET /api/xunzhi/v1/users/check-login`
- `POST /api/xunzhi/v1/users/logout`
- `GET /api/xunzhi/v1/users/is-admin`
- `POST /api/xunzhi/v1/users/admin`

## 登录返回语义

登录成功后会返回：

- `token`
- `username`
- `isAdmin`

`check-login` 会返回：

- `isLogin`
- 如果已登录，还会带 `username` 和 `token`

## LoginSessionService 语义

- `login(username)`：建立登录态。
- `getCurrentToken()`：取当前 token。
- `getCurrentLoginId()`：取当前 loginId。
- `logoutByToken(token)`：按 token 登出。
- `getTokenTimeout(token)`：返回 token 剩余时间；无效 token 返回 `-2`。

## 权限语义

- `PermissionService.isAdmin(username)` 最终走 `AdminPermissionService.isAdmin(username)`。
- `POST /users/admin` 受 `@SaCheckRole("admin")` 保护。
- 所以“是否管理员”是后端运行时判断，不应由前端自行缓存为可信事实。

## 一个实用判断

- 登录成功不等于拿到了会话归属权限。
- `isAdmin = true` 只说明管理员角色成立，不代表可以跳过业务域里的归属校验。
