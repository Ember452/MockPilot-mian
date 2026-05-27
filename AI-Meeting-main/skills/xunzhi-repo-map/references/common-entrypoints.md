# 常见入口文件

## 先看这些入口

- `admin/src/main/java/com/hewei/hzyjy/xunzhi/interview/api/InterviewSessionController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/interview/api/InterviewResumeController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/agent/api/AgentController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/agent/api/AgentFileController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/agent/api/AgentPropertiesController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/media/api/WebSocketController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/media/api/XunfeiTtsController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/auth/infrastructure/web/CurrentUserArgumentResolver.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/auth/infrastructure/websocket/SaTokenWebSocketAuthService.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/interview/application/flow/InterviewFlowStateMachine.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/interview/flow/answer/InterviewAnswerPipeline.java`

## 入口对应的事情

- 面试创建、提题、答题、恢复、收尾，优先看 `InterviewSessionController` 和 `InterviewSessionFacade`。
- 简历预览，优先看 `InterviewResumeController` 和 `InterviewResumePreviewService`。
- Agent 聊天、历史消息、结束会话，优先看 `AgentController` 和 `AgentMessageServiceImpl`。
- WebSocket 在线状态、消息推送、转写结果，优先看 `WebSocketController`。
- 长文本 TTS，优先看 `XunfeiTtsController`。
- 登录态和 `@CurrentUser` 解析，优先看 `CurrentUserArgumentResolver` 和 `SaTokenCurrentUserService`。

## 先后顺序

- 先找 controller，再找 facade/service，再找 runtime 或 workflow。
- 如果 controller 只是透传，继续下钻到真正持有业务约束的类。
- 如果入口触发了状态机、幂等、限流、singleflight 或缓存回写，先看对应的运行时 Skill。