package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.hewei.hzyjy.xunzhi.knowledge.config.RagProperties;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.RagTrace;
import com.hewei.hzyjy.xunzhi.knowledge.flow.RagContext;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * RagTraceService 单测：trace 字段推导、summary 内存聚合、留档开关。
 */
class RagTraceServiceTest {

    // ---------- buildTrace ----------

    @Test
    void buildTraceDerivesFieldsFromContext() {
        RagContext ctx = new RagContext()
                .setSessionId("s-1")
                .setKbId(7L)
                .setRewrittenQuery("改写后查询")
                .setRetrievedChunks(List.of(Map.of("_rerank_provider", "dashscope")))
                .setNeedWebSearch(true)
                .setWebSearchResult("网络搜索结果")
                .setReferences(List.of(Map.of("id", 1), Map.of("id", 2)));
        ctx.getStageTimings().put("queryRewrite", 10L);
        ctx.getStageTimings().put("ragRetrieval", 20L);
        ctx.getStageTimings().put("retrievalGrader", 5L);
        ctx.getStageTimings().put("webSearch", 30L);
        ctx.getStageTimings().put("contextCompression", 8L);

        RagTrace trace = RagTraceService.buildTrace(ctx, "alice", 123L, false);

        assertEquals("s-1", trace.getSessionId());
        assertEquals(7L, trace.getKbId());
        assertEquals("alice", trace.getUsername());
        assertTrue(trace.isRewriteApplied());
        assertEquals("dashscope", trace.getRerankProvider());
        assertEquals("web_search", trace.getGraderDecision());
        assertTrue(trace.isWebSearchTriggered());
        assertEquals(2, trace.getReferenceCount());
        assertEquals(123L, trace.getTokenTotal());
        assertFalse(trace.isTokenEstimated());
        // 五阶段齐全且保序
        assertEquals(List.of("queryRewrite", "ragRetrieval", "retrievalGrader", "webSearch", "contextCompression"),
                List.copyOf(trace.getStageTimings().keySet()));
    }

    @Test
    void buildTraceDefaultsWhenNoRewriteNoChunks() {
        RagContext ctx = new RagContext().setSessionId("s-2").setKbId(1L);

        RagTrace trace = RagTraceService.buildTrace(ctx, "bob", 10L, true);

        assertFalse(trace.isRewriteApplied());
        assertNull(trace.getRerankProvider());
        assertEquals("pass", trace.getGraderDecision());
        assertFalse(trace.isWebSearchTriggered());
        assertEquals(0, trace.getReferenceCount());
        assertTrue(trace.isTokenEstimated());
    }

    @Test
    void buildTraceRerankProviderFallsBackToCosine() {
        // chunk 无 _rerank_provider 标记 → 视为 cosine 降级
        RagContext ctx = new RagContext()
                .setRetrievedChunks(List.of(Map.of("content", "文本")));

        RagTrace trace = RagTraceService.buildTrace(ctx, "bob", 0L, true);

        assertEquals("cosine", trace.getRerankProvider());
    }

    // ---------- summarize ----------

    @Test
    void summarizeAggregatesRatesAndStages() {
        RagTrace t1 = new RagTrace().setRewriteApplied(true).setRerankProvider("dashscope")
                .setWebSearchTriggered(false).setTokenTotal(100L).setTokenEstimated(false)
                .setStageTimings(Map.of("ragRetrieval", 100L))
                .setCreateTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        RagTrace t2 = new RagTrace().setRewriteApplied(false).setRerankProvider("cosine")
                .setWebSearchTriggered(true).setTokenTotal(50L).setTokenEstimated(true)
                .setStageTimings(Map.of("ragRetrieval", 300L))
                .setCreateTime(LocalDateTime.of(2025, 1, 2, 10, 0));

        Map<String, Object> summary = RagTraceService.summarize(List.of(t1, t2));

        assertEquals(2, summary.get("totalCalls"));
        assertEquals(0.5, summary.get("rewriteRate"));
        assertEquals(0.5, summary.get("rerankFallbackRate"));
        assertEquals(0.5, summary.get("webSearchRate"));
        assertEquals(150L, summary.get("tokenTotal"));
        assertEquals(0.5, summary.get("tokenEstimatedRate"));

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> stages = (Map<String, Map<String, Object>>) summary.get("stages");
        assertEquals(200L, stages.get("ragRetrieval").get("avgMs"));
        assertEquals(300L, stages.get("ragRetrieval").get("p95Ms"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> daily = (List<Map<String, Object>>) summary.get("daily");
        assertEquals(2, daily.size());
        assertEquals("2025-01-01", daily.get(0).get("date"));
        assertEquals(1L, daily.get(0).get("calls"));
        assertEquals(100L, daily.get(0).get("tokens"));
    }

    @Test
    void summarizeEmptyTracesIsSafe() {
        Map<String, Object> summary = RagTraceService.summarize(List.of());

        assertEquals(0, summary.get("totalCalls"));
        assertEquals(0.0, summary.get("rewriteRate"));
        assertEquals(0L, summary.get("tokenTotal"));
        assertTrue(((Map<?, ?>) summary.get("stages")).isEmpty());
        assertTrue(((List<?>) summary.get("daily")).isEmpty());
    }

    // ---------- record 开关 ----------

    @Test
    void recordSkipsWhenTraceDisabled() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        RagProperties properties = new RagProperties();
        properties.getMetrics().setTraceEnabled(false);
        RagTraceService service = new RagTraceService(mongoTemplate, properties, executor);

        service.record(new RagContext(), "alice", 1L, false);

        verify(executor, never()).execute(any());
    }

    @Test
    void recordSubmitsAsyncTaskWhenEnabled() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        RagProperties properties = new RagProperties();
        RagTraceService service = new RagTraceService(mongoTemplate, properties, executor);

        service.record(new RagContext().setSessionId("s-9"), "alice", 1L, true);

        verify(executor).execute(any(Runnable.class));
    }
}
