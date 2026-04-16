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
}
