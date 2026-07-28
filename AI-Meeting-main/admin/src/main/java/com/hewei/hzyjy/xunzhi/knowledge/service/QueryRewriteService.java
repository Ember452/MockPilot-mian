package com.hewei.hzyjy.xunzhi.knowledge.service;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.knowledge.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 多轮对话查询改写：调用百炼 compatible-mode 轻量模型（非流式）把指代性提问
 * 改写为独立完整问题。失败/超时返回 null，由调用方 fail-open 回退原 query。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriteService {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private static final String REWRITE_SYSTEM_PROMPT = """
            你是查询改写助手。根据对话历史，将用户的最新问题改写为一个不依赖上下文、语义完整的独立问题。
            要求：只输出改写后的问题本身，不要任何解释、前缀或标点修饰；若问题本身已完整，原样输出。""";

    private final RagProperties ragProperties;

    @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    private volatile RestClient cachedRestClient;

    private RestClient getRestClient() {
        if (cachedRestClient == null) {
            synchronized (this) {
                if (cachedRestClient == null) {
                    int timeoutMs = ragProperties.getRuleEngine().getQueryRewriteTimeoutMs();
                    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                    requestFactory.setConnectTimeout(timeoutMs);
                    requestFactory.setReadTimeout(timeoutMs);
                    cachedRestClient = RestClient.builder()
                            .baseUrl(baseUrl)
                            .requestFactory(requestFactory)
                            .defaultHeader("Authorization", "Bearer " + apiKey)
                            .defaultHeader("Content-Type", "application/json")
                            .build();
                }
            }
        }
        return cachedRestClient;
    }

    /**
     * @return 改写后的独立问题；失败/超时/未配置 Key 返回 null
     */
    public String rewrite(String query, List<AiMessageHistoryRespDTO> historyMessages) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", REWRITE_SYSTEM_PROMPT));
        for (AiMessageHistoryRespDTO history : historyMessages) {
            String role = history.getMessageType() != null && history.getMessageType() == 1
                    ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", StrUtil.nullToEmpty(history.getMessageContent())));
        }
        messages.add(Map.of("role", "user", "content", "最新问题：" + query));
        return complete(messages);
    }

    /**
     * 通用轻量模型非流式调用（查询改写 / LLM grader 共用）。
     *
     * @return 模型输出文本；失败/超时/未配置 Key 返回 null
     */
    public String complete(List<Map<String, String>> messages) {
        if (StrUtil.isBlank(apiKey)) {
            return null;
        }
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", ragProperties.getRuleEngine().getQueryRewriteModel(),
                    "messages", messages,
                    "stream", false
            );

            Map<String, Object> response = getRestClient().post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !(response.get("choices") instanceof List<?> choices) || choices.isEmpty()) {
                return null;
            }
            Object message = ((Map<?, ?>) choices.get(0)).get("message");
            if (!(message instanceof Map<?, ?> messageMap)) {
                return null;
            }
            String content = StrUtil.trimToNull(String.valueOf(messageMap.get("content")));
            return "null".equals(content) ? null : content;
        } catch (Exception e) {
            log.warn("Lightweight LLM completion failed: {}", e.getMessage());
            return null;
        }
    }
}
