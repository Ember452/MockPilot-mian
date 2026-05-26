# 媒体 API 映射

## HTTP 入口

- `POST /api/xunzhi/v1/websocket/send-message`
- `POST /api/xunzhi/v1/websocket/notification/{userId}`
- `POST /api/xunzhi/v1/websocket/transcription/{userId}`
- `POST /api/xunzhi/v1/websocket/error/{userId}`
- `GET /api/xunzhi/v1/websocket/user/{userId}/status`
- `POST /api/xunzhi/v1/xunfei/tts/tasks`
- `GET /api/xunzhi/v1/xunfei/tts/tasks/{taskId}`
- `POST /api/xunzhi/v1/xunfei/tts/synthesize`

## WebSocket 入口

- `@ServerEndpoint("/api/xunzhi/v1/xunfei/audio-to-text/{userId}")`

## 责任划分

- `WebSocketController`：统一做服务端推送和状态查询。
- `AudioTranscriptionWebSocketHandler`：处理实时转写的连接、命令和数据流。
- `XunfeiTtsController`：处理长文本语音合成任务。