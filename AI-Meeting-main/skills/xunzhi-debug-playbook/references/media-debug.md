# 媒体链路排障剧本

## 常见现象

- WebSocket 连不上。
- 已连接但没有心跳。
- `start_transcription` 后没有结果。
- TTS 任务一直查不到完成态。

## 先看哪些对象

| 现象 | 先看对象 | 真相源 |
| --- | --- | --- |
| 连不上 | `token`、`pathUserId`、`SESSION_USER_MAP` | 鉴权结果 + 内存映射 |
| 没心跳 | `WebSocketResponse`、连接状态 | 连接管理 |
| 没转写 | `TranscriptionSessionContext`、`RealtimeTranscriptionUpdate` | 内存上下文 + 引擎回调 |
| TTS 查不到完成态 | `taskId`、`taskStatus` | 平台任务系统 |

## 优先检查

1. WebSocket token 是否有效、path `userId` 是否匹配。
2. `AudioTranscriptionWebSocketHandler.onOpen` 是否成功登记 session。
3. 是否触发了 `transcription_already_started` 或 `transcription_already_stopped`。
4. 停止转写后出现 `Pipe closed` / `Stream closed` 是否属于预期关闭。
5. TTS 走的是异步任务还是同步等待接口。

## 常见根因

- 鉴权失败但前端只看到了“连接创建成功”。
- 建连成功，但 `TranscriptionSessionContext` 没真正进入 active 状态。
- 停止路径与最终文本回包的时序没理顺。
- TTS 任务受理成功，但查询还没等到完成态。

## 处理建议

- 先确认连接层，再确认业务层，再确认引擎回调层。
- 如果现象是“能连上但没文本”，优先看上下文是否 active。
- 如果现象是“TTS 不返回”，先看任务是否受理，再看轮询状态。
