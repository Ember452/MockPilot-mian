package com.hewei.hzyjy.xunzhi.knowledge.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端标准 RRF 融合纯函数测试。
 */
class HybridSearchServiceTest {

    private static Map<String, Object> hit(String chunkId) {
        Map<String, Object> map = new HashMap<>();
        map.put("chunk_id", chunkId);
        return map;
    }

    @Test
    void bothRoutesHitScoreIsSumOfRankContributions() {
        // c1 在 BM25 排第 1、kNN 排第 2；c2 反之
        List<Map<String, Object>> bm25 = List.of(hit("c1"), hit("c2"));
        List<Map<String, Object>> knn = List.of(hit("c2"), hit("c1"));

        List<Map<String, Object>> fused = HybridSearchService.fuseByRrf(bm25, knn, 10);

        assertEquals(2, fused.size());
        double expected = 1.0 / 61 + 1.0 / 62;
        assertEquals(expected, (Double) fused.get(0).get("_rrf_score"), 1e-9);
        assertEquals(expected, (Double) fused.get(1).get("_rrf_score"), 1e-9);
    }

    @Test
    void doubleRouteHitRanksAboveSingleRouteHit() {
        // c1 双路命中（各排第 2），c2 仅 BM25 第 1
        List<Map<String, Object>> bm25 = List.of(hit("c2"), hit("c1"));
        List<Map<String, Object>> knn = List.of(hit("c3"), hit("c1"));

        List<Map<String, Object>> fused = HybridSearchService.fuseByRrf(bm25, knn, 10);

        assertEquals("c1", fused.get(0).get("chunk_id"));
        double c1Score = (Double) fused.get(0).get("_rrf_score");
        assertEquals(2.0 / 62, c1Score, 1e-9);
    }

    @Test
    void singleRouteOrderPreserved() {
        List<Map<String, Object>> bm25 = List.of(hit("a"), hit("b"), hit("c"));

        List<Map<String, Object>> fused = HybridSearchService.fuseByRrf(bm25, List.of(), 10);

        assertEquals(List.of("a", "b", "c"),
                fused.stream().map(m -> m.get("chunk_id")).toList());
        assertTrue((Double) fused.get(0).get("_rrf_score") > (Double) fused.get(1).get("_rrf_score"));
    }

    @Test
    void limitTruncatesResult() {
        List<Map<String, Object>> bm25 = List.of(hit("a"), hit("b"), hit("c"), hit("d"));

        List<Map<String, Object>> fused = HybridSearchService.fuseByRrf(bm25, List.of(), 2);

        assertEquals(2, fused.size());
        assertEquals("a", fused.get(0).get("chunk_id"));
    }

    @Test
    void emptyAndNullRoutesReturnEmpty() {
        assertTrue(HybridSearchService.fuseByRrf(List.of(), List.of(), 10).isEmpty());
        assertTrue(HybridSearchService.fuseByRrf(null, null, 10).isEmpty());
    }
}
