package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.alibaba.fastjson2.JSON;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiChatStreamRespDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.ai.service.chat.AiChatHandler;
import com.hewei.hzyjy.xunzhi.ai.service.chat.AiChatHandlerFactory;
import com.hewei.hzyjy.xunzhi.ai.service.AiPropertiesService;
import com.hewei.hzyjy.xunzhi.ai.service.UserModelPreferenceService;
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
    private final UserModelPreferenceService userModelPreferenceService;
    private final AiChatHandlerFactory aiChatHandlerFactory;
    private final RagProperties ragProperties;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final RagTraceService ragTraceService;

    /**
     * 链路入口
     * @return 本轮推送给前端的引用来源列表（联网降级时为空），供调用方随助手消息持久化
     */
    public List<Map<String, Object>> executeRagChat(
            String sessionId,
            String userMessage,
            Long kbId,  // 知识库 ID
            Long aiId,  // AI 模型 ID
            String username,
            List<AiMessageHistoryRespDTO> historyMessages, // 历史消息
            // 一个可以把数据推送到响应式流Flux的管道口
            FluxSink<String> sink,
            AIContentAccumulator accumulator) {

        RagContext ragCtx = new RagContext()
                .setSessionId(sessionId)
                .setQuery(userMessage)
                .setKbId(kbId)
                .setHistoryMessages(historyMessages);
        //----------------------------------RAG检索链路
        // 进行检索增强生成
        try {
            // 执行RAG检索链路，进行查询改写，检索重排，上下文压缩
            flowExecutor.execute2Resp("default_rag_chain", null, ragCtx);
        } catch (Exception e) {
            log.error("LiteFlow RAG chain execution failed", e);
            sink.next("RAG 检索过程出错，将使用通用对话模式回复。");
        }
        //---------------------------引用构建，用户可以先看到引用的参考材料
        // 流式输出前先推送结构化引用来源事件（旧前端对未知 type 静默忽略）
        // 判断是否触发了联网搜索，用于区分参考资料来源
        boolean webSearchTriggered = ragCtx.getWebSearchResult() != null && !ragCtx.getWebSearchResult().isBlank();
        // 构建引用来源
        List<Map<String, Object>> references = buildReferences(ragCtx.getRetrievedChunks(), webSearchTriggered);
        ragCtx.setReferences(references);
        try {
            // 构建SSE事件推送到前端
            Map<String, Object> referencesEvent = new HashMap<>();
            referencesEvent.put("type", "references");
            referencesEvent.put("data", references);
            sink.next(JSON.toJSONString(referencesEvent));
        } catch (Exception e) {
            log.warn("Push references event failed: {}", e.getMessage());
        }

        // 构建检索增强后的Prompt，优先使用知识库的专属模版，没有使用默认模版
        String augmentedPrompt = buildAugmentedPrompt(userMessage, ragCtx.getCompressedContext(), kbId);

        // AI 模型配置类
        AiPropertiesDO aiProperties = resolveAiProperties(aiId, username);
        // 获取AI模型对应的处理器
        AiChatHandler handler = aiChatHandlerFactory.getHandler(aiProperties.getAiType());
        if (handler == null) {
            sink.next("当前 AI 类型不支持");
            // 通知订阅者数据流发送完毕，可以关闭连接，前端收到后释放资源停止等待
            sink.complete();   // 结束
            // 记录链路明细，留档
            recordTrace(ragCtx, username, accumulator);
            return references;
        }

        // 构建包含系统提示词的历史消息列表
        List<AiMessageHistoryRespDTO> augmentedHistory = buildAugmentedHistory(historyMessages, userMessage);

        try {
            // 构建并流式输出对话
            handler.streamToSink(aiProperties, augmentedPrompt, augmentedHistory, sink, accumulator);
        } catch (Exception e) {
            log.error("RAG chat stream failed", e);
            sink.error(e);
        }
        // 流式结束后 token 已累积完整，异步留档本轮链路明细
        recordTrace(ragCtx, username, accumulator);
        return references;
    }

    /**
     * 记录链路明细
     * @param ragCtx RAG 链路上下文
     * @param username  用户名字
     * @param accumulator 累计的 token 数量
     */
    private void recordTrace(RagContext ragCtx, String username, AIContentAccumulator accumulator) {
        try {
            ragTraceService.record(ragCtx, username, accumulator.getTotalTokens(), accumulator.isTokenEstimated());
        } catch (Exception e) {
            log.warn("Record rag trace failed: {}", e.getMessage());
        }
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
     * 优先使用自定义的知识库专属模版，没有就用默认模版
     * ----
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

    /**
     * 构建包含系统提示词的历史消息列表，为AI对话提供角色设定和上下文背景
     * @param historyMessages 历史消息
     * @param userMessage 用户消息
     * @return 构建后的历史消息列表
     * 如果有历史消息，这系统提示词作为第一个历史消息，用户消息作为后面的历史消息。没有返回提示词
     * --------------
     * 确保AI在开始对话时，明确自己定位和规范，同时保留完整对话供RAG用
     */
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

    /**
     * 解析用户使用哪个模型
     * 优先使用指定模型，否则使用用户绑定的功能默认模型，再回退平台默认
     */
    private AiPropertiesDO resolveAiProperties(Long aiId, String username) {
        AiPropertiesDO aiProperties;
        if (aiId == null) {
            // 未显式指定：优先用户绑定的功能默认模型，再回退平台默认
            aiProperties = userModelPreferenceService.resolvePreferred(username, UserModelPreferenceService.FEATURE_KB_CHAT);
            if (aiProperties == null) {
                aiProperties = aiPropertiesService.getDefaultDoubaoConfig();
            }
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
