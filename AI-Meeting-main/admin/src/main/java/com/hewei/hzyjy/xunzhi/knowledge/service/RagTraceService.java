package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.hewei.hzyjy.xunzhi.knowledge.config.RagProperties;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.RagTrace;
import com.hewei.hzyjy.xunzhi.knowledge.flow.RagContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * rag_trace 明细留档：每轮 RAG 对话结束后异步写一条，写失败仅告警——
 * 观测不得影响主链路。TTL 索引在启动时维护（auto-index-creation 未开启）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagTraceService {

    /** summary 统计窗口内最多加载的明细条数，防止大窗口拖垮内存 */
    private static final int SUMMARY_SAMPLE_LIMIT = 5000;

    private final MongoTemplate mongoTemplate;
    private final RagProperties ragProperties;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @PostConstruct
    void ensureIndexes() {
        try {
            var indexOps = mongoTemplate.indexOps(RagTrace.class);
            int ttlDays = ragProperties.getMetrics().getTraceTtlDays() == null
                    ? 30 : ragProperties.getMetrics().getTraceTtlDays();
            indexOps.ensureIndex(new Index().on("create_time", Sort.Direction.ASC)
                    .named("idx_create_time_ttl")
                    .expire(Duration.ofDays(ttlDays)));
            indexOps.ensureIndex(new Index().on("username", Sort.Direction.ASC).named("idx_username"));
            indexOps.ensureIndex(new Index().on("kb_id", Sort.Direction.ASC).named("idx_kb_id"));
        } catch (Exception e) {
            // TTL 参数变更时 ensureIndex 冲突也仅告警，不阻断启动
            log.warn("Ensure rag_trace indexes failed: {}", e.getMessage());
        }
    }

    /**
     * 异步留档一轮 RAG 调用；开关关闭时静默跳过。
     */
    public void record(RagContext ctx, String username, long tokenTotal, boolean tokenEstimated) {
        if (!Boolean.TRUE.equals(ragProperties.getMetrics().getTraceEnabled())) {
            return;
        }
        try {
            RagTrace trace = buildTrace(ctx, username, tokenTotal, tokenEstimated);
            threadPoolTaskExecutor.execute(() -> {
                try {
                    mongoTemplate.save(trace);
                } catch (Exception e) {
                    log.warn("Save rag_trace failed, sessionId={}: {}", trace.getSessionId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("Submit rag_trace task failed: {}", e.getMessage());
        }
    }

    /**
     * 由链路上下文推导 trace 字段（静态纯函数，供单测）。
     */
    static RagTrace buildTrace(RagContext ctx, String username, long tokenTotal, boolean tokenEstimated) {
        boolean webSearchTriggered = ctx.getWebSearchResult() != null && !ctx.getWebSearchResult().isBlank();
        String rerankProvider = null;
        if (ctx.getRetrievedChunks() != null && !ctx.getRetrievedChunks().isEmpty()) {
            Object provider = ctx.getRetrievedChunks().get(0).get("_rerank_provider");
            rerankProvider = provider == null ? "cosine" : String.valueOf(provider);
        }
        return new RagTrace()
                .setSessionId(ctx.getSessionId())
                .setKbId(ctx.getKbId())
                .setUsername(username)
                .setStageTimings(new LinkedHashMap<>(ctx.getStageTimings()))
                .setRewriteApplied(ctx.getRewrittenQuery() != null)
                .setRerankProvider(rerankProvider)
                .setGraderDecision(ctx.isNeedWebSearch() ? "web_search" : "pass")
                .setWebSearchTriggered(webSearchTriggered)
                .setReferenceCount(ctx.getReferences() == null ? 0 : ctx.getReferences().size())
                .setTokenTotal(tokenTotal)
                .setTokenEstimated(tokenEstimated)
                .setCreateTime(LocalDateTime.now());
    }

    /**
     * 汇总看板：窗口内明细在内存中聚合（窗口受 TTL 与采样上限约束）。
     */
    public Map<String, Object> summary(String username, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(Math.max(1, days));
        Query query = new Query(Criteria.where("username").is(username).and("create_time").gte(since))
                .with(Sort.by(Sort.Direction.DESC, "create_time"))
                .limit(SUMMARY_SAMPLE_LIMIT);
        List<RagTrace> traces = mongoTemplate.find(query, RagTrace.class);
        return summarize(traces);
    }

    /**
     * 聚合计算（静态纯函数，供单测）：调用量、阶段耗时 avg/p95、
     * 改写率、rerank 降级率、联网降级率、token 总量与估算占比、按日趋势。
     */
    static Map<String, Object> summarize(List<RagTrace> traces) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCalls", traces.size());

        // 阶段耗时聚合
        Map<String, List<Long>> stageSamples = new LinkedHashMap<>();
        for (RagTrace trace : traces) {
            if (trace.getStageTimings() == null) {
                continue;
            }
            trace.getStageTimings().forEach((stage, millis) ->
                    stageSamples.computeIfAbsent(stage, k -> new ArrayList<>()).add(millis));
        }
        Map<String, Object> stages = new LinkedHashMap<>();
        stageSamples.forEach((stage, samples) -> {
            samples.sort(Long::compareTo);
            long sum = samples.stream().mapToLong(Long::longValue).sum();
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("avgMs", samples.isEmpty() ? 0 : sum / samples.size());
            stat.put("p95Ms", samples.isEmpty() ? 0 : samples.get((int) Math.ceil(samples.size() * 0.95) - 1));
            stages.put(stage, stat);
        });
        result.put("stages", stages);

        long rewriteApplied = traces.stream().filter(RagTrace::isRewriteApplied).count();
        long rerankFallback = traces.stream()
                .filter(t -> t.getRerankProvider() != null && !"dashscope".equals(t.getRerankProvider()))
                .count();
        long webSearch = traces.stream().filter(RagTrace::isWebSearchTriggered).count();
        long tokenTotal = traces.stream().mapToLong(RagTrace::getTokenTotal).sum();
        long tokenEstimated = traces.stream().filter(RagTrace::isTokenEstimated).count();
        int total = traces.size();
        result.put("rewriteRate", rate(rewriteApplied, total));
        result.put("rerankFallbackRate", rate(rerankFallback, total));
        result.put("webSearchRate", rate(webSearch, total));
        result.put("tokenTotal", tokenTotal);
        result.put("tokenEstimatedRate", rate(tokenEstimated, total));

        // 按日趋势（date 升序）：调用量 + token
        Map<String, Map<String, Object>> daily = new java.util.TreeMap<>();
        for (RagTrace trace : traces) {
            if (trace.getCreateTime() == null) {
                continue;
            }
            String day = trace.getCreateTime().toLocalDate().toString();
            Map<String, Object> bucket = daily.computeIfAbsent(day, k -> {
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("date", k);
                b.put("calls", 0L);
                b.put("tokens", 0L);
                return b;
            });
            bucket.put("calls", (Long) bucket.get("calls") + 1);
            bucket.put("tokens", (Long) bucket.get("tokens") + trace.getTokenTotal());
        }
        result.put("daily", new ArrayList<>(daily.values()));
        return result;
    }

    private static double rate(long part, int total) {
        return total == 0 ? 0.0 : Math.round(part * 10000.0 / total) / 10000.0;
    }

    /**
     * 明细分页（排查用），按时间倒序。
     */
    public Map<String, Object> traces(String username, Long kbId, int page, int size) {
        Criteria criteria = Criteria.where("username").is(username);
        if (kbId != null) {
            criteria = criteria.and("kb_id").is(kbId);
        }
        int safeSize = Math.min(Math.max(1, size), 100);
        int safePage = Math.max(1, page);
        Query countQuery = new Query(criteria);
        long total = mongoTemplate.count(countQuery, RagTrace.class);
        Query pageQuery = new Query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "create_time"))
                .skip((long) (safePage - 1) * safeSize)
                .limit(safeSize);
        List<RagTrace> records = mongoTemplate.find(pageQuery, RagTrace.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("records", records);
        return result;
    }
}
