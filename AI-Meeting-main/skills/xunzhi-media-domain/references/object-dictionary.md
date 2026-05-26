# 媒体域对象词典

## 1. WebSocket 连接对象

| 对象 | 含义 | 真相源 | 谁写 | 谁读 |
| --- | --- | --- | --- | --- |
| `USER_SESSIONS` | 用户到连接的映射 | 内存态 | 建连、断连 | 推送层、连接管理 |
| `SESSION_USER_MAP` | 连接到用户的映射 | 内存态 | 建连、断连 | 推送层、鉴权后校验 |
| `WebSocketMessage` | 客户端控制命令 | WebSocket 入参 | 前端 | WebSocket handler |
| `WebSocketResponse` | 服务端统一响应 | WebSocket 出参 | 服务端 | 前端 |
| `TranscriptionSessionContext` | 单连接转写上下文 | 内存态 | start/stop 链路 | 转写执行器、停止逻辑 |

## 2. 实时转写对象

| 对象 | 含义 | 关键字段 | 说明 |
| --- | --- | --- | --- |
| `RealtimeTranscriptionUpdate` | 实时转写增量 | `fullText`、`committedText`、`liveText`、`displayText`、`revision`、`resultStatus`、`segmentId`、`segmentText`、`pgs`、`rg`、`bg`、`ed`、`finalPacket` | 直接决定前端如何展示流式文本 |
| `fullText` | 全量文本 | 完整结果 | 最终展示的完整文本 |
| `committedText` | 已提交文本 | 稳定片段 | 一般可持久展示 |
| `liveText` | 实时文本 | 当前滚动片段 | 会被后续修订覆盖 |
| `displayText` | 前端展示文本 | 渲染用文本 | 前端不一定自己拼接 |
| `revision` | 版本号 | 更新顺序 | 处理乱序和重绘 |

## 3. TTS 对象

| 对象 | 含义 | 真相源 | 说明 |
| --- | --- | --- | --- |
| `LongTextTtsReqDTO` | 长文本合成请求 | HTTP 请求体 | 任务创建入口 |
| `LongTextTtsTaskRespDTO` | 长文本合成任务响应 | HTTP 响应体 | 任务状态与结果视图 |
| `taskId` | 任务主键 | 平台任务系统 | 创建后用于轮询查询 |
| `taskStatus` | 任务状态 | 平台状态 | 不是本地枚举真相 |
| `audioBase64` / `audioUrl` | 合成结果 | 平台回写或下载地址 | 可能为空直到完成 |
| `pybufContent` / `pybufUrl` | 拼音内容 | 平台辅助结果 | 非所有场景都有 |

## 4. 生命周期

1. WebSocket `onOpen` 先鉴权，再登记连接映射。
2. 客户端发送 `start_transcription` 后创建 `TranscriptionSessionContext`。
3. 转写引擎持续产出 `RealtimeTranscriptionUpdate`，服务端推送 `transcription` 事件。
4. 收到 `finalPacket` 或显式停止时，发送 `final` 或停止态事件并清理上下文。
5. TTS 先创建任务，再通过 `taskId` 查询完成态和音频结果。

## 5. 关键不变量

- `userId` 既是路由参数，也是鉴权输入。
- 同一连接上下文不能重复开启多个转写上下文。
- `ping/pong` 只是保活，不是业务事件。
- `transcription_already_started` 是幂等语义，不是系统故障。
- `final` 是终态，`transcription` 是中间态。

## 6. 常见误判

- WebSocket 连接 session 不等于业务会话 `sessionId`。
- `stop_transcription` 成功不等于一定会立刻拿到最终文本。
- `Pipe closed` / `Stream closed` 在停止路径上可能是正常现象。
- TTS 创建成功只表示任务受理，不表示音频已经可取。
