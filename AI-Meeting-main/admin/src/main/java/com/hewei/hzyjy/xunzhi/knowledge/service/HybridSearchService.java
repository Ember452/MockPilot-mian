package com.hewei.hzyjy.xunzhi.knowledge.service;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.knowledge.config.RagProperties;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeBaseDO;
import com.hewei.hzyjy.xunzhi.knowledge.dao.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private static final double RRF_K = 60.0;

    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final RagProperties ragProperties;
    private final DashScopeRerankService dashScopeRerankService;
    private final CosineRerankFallback cosineRerankFallback;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public List<Map<String, Object>> search(Long kbId, String query, int topK, int rerankTopN) {
        // embedding 模型不一致时抛异常，由上层既有 fail-open 捕获降级为普通对话
        KnowledgeBaseDO kb = knowledgeBaseMapper.selectById(kbId);
        if (kb != null) {
            EmbeddingService.validateEmbeddingCompatibility(
                    kb.getEmbeddingModel(), embeddingService.getEmbeddingModel());
        }

        List<Float> queryEmbedding = embeddingService.embed(query);

        int candidateSize = topK * 2;
        VectorStore.DualRecallResult recall =
                vectorStore.dualRecall(kbId, query, queryEmbedding, candidateSize);

        List<Map<String, Object>> fused = fuseByRrf(recall.bm25Hits(), recall.knnHits(), candidateSize);
        if (fused.isEmpty()) {
            return fused;
        }

        return rerank(query, queryEmbedding, fused, rerankTopN);
    }

    public List<Map<String, Object>> search(Long kbId, String query, int topK) {
        return search(kbId, query, topK, Math.min(topK, 3));
    }

    /**
     * 客户端标准 RRF：按 chunk_id 合并双路结果，以各自排名（从 1 起）计算融合分
     * score = Σ 1/(RRF_K + rank_i)，降序取 limit。
     */
    static List<Map<String, Object>> fuseByRrf(List<Map<String, Object>> bm25Hits,
                                               List<Map<String, Object>> knnHits,
                                               int limit) {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        Map<String, Double> rrfScores = new LinkedHashMap<>();

        accumulateRoute(bm25Hits, merged, rrfScores);
        accumulateRoute(knnHits, merged, rrfScores);

        List<Map<String, Object>> fused = new ArrayList<>();
        rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .forEach(entry -> {
                    Map<String, Object> candidate = merged.get(entry.getKey());
                    candidate.put("_rrf_score", entry.getValue());
                    fused.add(candidate);
                });
        return fused;
    }

    private static void accumulateRoute(List<Map<String, Object>> hits,
                                        Map<String, Map<String, Object>> merged,
                                        Map<String, Double> rrfScores) {
        if (hits == null) {
            return;
        }
        int rank = 1;
        for (Map<String, Object> hit : hits) {
            String chunkId = Objects.toString(hit.get("chunk_id"), null);
            if (chunkId == null) {
                continue;
            }
            merged.putIfAbsent(chunkId, hit);
            rrfScores.merge(chunkId, 1.0 / (RRF_K + rank), Double::sum);
            rank++;
        }
    }

    private List<Map<String, Object>> rerank(String query, List<Float> queryEmbedding,
                                             List<Map<String, Object>> candidates, int topN) {
        RagProperties.Rerank config = ragProperties.getRerank();
        boolean dashScopeEnabled = "dashscope".equalsIgnoreCase(config.getProvider())
                && StrUtil.isNotBlank(config.getApiKey());

        if (dashScopeEnabled) {
            try {
                return dashScopeRerankService.rerank(query, queryEmbedding, candidates, topN);
            } catch (Exception e) {
                log.warn("DashScope rerank failed, fallback to cosine: {}", e.getMessage());
            }
        }
        return cosineRerankFallback.rerank(query, queryEmbedding, candidates, topN);
    }
}
