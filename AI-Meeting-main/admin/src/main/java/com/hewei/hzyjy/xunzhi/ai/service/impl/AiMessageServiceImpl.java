package com.hewei.hzyjy.xunzhi.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiMessageReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.ai.service.AiConversationService;
import com.hewei.hzyjy.xunzhi.ai.service.AiMessageService;
import com.hewei.hzyjy.xunzhi.ai.service.AiPropertiesService;
import com.hewei.hzyjy.xunzhi.ai.service.chat.AiChatHandler;
import com.hewei.hzyjy.xunzhi.ai.service.chat.AiChatHandlerFactory;
import com.hewei.hzyjy.xunzhi.auth.application.CurrentUserService;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.conversation.application.ConversationMessageHistoryService;
import com.hewei.hzyjy.xunzhi.conversation.application.ConversationMessagePersistenceService;
import com.hewei.hzyjy.xunzhi.conversation.application.ConversationStreamingSupport;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AIContentAccumulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageServiceImpl implements AiMessageService {

    private static final String DEFAULT_ERROR_CONTENT = "Sorry, an error occurred while processing your request.";
    private static final String UNSUPPORTED_AI_TYPE = "Current AI type is not supported";

    private final AiPropertiesService aiPropertiesService;
    private final AiConversationService aiConversationService;
    private final CurrentUserService currentUserService;
    private final AiChatHandlerFactory aiChatHandlerFactory;
    private final ConversationMessageHistoryService conversationMessageHistoryService;
    private final ConversationMessagePersistenceService conversationMessagePersistenceService;
    private final ConversationStreamingSupport conversationStreamingSupport;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public Flux<String> aiChatFlux(AiMessageReqDTO requestParam) {
        String username = requestParam == null ? null : requestParam.getUserName();
        return aiChatFlux(requestParam, username);
    }

    @Override
    public Flux<String> aiChatFlux(AiMessageReqDTO requestParam, String username) {
        if (requestParam == null) {
            return Flux.error(new ClientException("request body cannot be empty"));
        }
        if (StrUtil.isBlank(requestParam.getSessionId())) {
            return Flux.error(new ClientException("sessionId cannot be empty"));
        }
        if (StrUtil.isBlank(username)) {
            return Flux.error(new ClientException("username cannot be empty"));
        }
        aiConversationService.requireOwnedConversation(requestParam.getSessionId(), username);
        requestParam.setUserName(username);
        return aiChatFluxInternal(requestParam);
    }

    private Flux<String> aiChatFluxInternal(AiMessageReqDTO requestParam) {
        if (requestParam == null) {
            return Flux.error(new ClientException("request body cannot be empty"));
        }

        String sessionId = requestParam.getSessionId();
        if (StrUtil.isBlank(sessionId)) {
            return Flux.error(new ClientException("sessionId cannot be empty"));
        }

        return Flux.create(sink -> {
            String userMessage = StrUtil.blankToDefault(requestParam.getInputMessage(), "No input");
            Long aiId = requestParam.getAiId();
            AIContentAccumulator accumulator = new AIContentAccumulator();

            threadPoolTaskExecutor.submit(() -> processChat(sessionId, aiId, userMessage, sink, accumulator));
            sink.onCancel(() -> log.warn("AI chat flux cancelled, sessionId={}", sessionId));
            sink.onDispose(() -> log.info("AI chat flux disposed, sessionId={}", sessionId));
        });
    }

    private void processChat(
            String sessionId,
            Long aiId,
            String userMessage,
            FluxSink<String> sink,
            AIContentAccumulator accumulator) {
        conversationStreamingSupport.execute(ConversationStreamingSupport.ConversationStreamRequest
                .<AiMessageHistoryRespDTO>builder()
                .sessionId(sessionId)
                .defaultErrorContent(DEFAULT_ERROR_CONTENT)
                .accumulator(accumulator)
                .historySupplier(() -> conversationMessageHistoryService.listAiHistory(sessionId))
                .userMessageSaver(() -> conversationMessagePersistenceService.saveAiUserMessage(sessionId, userMessage))
                .streamExecutor((historyMessages, contentAccumulator) -> {
                    AiPropertiesDO aiProperties = resolveAiProperties(aiId);
                    AiChatHandler handler = aiChatHandlerFactory.getHandler(aiProperties.getAiType());
                    if (handler == null) {
                        sendUnsupportedSink(sink, contentAccumulator);
                        return;
                    }
                    handler.streamToSink(aiProperties, userMessage, historyMessages, sink, contentAccumulator);
                })
                .assistantMessageSaver(payload -> conversationMessagePersistenceService.saveAiAssistantMessage(
                        sessionId,
                        payload.content(),
                        payload.reasoningContent(),
                        payload.responseTime(),
                        payload.errorMessage()))
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
    }

    private AiPropertiesDO resolveAiProperties(Long aiId) {
        AiPropertiesDO aiProperties;
        if (aiId == null) {
            aiProperties = aiPropertiesService.getDefaultDoubaoConfig();
            if (aiProperties == null) {
                throw new ClientException("Default AI config does not exist");
            }
        } else {
            aiProperties = aiPropertiesService.getById(aiId);
            if (aiProperties == null || aiProperties.getDelFlag() == 1 || aiProperties.getIsEnabled() == 0) {
                throw new ClientException("AI config does not exist or is disabled");
            }
        }
        return aiProperties;
    }

    @Override
    public List<AiMessageHistoryRespDTO> getConversationHistory(String sessionId) {
        return conversationMessageHistoryService.listAiHistory(sessionId);
    }

    @Override
    public List<AiMessageHistoryRespDTO> getConversationHistory(String sessionId, String username) {
        aiConversationService.requireOwnedConversation(sessionId, username);
        return getConversationHistory(sessionId);
    }

    @Override
    public IPage<AiMessageHistoryRespDTO> pageHistoryMessages(String sessionId, Integer current, Integer size) {
        if (StrUtil.isNotBlank(sessionId)) {
            return conversationMessageHistoryService.pageAiHistory(sessionId, current, size);
        }
        return conversationMessageHistoryService.pageAllAiHistory(current, size);
    }

    @Override
    public IPage<AiMessageHistoryRespDTO> pageHistoryMessages(
            String sessionId,
            Integer current,
            Integer size,
            String username) {
        if (StrUtil.isNotBlank(sessionId)) {
            aiConversationService.requireOwnedConversation(sessionId, username);
            return pageHistoryMessages(sessionId, current, size);
        }

        List<String> ownedSessionIds = aiConversationService.listOwnedSessionIds(username);
        if (CollUtil.isEmpty(ownedSessionIds)) {
            Page<AiMessageHistoryRespDTO> emptyPage = new Page<>(current, size);
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }
        return conversationMessageHistoryService.pageAiHistory(ownedSessionIds, current, size);
    }

    @Override
    public Long getUserIdByUsername(String username) {
        return currentUserService.getUserIdByUsername(username);
    }

    private void sendUnsupportedSink(FluxSink<String> sink, AIContentAccumulator accumulator) {
        if (!sink.isCancelled()) {
            sink.next(UNSUPPORTED_AI_TYPE);
        }
        accumulator.appendSimpleContent(UNSUPPORTED_AI_TYPE);
    }
}
