package com.hewei.hzyjy.xunzhi.knowledge.api;

import com.alibaba.fastjson2.JSON;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiChatStreamRespDTO;
import com.hewei.hzyjy.xunzhi.ai.service.AiConversationService;
import com.hewei.hzyjy.xunzhi.common.convention.annotation.CurrentUser;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.conversation.application.ConversationMessageHistoryService;
import com.hewei.hzyjy.xunzhi.conversation.application.ConversationMessagePersistenceService;
import com.hewei.hzyjy.xunzhi.conversation.application.ConversationStreamingSupport;
import com.hewei.hzyjy.xunzhi.knowledge.api.io.req.KnowledgeChatReqDTO;
import com.hewei.hzyjy.xunzhi.knowledge.service.RagChatService;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AIContentAccumulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/xunzhi/v1/knowledge-chat")
@RequiredArgsConstructor
public class KnowledgeChatController {

    private static final String DEFAULT_ERROR_CONTENT = "抱歉，处理请求时出现错误。";

    private final RagChatService ragChatService;
    private final AiConversationService aiConversationService;
    private final ConversationMessageHistoryService conversationMessageHistoryService;
    private final ConversationMessagePersistenceService conversationMessagePersistenceService;
    private final ConversationStreamingSupport conversationStreamingSupport;

    @PostMapping(value = "/sessions/{sessionId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@PathVariable String sessionId,
                             @RequestBody KnowledgeChatReqDTO requestParam,
                             @CurrentUser String username,
                             jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        if (requestParam.getKbId() == null) {
            return Flux.error(new ClientException("knowledge base id is required"));
        }

        requestParam.setSessionId(sessionId);
        requestParam.setUserName(username);

        return Flux.create(sink -> {
            String userMessage = requestParam.getInputMessage() != null
                    ? requestParam.getInputMessage() : "No input";
            Long kbId = requestParam.getKbId();
            Long aiId = requestParam.getAiId();
            AIContentAccumulator accumulator = new AIContentAccumulator();
            // 捕获本轮引用来源，流结束后随助手消息持久化
            java.util.concurrent.atomic.AtomicReference<java.util.List<java.util.Map<String, Object>>> referencesRef =
                    new java.util.concurrent.atomic.AtomicReference<>();

            conversationStreamingSupport.execute(ConversationStreamingSupport.ConversationStreamRequest
                    .<com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiMessageHistoryRespDTO>builder()
                    .sessionId(sessionId)
                    .defaultErrorContent(DEFAULT_ERROR_CONTENT)
                    .accumulator(accumulator)
                    .historySupplier(() -> conversationMessageHistoryService.listAiHistory(sessionId))
                    .userMessageSaver(() -> conversationMessagePersistenceService.saveAiUserMessage(sessionId, userMessage))
                    .streamExecutor((historyMessages, contentAccumulator) -> {
                        referencesRef.set(ragChatService.executeRagChat(
                                sessionId, userMessage, kbId, aiId, username, historyMessages, sink, contentAccumulator));
                    })
                    .assistantMessageSaver(payload -> conversationMessagePersistenceService.saveAiAssistantMessage(
                            sessionId,
                            Objects.toString(payload.content(), ""),
                            null,
                            payload.responseTime(),
                            payload.errorMessage(),
                            referencesRef.get()))
                    .conversationUpdater(messageSeq -> aiConversationService.updateConversation(sessionId, messageSeq, null))
                    .successHandler(() -> {
                        if (!sink.isCancelled()) {
                            sink.complete();
                        }
                    })
                    .errorHandler(ex -> {
                        if (!sink.isCancelled()) {
                            sink.next(DEFAULT_ERROR_CONTENT);
                            sink.error(ex);
                        }
                    })
                    .build());
        });
    }
}
