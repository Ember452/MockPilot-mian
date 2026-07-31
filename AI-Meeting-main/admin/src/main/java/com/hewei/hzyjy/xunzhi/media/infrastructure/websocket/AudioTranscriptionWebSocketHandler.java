package com.hewei.hzyjy.xunzhi.media.infrastructure.websocket;

import com.alibaba.fastjson2.JSON;
import com.hewei.hzyjy.xunzhi.auth.application.WebSocketAuthService;
import com.hewei.hzyjy.xunzhi.media.infrastructure.integration.XunfeiAudioService;
import com.hewei.hzyjy.xunzhi.media.infrastructure.integration.XunfeiAudioService.RealtimeTranscriptionUpdate;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Real-time speech-to-text WebSocket endpoint.
 * 实时语音转写WebSocket端点
 */
@Slf4j
@Component
@ServerEndpoint(value = "/api/xunzhi/v1/xunfei/audio-to-text/{userId}")
public class AudioTranscriptionWebSocketHandler {

    private static volatile XunfeiAudioService xunfeiAudioService;
    private static volatile WebSocketAuthService webSocketAuthService;
    private static volatile ScheduledExecutorService heartbeatExecutor;

    @Autowired
    public void setXunfeiAudioService(XunfeiAudioService service) {
        AudioTranscriptionWebSocketHandler.xunfeiAudioService = service;
    }

    @Autowired
    public void setWebSocketAuthService(WebSocketAuthService service) {
        AudioTranscriptionWebSocketHandler.webSocketAuthService = service;
    }

    @Autowired
    public void setHeartbeatExecutor(@Qualifier("scheduledExecutorService") ScheduledExecutorService scheduledExecutorService) {
        AudioTranscriptionWebSocketHandler.heartbeatExecutor = scheduledExecutorService;
    }

    private static final ConcurrentMap<String, Session> USER_SESSIONS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> SESSION_USER_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, TranscriptionSessionContext> TRANSCRIPTION_CONTEXTS =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ScheduledFuture<?>> HEARTBEAT_TASKS = new ConcurrentHashMap<>();

    /**
     * WebSocket建立连接入口
     * 鉴权，记录会话，启动心跳。
     * @ OnOpen 这个注解是java WebSocket规范中定义的一个注解，作用是标记一个方法，当新的
     *          WebSocket建立连接时，该方法自动被调用。
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        if (!isAuthorizedUser(session, userId)) {
            log.warn("WebSocket auth failed, userId={}, sessionId={}", userId, session.getId());
            closeSession(session, "Unauthorized websocket connection");
            return;
        }

        String sessionId = session.getId();
        USER_SESSIONS.put(userId, session);
        SESSION_USER_MAP.put(sessionId, userId);
        log.info("WebSocket connected, userId={}, sessionId={}", userId, sessionId);

        sendMessage(session, createResponse("connected", "WebSocket connected", userId));
        startHeartbeat(session);
    }

    private boolean isAuthorizedUser(Session session, String pathUserId) {
        if (webSocketAuthService == null) {
            log.error("WebSocketAuthService is not injected, reject websocket connection");
            return false;
        }
        return webSocketAuthService.isAuthorized(session, pathUserId);
    }

    private void closeSession(Session session, String reason) {
        if (session == null) {
            return;
        }
        try {
            if (session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
            }
        } catch (IOException ex) {
            log.warn("Failed to close websocket session, sessionId={}", session.getId(), ex);
        }
    }

    /**
     * 前端通过已经建立的WebSocket发送json文本
     * @ OnMessage 这个注解是java WebSocket规范中定义的一个注解
     *              当客户端通过WebSocket协议发送消息，传输数据时，该方法自动被调用。
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        String userId = SESSION_USER_MAP.get(session.getId());
        log.info("Received text message, userId={}, message={}", userId, message);

        try {
            WebSocketMessage wsMessage = JSON.parseObject(message, WebSocketMessage.class);
            handleControlMessage(session, userId, wsMessage);
        } catch (Exception ex) {
            sendMessage(session, createResponse("info", "Received text message: " + message, null));
        }
    }

    /**
     * 生产者端：WebSocket接收音频写入管道
     * 前端通过WebSocket发送二进制音频帧
     * PCM 16kHz 16bit单声道原始音频
     */
    @OnMessage
    public void onMessage(Session session, ByteBuffer byteBuffer) {
        String sessionId = session.getId();
        String userId = SESSION_USER_MAP.get(sessionId);
        log.debug("Received audio chunk, userId={}, sessionId={}, bytes={}",
                userId, sessionId, byteBuffer.remaining());

        try {
            byte[] audioData = new byte[byteBuffer.remaining()];
            byteBuffer.get(audioData);

            TranscriptionSessionContext context = TRANSCRIPTION_CONTEXTS.get(sessionId);
            if (context == null || !context.active.get()) {
                log.warn("Audio chunk received before transcription session started, userId={}, sessionId={}",
                        userId, sessionId);
                sendMessage(session, createResponse("error",
                        "Transcription session is not started. Send start_transcription first.", null));
                return;
            }

            context.audioOutputStream.write(audioData);
            context.audioOutputStream.flush();
        } catch (Exception ex) {
            log.error("Failed to process audio chunk, userId={}, sessionId={}", userId, sessionId, ex);
            sendMessage(session, createResponse("error", "Failed to process audio chunk: " + ex.getMessage(), null));
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        String sessionId = session.getId();
        String userId = SESSION_USER_MAP.get(sessionId);

        stopTranscriptionSession(sessionId);
        cancelHeartbeat(sessionId);

        if (userId != null) {
            USER_SESSIONS.remove(userId);
            SESSION_USER_MAP.remove(sessionId);
        }
        String reason = closeReason != null ? closeReason.getReasonPhrase() : "unknown";
        log.info("WebSocket closed, userId={}, sessionId={}, reason={}",
                userId, sessionId, reason);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        String sessionId = session != null ? session.getId() : null;
        String userId = sessionId != null ? SESSION_USER_MAP.get(sessionId) : null;
        log.error("WebSocket error, userId={}, sessionId={}", userId, sessionId, error);

        if (sessionId != null) {
            stopTranscriptionSession(sessionId);
            cancelHeartbeat(sessionId);
        }
        sendMessage(session, createResponse("error", "WebSocket error: " + error.getMessage(), null));
    }

    /**
     * 根据客户端指令中的type类型路由不同的服务。
     */
    private void handleControlMessage(Session session, String userId, WebSocketMessage message) {
        String type = message != null ? message.getType() : null;
        if (type == null) {
            sendMessage(session, createResponse("unknown_command", "Missing command type", null));
            return;
        }

        switch (type) {
            case "ping" -> sendMessage(session, createResponse("pong", "pong", String.valueOf(System.currentTimeMillis())));
            // 启动实时音频转录
            case "start_transcription" -> startTranscriptionSession(session, userId);
            case "stop_transcription" -> {
                // 停止实时音频转录
                boolean stopped = stopTranscriptionSession(session.getId());
                if (stopped) {
                    // 向客户端WebSocket发送消息，前端监听WebSocket的onmessage事件，处理消息。
                    sendMessage(session, createResponse("transcription_stopped", "Transcription stopped", null));
                } else {
                    sendMessage(session, createResponse("transcription_already_stopped",
                            "Transcription is already stopped", null));
                }
            }
            case "get_status" -> sendMessage(session, createResponse("status", "Connection is healthy", userId));
            default -> sendMessage(session, createResponse("unknown_command", "Unknown command: " + type, null));
        }
    }

    private void startHeartbeat(Session session) {
        if (heartbeatExecutor == null) {
            log.warn("scheduledExecutorService is not injected, skip heartbeat, sessionId={}", session.getId());
            return;
        }
        String sessionId = session.getId();
        ScheduledFuture<?> oldTask = HEARTBEAT_TASKS.remove(sessionId);
        if (oldTask != null) {
            oldTask.cancel(true);
        }

        ScheduledFuture<?> task = heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (session.isOpen()) {
                sendMessage(session, createResponse("heartbeat", "heartbeat", String.valueOf(System.currentTimeMillis())));
            }
        }, 30, 30, TimeUnit.SECONDS);
        HEARTBEAT_TASKS.put(sessionId, task);
    }

    private void cancelHeartbeat(String sessionId) {
        ScheduledFuture<?> task = HEARTBEAT_TASKS.remove(sessionId);
        if (task != null) {
            task.cancel(true);
        }
    }

    /**
     * 启动实时音频转录
     * WebSocket是并发的，同一个session可能收到多个请求指令
     * 两个线程都创建了自己的Context，导致重复创建，相互覆盖，冲突。
     */
    private void startTranscriptionSession(Session session, String userId) {
        String sessionId = session.getId();
        // 从concurrentMap中以sessionId为key查询实时音频转录会话上下文，如果已经存在，说明开启成功，直接返回。
        TranscriptionSessionContext existing = TRANSCRIPTION_CONTEXTS.get(sessionId);
        if (existing != null && existing.active.get() && !existing.stopRequested.get()) {
            sendMessage(session, createResponse("transcription_already_started",
                    "Transcription is already started", null));
            return;
        }
        // 停止当前会话转录:开启新的之前，把之前的转录资源停止并释放。防止残留资源造成内存冲突或泄露
        stopTranscriptionSession(sessionId);
        // 创建新的转录会话
        TranscriptionSessionContext context = createAndStartTranscriptionSession(session, userId);
        if (context != null) {
            // putIfAbsent：原子性放入Map，防止并发下
            TranscriptionSessionContext raced = TRANSCRIPTION_CONTEXTS.putIfAbsent(sessionId, context);
            if (raced != null && raced.active.get() && !raced.stopRequested.get()) {
                context.active.set(false);
                context.stopRequested.set(true);
                closeQuietly(context.audioOutputStream);
                closeQuietly(context.audioInputStream);
                sendMessage(session, createResponse("transcription_already_started",
                        "Transcription is already started", null));
                return;
            }
            TRANSCRIPTION_CONTEXTS.put(sessionId, context);
            sendMessage(session, createResponse("transcription_started", "Transcription started", null));
        } else {
            sendMessage(session, createResponse("error", "Failed to start transcription", null));
        }
    }

    /**
     * 创建管道并启动推流
     * 后台创建一个持续运行的转录会话
     */
    private TranscriptionSessionContext createAndStartTranscriptionSession(Session session, String userId) {
        String sessionId = session.getId();
        try {
            if (xunfeiAudioService == null) {
                log.error("XunfeiAudioService is not injected yet, cannot start transcription. sessionId={}", sessionId);
                return null;
            }
            PipedInputStream audioInputStream = new PipedInputStream(64 * 1024);
            PipedOutputStream audioOutputStream = new PipedOutputStream(audioInputStream);
            AtomicBoolean active = new AtomicBoolean(true);
            TranscriptionSessionContext context = new TranscriptionSessionContext(audioInputStream, audioOutputStream, active);

            // 创建一个异步任务调用讯飞实时音频转文字方法。
            CompletableFuture<String> future = xunfeiAudioService.realTimeAudioToText(audioInputStream, update ->
                    {
                        context.lastUpdate.set(update);
                        sendMessage(session, createResponse("transcription", "Partial snapshot", update, true));
                    }
            );

            future.whenComplete((finalResult, throwable) -> {
                if (throwable != null && !isExpectedStopException(context, throwable)) {
                    log.error("Transcription failed, userId={}, sessionId={}", userId, sessionId, throwable);
                    sendMessage(session, createResponse("error", "Transcription failed: " + throwable.getMessage(), null));
                } else {
                    log.info("Transcription finished, userId={}, sessionId={}", userId, sessionId);
                    if (!context.stopRequested.get() && finalResult != null) {
                        sendMessage(session, createResponse("final", "Transcription completed",
                                buildFinalUpdate(finalResult, context.lastUpdate.get()), true));
                    }
                }
                cleanupTranscriptionContext(sessionId, context);
            });
            return context;
        } catch (Exception ex) {
            log.error("Failed to create transcription session, userId={}, sessionId={}", userId, sessionId, ex);
            return null;
        }
    }

    private boolean stopTranscriptionSession(String sessionId) {
        TranscriptionSessionContext context = TRANSCRIPTION_CONTEXTS.remove(sessionId);
        if (context == null) {
            return false;
        }
        context.active.set(false);
        context.stopRequested.set(true);
        closeQuietly(context.audioOutputStream);
        return true;
    }

    private void cleanupTranscriptionContext(String sessionId, TranscriptionSessionContext context) {
        TRANSCRIPTION_CONTEXTS.remove(sessionId, context);
        context.active.set(false);
        closeQuietly(context.audioOutputStream);
        closeQuietly(context.audioInputStream);
    }

    private boolean isExpectedStopException(TranscriptionSessionContext context, Throwable throwable) {
        if (!context.stopRequested.get()) {
            return false;
        }
        Throwable cursor = throwable;
        while (cursor != null) {
            String msg = cursor.getMessage();
            if (msg != null && (msg.contains("Pipe closed") || msg.contains("Stream closed"))) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    /**
     * 安全的关闭Closeable对象
     */
    private void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // no-op
        }
    }

    private void sendMessage(Session session, String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException ex) {
                log.error("Failed to send message, sessionId={}", session.getId(), ex);
            }
        }
    }

    public static void sendMessageToUser(String userId, String type, String message, String data) {
        Session session = USER_SESSIONS.get(userId);
        if (session == null || !session.isOpen()) {
            log.warn("User is offline, userId={}", userId);
            return;
        }
        try {
            session.getBasicRemote().sendText(createStaticResponse(type, message, data));
        } catch (IOException ex) {
            log.error("Failed to send message to user, userId={}", userId, ex);
        }
    }

    public static void broadcastMessage(String type, String message, String data) {
        String payload = createStaticResponse(type, message, data);
        USER_SESSIONS.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(payload);
                } catch (IOException ex) {
                    log.error("Broadcast failed, userId={}", userId, ex);
                }
            }
        });
    }

    public static Set<String> getOnlineUsers() {
        return USER_SESSIONS.keySet();
    }

    public static boolean isUserOnline(String userId) {
        Session session = USER_SESSIONS.get(userId);
        return session != null && session.isOpen();
    }

    private String createResponse(String type, String message, String data) {
        return createResponse(type, message, data, false);
    }

    private String createResponse(String type, String message, String data, boolean isSnapshot) {
        WebSocketResponse response = new WebSocketResponse();
        response.setType(type);
        response.setMessage(message);
        response.setData(data);
        response.setFullText(resolveFullText(type, data));
        response.setIsSnapshot(isSnapshot);
        response.setUpdateAction(resolveUpdateAction(type));
        response.setTimestamp(System.currentTimeMillis());
        return JSON.toJSONString(response);
    }

    private String createResponse(String type,
                                  String message,
                                  RealtimeTranscriptionUpdate update,
                                  boolean isSnapshot) {
        WebSocketResponse response = new WebSocketResponse();
        response.setType(type);
        response.setMessage(message);
        response.setData(update != null ? update.fullText() : null);
        response.setFullText(update != null ? update.fullText() : null);
        response.setDisplayText(update != null ? update.displayText() : null);
        response.setCommittedText(update != null ? update.committedText() : null);
        response.setLiveText(update != null ? update.liveText() : null);
        response.setRevision(update != null ? update.revision() : null);
        response.setResultStatus(update != null ? update.resultStatus() : null);
        response.setIsSnapshot(isSnapshot);
        response.setUpdateAction(resolveUpdateAction(type));
        response.setTimestamp(System.currentTimeMillis());
        if (update != null) {
            response.setSegmentId(update.segmentId());
            response.setSentenceSeq(update.segmentId());
            response.setSegmentText(update.segmentText());
            response.setPgs(update.pgs());
            response.setRg(update.rg());
            response.setBg(update.bg());
            response.setEd(update.ed());
            response.setIsFinalPacket(update.finalPacket());
        }
        return JSON.toJSONString(response);
    }

    private RealtimeTranscriptionUpdate buildFinalUpdate(String finalResult,
                                                         RealtimeTranscriptionUpdate lastUpdate) {
        if (lastUpdate == null) {
            return new RealtimeTranscriptionUpdate(
                    finalResult,
                    finalResult,
                    "",
                    finalResult,
                    1,
                    "final",
                    0,
                    finalResult,
                    null,
                    null,
                    null,
                    null,
                    true
            );
        }
        return new RealtimeTranscriptionUpdate(
                finalResult,
                finalResult,
                "",
                finalResult,
                lastUpdate.revision() != null ? lastUpdate.revision() + 1 : 1,
                "final",
                lastUpdate.segmentId(),
                lastUpdate.segmentText(),
                lastUpdate.pgs(),
                lastUpdate.rg(),
                lastUpdate.bg(),
                lastUpdate.ed(),
                true
        );
    }

    private static String createStaticResponse(String type, String message, String data) {
        WebSocketResponse response = new WebSocketResponse();
        response.setType(type);
        response.setMessage(message);
        response.setData(data);
        response.setFullText(resolveFullText(type, data));
        response.setIsSnapshot(false);
        response.setUpdateAction(resolveUpdateAction(type));
        response.setTimestamp(System.currentTimeMillis());
        return JSON.toJSONString(response);
    }

    private static String resolveFullText(String type, String data) {
        if ("transcription".equals(type) || "final".equals(type)) {
            return data;
        }
        return null;
    }

    private static String resolveUpdateAction(String type) {
        if ("transcription".equals(type)) {
            return "replace";
        }
        if ("final".equals(type)) {
            return "archive";
        }
        return "none";
    }

    @Data
    public static class WebSocketResponse {
        private String type;
        private String message;
        private String data;
        private String fullText;
        private String displayText;
        private String committedText;
        private String liveText;
        private Integer revision;
        private String resultStatus;
        private Boolean isSnapshot;
        private String updateAction;
        private Long timestamp;
        private Integer segmentId;
        private Integer sentenceSeq;
        private String segmentText;
        private String pgs;
        private int[] rg;
        private Integer bg;
        private Integer ed;
        private Boolean isFinalPacket;
    }

    @Data
    public static class WebSocketMessage {
        private String type;
    }

    /**
     * 会话上下文
     */
    private static class TranscriptionSessionContext {
        private final PipedInputStream audioInputStream;  //消费端：讯飞推流线程读取
        private final PipedOutputStream audioOutputStream;  // 生产端：WebSocket接收线程写入
        private final AtomicBoolean active;
        private final AtomicBoolean stopRequested = new AtomicBoolean(false);
        private final AtomicReference<RealtimeTranscriptionUpdate> lastUpdate = new AtomicReference<>();

        private TranscriptionSessionContext(PipedInputStream audioInputStream,
                                            PipedOutputStream audioOutputStream,
                                            AtomicBoolean active) {
            this.audioInputStream = audioInputStream;
            this.audioOutputStream = audioOutputStream;
            this.active = active;
        }
    }
}
