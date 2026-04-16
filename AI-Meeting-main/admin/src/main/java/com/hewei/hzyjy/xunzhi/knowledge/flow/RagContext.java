package com.hewei.hzyjy.xunzhi.knowledge.flow;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class RagContext implements Serializable {

    private String sessionId;

    private String query;

    private Long kbId;

    private int topK = 5;

    private int rerankTopN = 3;

    private boolean enableWebSearch = true;

    private List<Map<String, Object>> retrievedChunks;

    private String webSearchResult;

    private String compressedContext;

    private String generatedAnswer;

    private List<Map<String, String>> citations;
}
