package com.hewei.hzyjy.xunzhi.knowledge.flow;

import cn.hutool.core.collection.CollUtil;
import com.hewei.hzyjy.xunzhi.knowledge.service.RagMetricsRecorder;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@LiteflowComponent("ragRetrieval")
@RequiredArgsConstructor
public class RagRetrievalNode extends NodeComponent {

    private final com.hewei.hzyjy.xunzhi.knowledge.service.HybridSearchService hybridSearchService;
    private final RagMetricsRecorder ragMetricsRecorder;

    @Override
    public void process() throws Exception {
        // 从当前节点是历史上下文中得到数据
        RagContext ctx = this.getContextBean(RagContext.class);
        long start = System.currentTimeMillis();
        try {
            doProcess(ctx);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            ctx.getStageTimings().put("ragRetrieval", elapsed);
            ragMetricsRecorder.recordStage("ragRetrieval", elapsed);
        }
    }

    private void doProcess(RagContext ctx) {
        String query = ctx.getRewrittenQuery() != null ? ctx.getRewrittenQuery() : ctx.getQuery();
        Long kbId = ctx.getKbId();
        int topK = ctx.getTopK();
        int rerankTopN = ctx.getRerankTopN();

        if (kbId == null) {
            log.warn("No kbId specified, skip retrieval");
            ctx.setRetrievedChunks(List.of());
            return;
        }
        // 根据重写过的query进行检索
        List<Map<String, Object>> chunks = hybridSearchService.search(kbId, query, topK, rerankTopN);
        ctx.setRetrievedChunks(chunks);
        if (CollUtil.isNotEmpty(chunks)) {
            String provider = Objects.toString(chunks.get(0).get("_rerank_provider"), null);
            // 降级标记供后续检索质量评估节点判断分数语义
            ctx.setRerankDegraded(!"dashscope".equals(provider));
            // 记录Rerank结果
            ragMetricsRecorder.recordRerank(provider == null ? "cosine" : provider);
        }
        log.info("RAG retrieval: query_length={}, kbId={}, found={}", query.length(), kbId, chunks.size());
    }
}
