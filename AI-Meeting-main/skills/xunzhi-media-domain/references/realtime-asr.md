# 实时 ASR

## 连接对象

- 连接地址：`/api/xunzhi/v1/xunfei/audio-to-text/{userId}`
- `onOpen` 时先做鉴权，再登记 `USER_SESSIONS`、`SESSION_USER_MAP`。
- 鉴权失败会直接关闭 session。

## 控制命令

- `ping` -> `pong`
- `start_transcription` -> 开始转写
- `stop_transcription` -> 停止转写
- `get_status` -> 返回健康状态
- 其他值 -> `unknown_command`

## 状态事件

- `connected`
- `heartbeat`
- `transcription_started`
- `transcription_stopped`
- `transcription_already_started`
- `transcription_already_stopped`
- `transcription`
- `final`
- `error`

## 生命周期

1. 建连后先通过鉴权，再建立连接映射。
2. 收到 `start_transcription` 后，创建 `TranscriptionSessionContext`。
3. 引擎持续回传 `RealtimeTranscriptionUpdate`，服务端按 `transcription` 推送。
4. 收到终态或停止指令后，发送 `final` / 停止事件并释放资源。
5. 任何重复 start/stop 都应该走幂等语义，而不是直接判故障。

## 关键实现点

- 心跳每 30 秒发一次。
- 转写过程中会把中间更新作为 `transcription` 事件推回前端。
- 完成后会发送 `final` 事件。
- `stopTranscriptionSession` 会关闭 pipe，底层出现 `Pipe closed` / `Stream closed` 属于预期停止路径。

## 一个实用判断

- 连不上，先看鉴权和 path `userId`。
- 能连上但没文本，先看上下文是否 active。
- 有文本但不稳定，先看 `finalPacket` 和修订顺序。
