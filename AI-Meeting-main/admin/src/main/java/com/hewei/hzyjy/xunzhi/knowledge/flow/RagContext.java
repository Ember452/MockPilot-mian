package com.hewei.hzyjy.xunzhi.knowledge.flow;

import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiMessageHistoryRespDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索上下文(从问题输入到答案输出的所有数据)
 * 让所有数据都在一个对象中传递，避免了传参过多的问题，便于埋点
 */
@Data
@Accessors(chain = true)
public class RagContext implements Serializable {

    private String sessionId;

    private String query;

    private String rewrittenQuery;

    private List<AiMessageHistoryRespDTO> historyMessages;

    private Long kbId;

    private int topK = 5;

    private int rerankTopN = 3;

    private boolean enableWebSearch = true;

    private List<Map<String, Object>> retrievedChunks;

    private boolean rerankDegraded;

    private boolean needWebSearch;

    private List<Map<String, Object>> references;

    /** 网页溯源结果 */
    private String webSearchResult;

    /** 压缩后的知识库检索结果 */
    private String compressedContext;

    private String generatedAnswer;

    private List<Map<String, String>> citations;

    /** 各节点耗时（毫秒），按执行顺序记录，供埋点与 trace 留档 */
    private Map<String, Long> stageTimings = new LinkedHashMap<>();
}
