---
name: xunzhi-media-domain
description: AI-Meeting 媒体与实时通信知识 Skill。用于处理实时语音转写、WebSocket 推送、心跳、错误通知、长文本 TTS 和消息协议；当需求命中 `/api/xunzhi/v1/xunfei/**`、`/api/xunzhi/v1/websocket/**` 或 `@ServerEndpoint` 实时链路时使用。
---

# xunzhi-media-domain

## 使用顺序

- 再看 `references/object-dictionary.md`，确认连接对象、转写对象、TTS 对象的真相源。
- 先看 `references/api-map.md`，确认是 WebSocket、转写还是 TTS。
- 再看 `references/realtime-asr.md`，理解实时转写会话与消息协议。
- 再看 `references/websocket-notification.md`，理解服务器推送的消息类型。
- 最后看 `references/tts.md` 和 `references/gotchas.md`。

## 关键入口

- `admin/src/main/java/com/hewei/hzyjy/xunzhi/media/api/WebSocketController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/media/api/XunfeiTtsController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/media/infrastructure/websocket/AudioTranscriptionWebSocketHandler.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/media/infrastructure/integration/XunfeiAudioService.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/media/infrastructure/integration/XunfeiLongTextTtsService.java`

## 必守约束

- WebSocket 连接 session 和业务 `sessionId` 不是一回事，不能混写。
- WebSocket 转写必须先鉴权，再进入转写会话。
- 同一个 session 不能无限叠加多个转写上下文。
- 心跳、开始、停止、状态查询的协议字段必须前后端一致。
- 长文本 TTS 的任务创建和查询是两条不同的生命周期。

## 参考资料

- `references/object-dictionary.md`
- `references/api-map.md`
- `references/realtime-asr.md`
- `references/websocket-notification.md`
- `references/tts.md`
- `references/gotchas.md`
