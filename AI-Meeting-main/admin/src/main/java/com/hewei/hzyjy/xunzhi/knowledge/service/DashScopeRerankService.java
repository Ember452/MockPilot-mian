package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.hewei.hzyjy.xunzhi.knowledge.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 阿里云百炼 DashScope Rerank（gte-rerank-v2）精排实现。
 * 失败/超时直接抛异常，由调用方降级 cosine（fail-open）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashScopeRerankService implements RerankService {

    private final RagProperties ragProperties;

    private volatile RestClient cachedRestClient;

    private RestClient getRestClient() {
        if (cachedRestClient == null) {
            synchronized (this) {
                if (cachedRestClient == null) {
                    RagProperties.Rerank config = ragProperties.getRerank();
                    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                    requestFactory.setConnectTimeout(config.getTimeoutMs());
                    requestFactory.setReadTimeout(config.getTimeoutMs());
                    cachedRestClient = RestClient.builder()
                            .requestFactory(requestFactory)
                            .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                            .defaultHeader("Content-Type", "application/json")
                            .build();
                }
            }
        }
        return cachedRestClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> rerank(String query, List<Float> queryEmbedding,
                                            List<Map<String, Object>> candidates, int topN) {
        RagProperties.Rerank config = ragProperties.getRerank();
        List<String> documents = candidates.stream()
                .map(c -> Objects.toString(c.get("content"), ""))
                .toList();

        Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "input", Map.of("query", query, "documents", documents),
                "parameters", Map.of("top_n", topN, "return_documents", false)
        );

        Map<String, Object> response = getRestClient().post()
                .uri(config.getBaseUrl())
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("output") instanceof Map<?, ?> output)
                || !(output.get("results") instanceof List<?> results) || results.isEmpty()) {
            throw new IllegalStateException("DashScope rerank response missing output.results");
        }

        List<Map<String, Object>> reranked = new ArrayList<>();
        for (Object item : results) {
            Map<String, Object> result = (Map<String, Object>) item;
            int index = ((Number) result.get("index")).intValue();
            double relevanceScore = ((Number) result.get("relevance_score")).doubleValue();
            if (index < 0 || index >= candidates.size()) {
                continue;
            }
            Map<String, Object> candidate = candidates.get(index);
            candidate.put("_rerank_score", relevanceScore);
            candidate.put("_rerank_provider", "dashscope");
            reranked.add(candidate);
        }

        reranked.sort(Comparator.<Map<String, Object>, Double>comparing(
                m -> ((Number) m.getOrDefault("_rerank_score", 0.0)).doubleValue()
        ).reversed());

        return reranked.subList(0, Math.min(topN, reranked.size()));
    }
}
