package com.hewei.hzyjy.xunzhi.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * cosine 相似度精排降级实现：DashScope Rerank 失败或未配置时使用。
 * 候选已不回传 embedding，按 chunk_id 用 mget 一次批量补查向量。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CosineRerankFallback implements RerankService {

    private final VectorStore vectorStore;

    @Override
    public List<Map<String, Object>> rerank(String query, List<Float> queryEmbedding,
                                            List<Map<String, Object>> candidates, int topN) {
        Long kbId = firstKbId(candidates);
        Map<String, List<Float>> fetched = fetchMissingEmbeddings(kbId, candidates);

        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            List<Float> docEmbedding = resolveEmbedding(candidate, fetched);
            if (docEmbedding.isEmpty()) {
                continue;
            }
            double similarity = cosineSimilarity(queryEmbedding, docEmbedding);
            candidate.put("_rerank_score", similarity);
            candidate.put("_similarity", similarity);
            candidate.put("_rerank_provider", "cosine");
            scored.add(candidate);
        }

        scored.sort(Comparator.<Map<String, Object>, Double>comparing(
                m -> ((Number) m.getOrDefault("_rerank_score", 0.0)).doubleValue()
        ).reversed());

        return scored.subList(0, Math.min(topN, scored.size()));
    }

    private Long firstKbId(List<Map<String, Object>> candidates) {
        for (Map<String, Object> candidate : candidates) {
            if (candidate.get("kb_id") instanceof Number num) {
                return num.longValue();
            }
        }
        return null;
    }

    private Map<String, List<Float>> fetchMissingEmbeddings(Long kbId, List<Map<String, Object>> candidates) {
        List<String> missingChunkIds = candidates.stream()
                .filter(c -> !(c.get("embedding") instanceof List<?> embList && !embList.isEmpty()))
                .map(c -> Objects.toString(c.get("chunk_id"), null))
                .filter(Objects::nonNull)
                .toList();
        if (kbId == null || missingChunkIds.isEmpty()) {
            return Map.of();
        }
        return vectorStore.fetchEmbeddings(kbId, missingChunkIds);
    }

    private List<Float> resolveEmbedding(Map<String, Object> candidate, Map<String, List<Float>> fetched) {
        if (candidate.get("embedding") instanceof List<?> embList && !embList.isEmpty()) {
            List<Float> docEmbedding = new ArrayList<>();
            for (Object val : embList) {
                if (val instanceof Number num) {
                    docEmbedding.add(num.floatValue());
                }
            }
            return docEmbedding;
        }
        String chunkId = Objects.toString(candidate.get("chunk_id"), null);
        List<Float> docEmbedding = chunkId != null ? fetched.get(chunkId) : null;
        return docEmbedding != null ? docEmbedding : List.of();
    }

    private double cosineSimilarity(List<Float> vecA, List<Float> vecB) {
        if (vecA.size() != vecB.size()) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.size(); i++) {
            dotProduct += vecA.get(i) * vecB.get(i);
            normA += vecA.get(i) * vecA.get(i);
            normB += vecB.get(i) * vecB.get(i);
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
