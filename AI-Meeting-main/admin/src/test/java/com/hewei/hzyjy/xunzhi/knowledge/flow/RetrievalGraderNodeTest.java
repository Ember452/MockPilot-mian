package com.hewei.hzyjy.xunzhi.knowledge.flow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.hewei.hzyjy.xunzhi.knowledge.flow.RetrievalGraderNode.RuleDecision;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CRAG 检索质量评估规则判定测试。
 */
class RetrievalGraderNodeTest {

    private static final double THRESHOLD = 0.5;

    @Test
    void emptyChunksTriggerWebSearch() {
        assertEquals(RuleDecision.WEB_SEARCH,
                RetrievalGraderNode.gradeByRule(List.of(), false, THRESHOLD, false));
        assertEquals(RuleDecision.WEB_SEARCH,
                RetrievalGraderNode.gradeByRule(null, false, THRESHOLD, false));
    }

    @Test
    void degradedRerankPassesThrough() {
        // cosine 降级态分数分布不同，跳过评估直接放行
        List<Map<String, Object>> chunks = List.of(Map.of("_rerank_score", 0.1));
        assertEquals(RuleDecision.PASS,
                RetrievalGraderNode.gradeByRule(chunks, true, THRESHOLD, false));
    }

    @Test
    void scoreAboveThresholdPasses() {
        List<Map<String, Object>> chunks = List.of(Map.of("_rerank_score", 0.8));
        assertEquals(RuleDecision.PASS,
                RetrievalGraderNode.gradeByRule(chunks, false, THRESHOLD, false));
    }

    @Test
    void scoreBelowThresholdTriggersWebSearchWhenLlmGraderDisabled() {
        List<Map<String, Object>> chunks = List.of(Map.of("_rerank_score", 0.2));
        assertEquals(RuleDecision.WEB_SEARCH,
                RetrievalGraderNode.gradeByRule(chunks, false, THRESHOLD, false));
    }

    @Test
    void scoreBelowThresholdDelegatesToLlmWhenEnabled() {
        List<Map<String, Object>> chunks = List.of(Map.of("_rerank_score", 0.2));
        assertEquals(RuleDecision.LLM_CHECK,
                RetrievalGraderNode.gradeByRule(chunks, false, THRESHOLD, true));
    }

    @Test
    void missingScorePassesThrough() {
        // 分数缺失时阈值语义不成立，fail-open
        List<Map<String, Object>> chunks = List.of(Map.of("chunk_id", "c1"));
        assertEquals(RuleDecision.PASS,
                RetrievalGraderNode.gradeByRule(chunks, false, THRESHOLD, false));
    }
}
