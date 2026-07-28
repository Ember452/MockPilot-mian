package com.hewei.hzyjy.xunzhi.knowledge.flow;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.knowledge.config.RagProperties;
import com.hewei.hzyjy.xunzhi.knowledge.service.QueryRewriteService;
import com.hewei.hzyjy.xunzhi.knowledge.service.RagMetricsRecorder;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@LiteflowComponent("queryRewrite")
@RequiredArgsConstructor
public class QueryRewriteNode extends NodeComponent {

    /**
     * 常见指代词：query 命中任一则视为依赖上下文，需改写
     */
    private static final List<String> ANAPHORA_WORDS =
            List.of("它", "这", "那", "上面", "刚才", "前面", "上述", "之前", "该", "其");

    private static final int SELF_CONTAINED_MIN_LENGTH = 10;

    /**
     * 改写仅携带最近 3 轮历史（用户+助手共 6 条）
     */
    private static final int MAX_HISTORY_MESSAGES = 6;

    private final RagProperties ragProperties;
    private final QueryRewriteService queryRewriteService;
    private final RagMetricsRecorder ragMetricsRecorder;

    @Override
    public void process() throws Exception {
        RagContext ctx = this.getContextBean(RagContext.class);
        long start = System.currentTimeMillis();
        try {
            doProcess(ctx);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            ctx.getStageTimings().put("queryRewrite", elapsed);
            ragMetricsRecorder.recordStage("queryRewrite", elapsed);
        }
    }

    private void doProcess(RagContext ctx) {
        if (!Boolean.TRUE.equals(ragProperties.getRuleEngine().getEnableQueryRewrite())) {
            ragMetricsRecorder.recordRewrite("skipped");
            return;
        }
        if (CollUtil.isEmpty(ctx.getHistoryMessages())) {
            ragMetricsRecorder.recordRewrite("skipped");
            return;
        }
        String query = ctx.getQuery();
        if (isSelfContained(query)) {
            log.debug("Query rewrite skipped by heuristic: query_length={}", query.length());
            ragMetricsRecorder.recordRewrite("skipped");
            return;
        }

        try {
            List<com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiMessageHistoryRespDTO> history = ctx.getHistoryMessages();
            List<com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiMessageHistoryRespDTO> recentHistory =
                    history.size() > MAX_HISTORY_MESSAGES
                            ? history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size())
                            : history;
            String rewritten = queryRewriteService.rewrite(query, recentHistory);
            if (StrUtil.isNotBlank(rewritten) && !rewritten.equals(query)) {
                ctx.setRewrittenQuery(rewritten);
                log.info("Query rewritten: original_length={}, rewritten={}", query.length(), rewritten);
                ragMetricsRecorder.recordRewrite("applied");
            } else {
                ragMetricsRecorder.recordRewrite("skipped");
            }
        } catch (Exception e) {
            log.warn("Query rewrite node failed, fail-open with original query: {}", e.getMessage());
            ragMetricsRecorder.recordRewrite("failed");
        }
    }

    /**
     * 启发式判定自包含问题：不含指代词且长度达标 → 跳过改写
     */
    static boolean isSelfContained(String query) {
        if (query == null || query.length() < SELF_CONTAINED_MIN_LENGTH) {
            return false;
        }
        for (String word : ANAPHORA_WORDS) {
            if (query.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
