package com.hewei.hzyjy.xunzhi.knowledge.flow;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@LiteflowComponent("ragRetrieval")
@RequiredArgsConstructor
public class RagRetrievalNode extends NodeComponent {

    private final com.hewei.hzyjy.xunzhi.knowledge.service.HybridSearchService hybridSearchService;

    @Override
    public void process() throws Exception {
        RagContext ctx = this.getContextBean(RagContext.class);
        String query = ctx.getQuery();
        Long kbId = ctx.getKbId();
        int topK = ctx.getTopK();
        int rerankTopN = ctx.getRerankTopN();

        if (kbId == null) {
            log.warn("No kbId specified, skip retrieval");
            ctx.setRetrievedChunks(List.of());
            return;
        }

        List<Map<String, Object>> chunks = hybridSearchService.search(kbId, query, topK, rerankTopN);
        ctx.setRetrievedChunks(chunks);
        log.info("RAG retrieval: query_length={}, kbId={}, found={}", query.length(), kbId, chunks.size());
    }
}
