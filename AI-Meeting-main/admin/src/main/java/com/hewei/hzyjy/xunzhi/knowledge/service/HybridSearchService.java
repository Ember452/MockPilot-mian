package com.hewei.hzyjy.xunzhi.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private static final double RRF_K = 60.0;

    private final ElasticsearchVectorStore vectorStore;
    private final EmbeddingService embeddingService;

    public List<Map<String, Object>> search(Long kbId, String query, int topK, int rerankTopN) {
        List<Float> queryEmbedding = embeddingService.embed(query);

        List<Map<String, Object>> esResults = vectorStore.hybridSearch(kbId, query, queryEmbedding, topK * 2);

        if (esResults.isEmpty()) {
            return esResults;
        }

        List<Map<String, Object>> reranked = rerankBySimilarity(esResults, queryEmbedding, rerankTopN);

        reranked.sort((a, b) -> {
            double scoreA = ((Number) a.getOrDefault("_score", 0.0)).doubleValue();
            double scoreB = ((Number) b.getOrDefault("_score", 0.0)).doubleValue();
            return Double.compare(scoreB, scoreA);
        });

        return reranked;
    }

    public List<Map<String, Object>> search(Long kbId, String query, int topK) {
        return search(kbId, query, topK, Math.min(topK, 3));
    }

    private List<Map<String, Object>> rerankBySimilarity(
            List<Map<String, Object>> candidates,
            List<Float> queryEmbedding,
            int topN) {

        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            Object embObj = candidate.get("embedding");
            if (embObj instanceof List<?> embList && !embList.isEmpty()) {
                List<Float> docEmbedding = new ArrayList<>();
                for (Object val : embList) {
                    if (val instanceof Number num) {
                        docEmbedding.add(num.floatValue());
                    }
                }
                if (!docEmbedding.isEmpty()) {
                    double similarity = cosineSimilarity(queryEmbedding, docEmbedding);
                    double esScore = ((Number) candidate.getOrDefault("_score", 0.0)).doubleValue();
                    double rrfScore = 1.0 / (RRF_K + esScore + 1);
                    double finalScore = 0.7 * similarity + 0.3 * rrfScore;
                    candidate.put("_rerank_score", finalScore);
                    candidate.put("_similarity", similarity);
                    scored.add(candidate);
                }
            }
        }

        scored.sort(Comparator.<Map<String, Object>, Double>comparing(
                m -> ((Number) m.getOrDefault("_rerank_score", 0.0)).doubleValue()
        ).reversed());

        return scored.subList(0, Math.min(topN, scored.size()));
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
