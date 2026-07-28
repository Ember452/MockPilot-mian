package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.alibaba.fastjson2.JSON;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiChatStreamRespDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.ai.service.chat.AiChatHandler;
import com.hewei.hzyjy.xunzhi.ai.service.chat.AiChatHandlerFactory;
import com.hewei.hzyjy.xunzhi.ai.service.AiPropertiesService;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.knowledge.config.RagProperties;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeBaseDO;
import com.hewei.hzyjy.xunzhi.knowledge.dao.mapper.KnowledgeBaseMapper;
import com.hewei.hzyjy.xunzhi.knowledge.flow.RagContext;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AIContentAccumulator;
import com.yomahub.liteflow.core.FlowExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatService {

    private final FlowExecutor flowExecutor;
    private final AiPropertiesService aiPropertiesService;
    private final AiChatHandlerFactory aiChatHandlerFactory;
    private final RagProperties ragProperties;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * @return 本轮推送给前端的引用来源列表（联网降级时为空），供调用方随助手消息持久化
     */
    public List<Map<String, Object>> executeRagChat(
            String sessionId,
            String userMessage,
            Long kbId,
            Long aiId,
            String username,
            List<AiMessageHistoryRespDTO> historyMessages,
            FluxSink<String> sink,
            AIContentAccumulator accumulator) {

        RagContext ragCtx = new RagContext()
                .setSessionId(sessionId)
                .setQuery(userMessage)
                .setKbId(kbId)
                .setHistoryMessages(historyMessages);

        try {
            flowExecutor.execute2Resp("default_rag_chain", null, ragCtx);
        } catch (Exception e) {
            log.error("LiteFlow RAG chain execution failed", e);
            sink.next("RAG 检索过程出错，将使用通用对话模式回复。");
        }

        // 流式输出前先推送结构化引用来源事件（旧前端对未知 type 静默忽略）
        boolean webSearchTriggered = ragCtx.getWebSearchResult() != null && !ragCtx.getWebSearchResult().isBlank();
        List<Map<String, Object>> references = buildReferences(ragCtx.getRetrievedChunks(), webSearchTriggered);
        ragCtx.setReferences(references);
        try {
            Map<String, Object> referencesEvent = new HashMap<>();
            referencesEvent.put("type", "references");
            referencesEvent.put("data", references);
            sink.next(JSON.toJSONString(referencesEvent));
        } catch (Exception e) {
            log.warn("Push references event failed: {}", e.getMessage());
        }

        String augmentedPrompt = buildAugmentedPrompt(userMessage, ragCtx.getCompressedContext(), kbId);

        AiPropertiesDO aiProperties = resolveAiProperties(aiId, username);
        AiChatHandler handler = aiChatHandlerFactory.getHandler(aiProperties.getAiType());
        if (handler == null) {
            sink.next("当前 AI 类型不支持");
            sink.complete();
            return references;
        }

        List<AiMessageHistoryRespDTO> augmentedHistory = buildAugmentedHistory(historyMessages, userMessage);

        try {
            handler.streamToSink(aiProperties, augmentedPrompt, augmentedHistory, sink, accumulator);
        } catch (Exception e) {
            log.error("RAG chat stream failed", e);
            sink.error(e);
        }
        return references;
    }

    /**
     * 构建引用来源：仅包含知识库检索命中；联网降级时返回空列表（网页溯源不在范围）。
     */
    static List<Map<String, Object>> buildReferences(List<Map<String, Object>> chunks, boolean webSearchTriggered) {
        if (webSearchTriggered || chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> references = new ArrayList<>(chunks.size());
        for (Map<String, Object> chunk : chunks) {
            String content = String.valueOf(chunk.getOrDefault("content", ""));
            Map<String, Object> reference = new LinkedHashMap<>();
            reference.put("fileName", chunk.getOrDefault("file_name", ""));
            reference.put("docId", chunk.getOrDefault("doc_id", ""));
            reference.put("chunkIndex", chunk.get("chunk_index"));
            reference.put("score", chunk.get("_rerank_score"));
            reference.put("snippet", content.length() > 200 ? content.substring(0, 200) : content);
            references.add(reference);
        }
        return references;
    }

    private String buildAugmentedPrompt(String userMessage, String context, Long kbId) {
        if (context == null || context.isBlank()) {
            return userMessage;
        }
        return renderTemplate(resolvePromptTemplate(kbId), context, userMessage);
    }

    /**
     * 模板优先级：knowledge_base.prompt_template（非空白）→ 全局配置默认模板。
     */
    private String resolvePromptTemplate(Long kbId) {
        try {
            if (kbId != null) {
                KnowledgeBaseDO kb = knowledgeBaseMapper.selectById(kbId);
                if (kb != null && kb.getPromptTemplate() != null && !kb.getPromptTemplate().isBlank()) {
                    return kb.getPromptTemplate();
                }
            }
        } catch (Exception e) {
            log.warn("Resolve kb prompt template failed, fallback to default: {}", e.getMessage());
        }
        return ragProperties.getPrompt().getRagTemplate();
    }

    /**
     * 占位符渲染（静态纯函数，供单测）：{context}/{question} 替换，不支持嵌套。
     */
    static String renderTemplate(String template, String context, String question) {
        return template
                .replace("{context}", context == null ? "" : context)
                .replace("{question}", question == null ? "" : question);
    }

    private List<AiMessageHistoryRespDTO> buildAugmentedHistory(
            List<AiMessageHistoryRespDTO> historyMessages,
            String userMessage) {
        String systemPrompt = ragProperties.getPrompt().getSystemPrompt();
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

    private AiPropertiesDO resolveAiProperties(Long aiId, String username) {
        AiPropertiesDO aiProperties;
        if (aiId == null) {
            aiProperties = aiPropertiesService.getDefaultDoubaoConfig();
            if (aiProperties == null) {
                throw new ClientException("默认 AI 配置不存在");
            }
        } else {
            // 含归属校验：公共或本人私有配置才可用
            aiProperties = aiPropertiesService.getUsableById(aiId, username);
        }
        return aiProperties;
    }
}
