package com.hewei.hzyjy.xunzhi.knowledge.flow;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LiteflowComponent("contextCompression")
@RequiredArgsConstructor
public class ContextCompressionNode extends NodeComponent {

    private static final int MAX_CONTEXT_LENGTH = 4000;

    @Override
    public void process() throws Exception {
        RagContext ctx = this.getContextBean(RagContext.class);

        StringBuilder context = new StringBuilder();

        if (CollUtil.isNotEmpty(ctx.getRetrievedChunks())) {
            context.append("【知识库检索结果】\n");
            for (int i = 0; i < ctx.getRetrievedChunks().size(); i++) {
                var chunk = ctx.getRetrievedChunks().get(i);
                String content = (String) chunk.getOrDefault("content", "");
                String fileName = (String) chunk.getOrDefault("file_name", "");

                context.append(String.format("[来源%d] %s\n%s\n\n", i + 1, fileName, content));

                if (context.length() > MAX_CONTEXT_LENGTH) {
                    context.append("...(更多结果已截断)");
                    break;
                }
            }
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
}
