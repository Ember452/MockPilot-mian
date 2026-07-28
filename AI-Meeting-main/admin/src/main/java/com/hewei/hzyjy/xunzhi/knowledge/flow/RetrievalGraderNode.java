package com.hewei.hzyjy.xunzhi.knowledge.flow;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.knowledge.config.RagProperties;
import com.hewei.hzyjy.xunzhi.knowledge.service.QueryRewriteService;
import com.hewei.hzyjy.xunzhi.knowledge.service.RagMetricsRecorder;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * CRAG 检索质量评估节点：规则先行（DashScope relevance_score 阈值），
 * 不合格时可选轻量 LLM 复判，最终判定不合格才触发联网降级。
 * 阈值仅对 DashScope 分数有效；cosine 降级态分数分布不同，跳过评估直接放行（fail-open）。
 */
@Slf4j
@LiteflowComponent("retrievalGrader")
@RequiredArgsConstructor
public class RetrievalGraderNode extends NodeComponent {

    private static final String GRADER_SYSTEM_PROMPT = """
            你是检索质量评估助手。判断给出的参考片段能否回答用户问题。
            只输出 yes 或 no：能回答输出 yes，不能回答输出 no。""";

    private final RagProperties ragProperties;
    private final QueryRewriteService queryRewriteService;
    private final RagMetricsRecorder ragMetricsRecorder;

    /** 规则判定结果 */
    enum RuleDecision {
        /** 合格，跳过联网 */
        PASS,
        /** 不合格，触发联网降级 */
        WEB_SEARCH,
        /** 分数不足，交由 LLM 复判 */
        LLM_CHECK
    }

    @Override
    public void process() throws Exception {
        RagContext ctx = this.getContextBean(RagContext.class);
        long start = System.currentTimeMillis();
        try {
            doProcess(ctx);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            ctx.getStageTimings().put("retrievalGrader", elapsed);
            ragMetricsRecorder.recordStage("retrievalGrader", elapsed);
        }
    }

    private void doProcess(RagContext ctx) {
        double threshold = ragProperties.getRuleEngine().getGradePassThreshold();
        boolean llmGraderEnabled = Boolean.TRUE.equals(ragProperties.getRuleEngine().getEnableLlmGrader());

        RuleDecision decision = gradeByRule(ctx.getRetrievedChunks(), ctx.isRerankDegraded(), threshold, llmGraderEnabled);
        if (decision == RuleDecision.LLM_CHECK) {
            decision = gradeByLlm(ctx) ? RuleDecision.PASS : RuleDecision.WEB_SEARCH;
        }

        ctx.setNeedWebSearch(decision == RuleDecision.WEB_SEARCH);
        ragMetricsRecorder.recordGrader(ctx.isNeedWebSearch() ? "web_search" : "pass");
        log.info("Retrieval grading: kbId={}, degraded={}, needWebSearch={}",
                ctx.getKbId(), ctx.isRerankDegraded(), ctx.isNeedWebSearch());
    }

    /**
     * 规则判定（静态纯函数，供单测）：
     * 空结果→联网；cosine 降级/分数缺失→放行；top1 达标→放行；不足→LLM 复判或联网。
     */
    static RuleDecision gradeByRule(List<Map<String, Object>> chunks,
                                    boolean rerankDegraded,
                                    double threshold,
                                    boolean llmGraderEnabled) {
        if (chunks == null || chunks.isEmpty()) {
            return RuleDecision.WEB_SEARCH;
        }
        if (rerankDegraded) {
            return RuleDecision.PASS;
        }
        Object score = chunks.get(0).get("_rerank_score");
        if (!(score instanceof Number top1Score)) {
            // 分数缺失时阈值语义不成立，fail-open 放行
            return RuleDecision.PASS;
        }
        if (top1Score.doubleValue() >= threshold) {
            return RuleDecision.PASS;
        }
        return llmGraderEnabled ? RuleDecision.LLM_CHECK : RuleDecision.WEB_SEARCH;
    }

    /**
     * LLM 复判分块与问题相关性；调用失败/超时放行（fail-open）。
     */
    private boolean gradeByLlm(RagContext ctx) {
        try {
            String query = ctx.getRewrittenQuery() != null ? ctx.getRewrittenQuery() : ctx.getQuery();
            StringBuilder snippets = new StringBuilder();
            for (Map<String, Object> chunk : ctx.getRetrievedChunks()) {
                String content = String.valueOf(chunk.getOrDefault("content", ""));
                snippets.append(content, 0, Math.min(content.length(), 300)).append("\n---\n");
            }
            String answer = queryRewriteService.complete(List.of(
                    Map.of("role", "system", "content", GRADER_SYSTEM_PROMPT),
                    Map.of("role", "user", "content", "【用户问题】\n" + query + "\n\n【参考片段】\n" + snippets)
            ));
            if (StrUtil.isBlank(answer)) {
                return true;
            }
            return !answer.trim().toLowerCase().startsWith("no");
        } catch (Exception e) {
            log.warn("LLM grader failed, pass through: {}", e.getMessage());
            return true;
        }
    }
}
