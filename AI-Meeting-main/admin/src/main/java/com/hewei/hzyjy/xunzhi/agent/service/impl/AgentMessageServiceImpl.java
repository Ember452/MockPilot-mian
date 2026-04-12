package com.hewei.hzyjy.xunzhi.agent.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.agent.application.AgentResolver;
import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentPropertiesDO;
import com.hewei.hzyjy.xunzhi.agent.service.AgentConversationService;
import com.hewei.hzyjy.xunzhi.agent.service.AgentMessageService;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.common.enums.AgentErrorCodeEnum;
import com.hewei.hzyjy.xunzhi.conversation.application.ConversationMessageHistoryService;
import com.hewei.hzyjy.xunzhi.conversation.application.ConversationMessagePersistenceService;
import com.hewei.hzyjy.xunzhi.conversation.application.ConversationOwnershipService;
import com.hewei.hzyjy.xunzhi.conversation.application.ConversationStreamingSupport;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AIContentAccumulator;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AgentPropertiesLoader;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.XingChenAIClient;
import com.hewei.hzyjy.xunzhi.user.api.io.req.UserMessageReqDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentMessageServiceImpl implements AgentMessageService {

    private static final String DEFAULT_ERROR_CONTENT = "Sorry, an error occurred while processing your request.";

    private final AgentResolver agentResolver;
    private final XingChenAIClient xingChenAIClient;
    private final AgentPropertiesLoader agentPropertiesLoader;
    private final AgentConversationService agentConversationService;
    private final ConversationOwnershipService conversationOwnershipService;
    private final ConversationMessageHistoryService conversationMessageHistoryService;
    private final ConversationMessagePersistenceService conversationMessagePersistenceService;
    private final ConversationStreamingSupport conversationStreamingSupport;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public List<AgentMessageHistoryRespDTO> getConversationHistory(String sessionId) {
        return conversationMessageHistoryService.listAgentHistory(sessionId);
    }

    @Override
    public List<AgentMessageHistoryRespDTO> getConversationHistory(String sessionId, Long userId) {
        conversationOwnershipService.requireOwnedConversation(sessionId, userId);
        return getConversationHistory(sessionId);
    }

    @Override
    public IPage<AgentMessageHistoryRespDTO> pageHistoryMessages(String sessionId, Integer current, Integer size) {
        if (StrUtil.isNotBlank(sessionId)) {
            return conversationMessageHistoryService.pageAgentHistory(sessionId, current, size);
        }
        return conversationMessageHistoryService.pageAllAgentHistory(current, size);
    }

    @Override
    public IPage<AgentMessageHistoryRespDTO> pageHistoryMessages(
            String sessionId,
            Integer current,
            Integer size,
            Long userId) {
        if (StrUtil.isNotBlank(sessionId)) {
            conversationOwnershipService.requireOwnedConversation(sessionId, userId);
            return pageHistoryMessages(sessionId, current, size);
        }

        List<String> sessionIds = conversationOwnershipService.listOwnedSessionIds(userId);
        if (CollUtil.isEmpty(sessionIds)) {
            Page<AgentMessageHistoryRespDTO> emptyPage = new Page<>(current, size);
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }
        return conversationMessageHistoryService.pageAgentHistory(sessionIds, current, size);
    }

    @Override
    public SseEmitter agentChatSse(UserMessageReqDTO requestParam) {
        String sessionId = requestParam.getSessionId();
        if (StrUtil.isBlank(sessionId)) {
            throw new ClientException("sessionId cannot be empty");
        }

        SseEmitter emitter = new SseEmitter(18000L);
        Long agentId = agentResolver.resolveAgentId(sessionId, null);
        if (agentId == null) {
            throw new ClientException(AgentErrorCodeEnum.Agent_NULL);
        }
        String userMessage = requestParam.getInputMessage() == null ? "No input" : requestParam.getInputMessage();
        AIContentAccumulator accumulator = new AIContentAccumulator();

        threadPoolTaskExecutor.submit(() -> processChat(sessionId, agentId, userMessage, emitter, accumulator));
        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    @Override
    public SseEmitter agentChatSse(UserMessageReqDTO requestParam, Long userId) {
        String sessionId = requestParam != null ? requestParam.getSessionId() : null;
        conversationOwnershipService.requireOwnedConversation(sessionId, userId);
        return agentChatSse(requestParam);
    }

    private void processChat(
            String sessionId,
            Long agentId,
            String userMessage,
            SseEmitter emitter,
            AIContentAccumulator accumulator) {
        conversationStreamingSupport.execute(ConversationStreamingSupport.ConversationStreamRequest
                .<AgentMessageHistoryRespDTO>builder()
                .sessionId(sessionId)
                .defaultErrorContent(DEFAULT_ERROR_CONTENT)
                .accumulator(accumulator)
                .historySupplier(() -> conversationMessageHistoryService.listAgentHistory(sessionId))
                .userMessageSaver(() -> conversationMessagePersistenceService.saveAgentUserMessage(sessionId, userMessage))
                .streamExecutor((historyMessages, contentAccumulator) -> {
                    AgentPropertiesDO agentProperties = agentPropertiesLoader.getByAgentId(agentId);
                    if (agentProperties == null) {
                        throw new ClientException(AgentErrorCodeEnum.Agent_NULL);
                    }
                    xingChenAIClient.chat(
                            userMessage,
                            sessionId,
                            buildHistoryJson(historyMessages),
                            true,
                            new OutputStream() {
                                @Override
                                public void write(int b) {
                                }

                                @Override
                                public void write(byte[] b, int off, int len) throws IOException {
                                    String jsonChunk = new String(b, off, len);
                                    emitter.send(SseEmitter.event().data(jsonChunk));
                                    contentAccumulator.appendChunk(b);
                                }

                                @Override
                                public void flush() {
                                }
                            },
                            data -> {
                            },
                            agentProperties.getApiKey(),
                            agentProperties.getApiSecret(),
                            agentProperties.getApiFlowId()
                    );
                })
                .assistantMessageSaver(payload -> conversationMessagePersistenceService.saveAgentAssistantMessage(
                        sessionId,
                        payload.content(),
                        payload.responseTime(),
                        payload.errorMessage()))
                .conversationUpdater(messageSeq -> agentConversationService.updateConversation(sessionId, messageSeq, null))
                .successHandler(() -> {
                    try {
                        emitter.send(SseEmitter.event().name("end").data("[DONE]"));
                        emitter.complete();
                    } catch (IOException ex) {
                        log.warn("Failed to send agent stream completion event, sessionId={}", sessionId, ex);
                        emitter.completeWithError(ex);
                    }
                })
                .errorHandler(emitter::completeWithError)
                .build());
    }

    private String buildHistoryJson(List<AgentMessageHistoryRespDTO> historyMessages) {
        List<HashMap<String, String>> historyList = new ArrayList<>();
        if (CollUtil.isNotEmpty(historyMessages)) {
            historyList = historyMessages.stream().map(history -> {
                HashMap<String, String> item = new HashMap<>();
                item.put("role", history.getMessageType() == 1 ? "user" : "assistant");
                item.put("content_type", "text");
                item.put("content", history.getMessageContent());
                return item;
            }).collect(Collectors.toList());
        }
        return JSON.toJSONString(historyList);
    }
}
