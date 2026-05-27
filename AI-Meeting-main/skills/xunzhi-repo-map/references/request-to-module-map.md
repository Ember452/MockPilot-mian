# 需求到模块映射

## 需求关键词 -> 主域

- “登录、token、当前用户、管理员、WebSocket 鉴权” -> `xunzhi-auth-user`
- “Agent 聊天、会话历史、文件上传、属性管理” -> `xunzhi-agent-domain`
- “面试创建、提题、答题、评分、追问、恢复、简历预览、神态分析” -> `xunzhi-interview-domain`
- “实时转写、推送、心跳、TTS” -> `xunzhi-media-domain`
- “限流、守护、singleflight、线程池、运行时配置” -> `xunzhi-ai-runtime`
- “接口、DTO、工作流、配置一起改” -> `xunzhi-change-playbook`
- “卡住、重复、重试、恢复失败、定位链路” -> `xunzhi-debug-playbook`

## 判定规则

- 只要出现会话状态、题号流转、分数提交、追问次数，就先判定为面试域。
- 只要出现 `@CurrentUser`、`StpUtil`、`isAdmin`、`logoutByToken`，就先判定为鉴权域。
- 只要出现 `SseEmitter`、`@ServerEndpoint`、`heartbeat`、`transcription`、`tts`，就先判定为媒体域。
- 只要出现 `AI Guard`、`SingleFlight`、`flow-limit`、`thread-pool`，就先判定为运行时域。

## 不要误判

- “看起来像 Agent 对话” 不一定是通用 Agent；如果它在面试链路里服务提问或评分，主域仍是面试。
- “看起来像聊天消息” 不一定是对话业务；如果它依赖会话状态机和题号推进，主域仍是面试。
- “看起来像 WebSocket” 不一定是媒体；如果它只是用于鉴权或状态查询，先看 auth 或对应业务域。