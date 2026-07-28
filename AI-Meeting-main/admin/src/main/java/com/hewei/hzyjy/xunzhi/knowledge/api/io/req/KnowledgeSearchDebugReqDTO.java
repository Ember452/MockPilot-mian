package com.hewei.hzyjy.xunzhi.knowledge.api.io.req;

import lombok.Data;

/**
 * 检索评测调试请求（仅登录鉴权内使用，供 scripts/rag-eval 评测脚本调用）
 */
@Data
public class KnowledgeSearchDebugReqDTO {

    private String query;

    /**
     * 召回条数，缺省用全局默认 top-k
     */
    private Integer topK;
}
