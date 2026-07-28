package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String defaultBaseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String defaultApiKey;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-v4}")
    private String embeddingModel;

    private static final String EMBEDDINGS_PATH = "/embeddings";

    private volatile RestClient cachedRestClient;

    private RestClient getRestClient() {
        if (cachedRestClient == null) {
            synchronized (this) {
                if (cachedRestClient == null) {
                    cachedRestClient = RestClient.builder()
                            .baseUrl(defaultBaseUrl)
                            .defaultHeader("Authorization", "Bearer " + defaultApiKey)
                            .defaultHeader("Content-Type", "application/json")
                            .build();
                }
            }
        }
        return cachedRestClient;
    }

    public EmbeddingService() {
    }

    public List<Float> embed(String text) {
        String apiKey = defaultApiKey;

        if (apiKey == null || apiKey.isBlank()) {
            throw new ClientException("Embedding API Key not configured");
        }

        String url = defaultBaseUrl + EMBEDDINGS_PATH;
        Map<String, Object> requestBody = Map.of(
                "model", embeddingModel,
                "input", text,
                "encoding_format", "float"
        );

        try {
            Map<String, Object> response = getRestClient().post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("data")) {
                throw new ClientException("Embedding response missing data field");
            }

            List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
            if (dataList == null || dataList.isEmpty()) {
                throw new ClientException("Embedding response data list empty");
            }

            List<Double> rawEmbedding = (List<Double>) dataList.get(0).get("embedding");

            return rawEmbedding.stream()
                    .map(Double::floatValue)
                    .toList();

        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding request failed", e);
            throw new ClientException("Embedding request failed: " + e.getMessage());
        }
    }

    public int getEmbeddingDimension() {
        return 1536;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    /**
     * embedding 模型兼容性校验（static 纯函数供单测）：
     * 不同模型的向量不可混用，kb 未绑定模型（legacy 空串）或与当前一致时放行，
     * 不一致时抛 ClientException（上传入口直接报错，检索入口由上层 fail-open 降级）。
     */
    public static void validateEmbeddingCompatibility(String kbModel, String currentModel) {
        if (kbModel == null || kbModel.isBlank()) {
            return;
        }
        if (kbModel.equals(currentModel)) {
            return;
        }
        throw new ClientException(String.format(
                "知识库绑定的embedding模型(%s)与当前配置(%s)不一致，向量不可混用，请恢复模型配置或重建知识库",
                kbModel, currentModel));
    }
}
