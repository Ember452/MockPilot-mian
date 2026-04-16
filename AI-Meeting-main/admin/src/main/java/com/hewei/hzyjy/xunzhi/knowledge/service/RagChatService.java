package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.alibaba.fastjson2.JSON;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiChatStreamRespDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.ai.service.chat.AiChatHandler;
import com.hewei.hzyjy.xunzhi.ai.service.chat.AiChatHandlerFactory;
import com.hewei.hzyjy.xunzhi.ai.service.AiPropertiesService;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.knowledge.flow.RagContext;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AIContentAccumulator;
import com.yomahub.liteflow.core.FlowExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.FluxSink;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatService {

    private final FlowExecutor flowExecutor;
    private final AiPropertiesService aiPropertiesService;
    private final AiChatHandlerFactory aiChatHandlerFactory;

    public void executeRagChat(
            String sessionId,
            String userMessage,
            Long kbId,
            Long aiId,
            List<AiMessageHistoryRespDTO> historyMessages,
            FluxSink<String> sink,
            AIContentAccumulator accumulator) {

        RagContext ragCtx = new RagContext()
                .setSessionId(sessionId)
                .setQuery(userMessage)
                .setKbId(kbId);

        try {
            flowExecutor.execute2Resp("default_rag_chain", null, ragCtx);
        } catch (Exception e) {
            log.error("LiteFlow RAG chain execution failed", e);
            sink.next("RAG 检索过程出错，将使用通用对话模式回复。");
        }

        String augmentedPrompt = buildAugmentedPrompt(userMessage, ragCtx.getCompressedContext());

        AiPropertiesDO aiProperties = resolveAiProperties(aiId);
        AiChatHandler handler = aiChatHandlerFactory.getHandler(aiProperties.getAiType());
        if (handler == null) {
            sink.next("当前 AI 类型不支持");
            sink.complete();
            return;
        }

        List<AiMessageHistoryRespDTO> augmentedHistory = buildAugmentedHistory(historyMessages, userMessage);

        try {
            handler.streamToSink(aiProperties, augmentedPrompt, augmentedHistory, sink, accumulator);
        } catch (Exception e) {
            log.error("RAG chat stream failed", e);
            sink.error(e);
        }
    }

    private String buildAugmentedPrompt(String userMessage, String context) {
        if (context == null || context.isBlank()) {
            return userMessage;
        }
        return String.format("""
                你是一位知识渊博的AI助手，请基于以下参考资料回答用户问题。
                回答要求：
                1. 优先使用参考资料中的信息
                2. 如果参考资料不足以回答，请明确说明并借鉴你的知识
                3. 回答末尾可附上参考来源编号

                【参考资料】
                %s

                【用户问题】
                %s""", context, userMessage);
    }

    private List<AiMessageHistoryRespDTO> buildAugmentedHistory(
            List<AiMessageHistoryRespDTO> historyMessages,
            String userMessage) {
        String systemPrompt = "你是讯智AI助手，请根据对话历史和参考资料，提供准确、有用的回答。";
        AiMessageHistoryRespDTO systemMsg = new AiMessageHistoryRespDTO();
        systemMsg.setMessageType(2);
        systemMsg.setMessageContent(systemPrompt);

        if (historyMessages != null && !historyMessages.isEmpty()) {
            // 始终在历史前注入系统提示词
            List<AiMessageHistoryRespDTO> augmented = new java.util.ArrayList<>();
            augmented.add(systemMsg);
            augmented.addAll(historyMessages);
            return augmented;
        }
        return List.of(systemMsg);
    }

    private AiPropertiesDO resolveAiProperties(Long aiId) {
        AiPropertiesDO aiProperties;
        if (aiId == null) {
            aiProperties = aiPropertiesService.getDefaultDoubaoConfig();
            if (aiProperties == null) {
                throw new ClientException("默认 AI 配置不存在");
            }
        } else {
            aiProperties = aiPropertiesService.getById(aiId);
            if (aiProperties == null || aiProperties.getDelFlag() == 1 || aiProperties.getIsEnabled() == 0) {
                throw new ClientException("AI 配置不存在或已禁用");
            }
        }
        return aiProperties;
    }
}
