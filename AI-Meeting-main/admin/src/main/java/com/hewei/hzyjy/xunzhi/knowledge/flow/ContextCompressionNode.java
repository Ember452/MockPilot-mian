package com.hewei.hzyjy.xunzhi.knowledge.flow;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@LiteflowComponent("contextCompression")
@RequiredArgsConstructor
public class ContextCompressionNode extends NodeComponent {

    // 父块注入后单条体量变大，上限提高避免 3 条父块被过早截断
    private static final int MAX_CONTEXT_LENGTH = 6000;

    @Override
    public void process() throws Exception {
        RagContext ctx = this.getContextBean(RagContext.class);

        StringBuilder context = new StringBuilder();

        if (CollUtil.isNotEmpty(ctx.getRetrievedChunks())) {
            context.append(buildKnowledgeContext(ctx.getRetrievedChunks()));
        }

        if (ctx.getWebSearchResult() != null && !ctx.getWebSearchResult().isBlank()) {
            context.append("\n【联网搜索结果】\n").append(ctx.getWebSearchResult()).append("\n");
        }

        if (context.length() > MAX_CONTEXT_LENGTH) {
            context = new StringBuilder(context.substring(0, MAX_CONTEXT_LENGTH) + "...(上下文已压缩)");
        }

        ctx.setCompressedContext(context.toString());
        log.info("Context compressed: total_length={}", ctx.getCompressedContext().length());
    }

    /**
     * Small-to-Big 注入：命中子块若携带 parent_content 则以父块内容注入，
     * 同父块按 parent_id 去重（后续命中仅标注同源，编号仍与 references 对齐）；
     * 无 parent_content 的存量子块回退 content。static 纯函数供单测。
     */
    static String buildKnowledgeContext(List<Map<String, Object>> chunks) {
        StringBuilder context = new StringBuilder("【知识库检索结果】\n");
        Map<String, Integer> injectedParents = new HashMap<>();

        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> chunk = chunks.get(i);
            String fileName = (String) chunk.getOrDefault("file_name", "");
            String parentId = (String) chunk.get("parent_id");
            String parentContent = (String) chunk.get("parent_content");

            String body;
            if (StrUtil.isNotBlank(parentId) && StrUtil.isNotBlank(parentContent)) {
                Integer firstNo = injectedParents.get(parentId);
                if (firstNo != null) {
                    body = "(内容同 [" + firstNo + "])";
                } else {
                    injectedParents.put(parentId, i + 1);
                    body = parentContent;
                }
            } else {
                body = (String) chunk.getOrDefault("content", "");
            }

            // 编号与 references 事件数组序号对齐，供模型行内引用 [n]
            context.append(String.format("[%d] %s\n%s\n\n", i + 1, fileName, body));

            if (context.length() > MAX_CONTEXT_LENGTH) {
                context.append("...(更多结果已截断)");
                break;
            }
        }
        return context.toString();
    }
}
