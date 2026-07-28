package com.hewei.hzyjy.xunzhi.knowledge.dao.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * RAG 链路单轮明细留档（TTL 自动过期，见 RagTraceService 索引维护）。
 */
@Data
@Accessors(chain = true)
@Document(collection = "rag_trace")
public class RagTrace {

    @Id
    private String id;

    @Field("session_id")
    private String sessionId;

    @Indexed
    @Field("kb_id")
    private Long kbId;

    @Indexed
    @Field("username")
    private String username;

    /** 各节点耗时（毫秒），key=节点名 */
    @Field("stage_timings")
    private Map<String, Long> stageTimings;

    @Field("rewrite_applied")
    private boolean rewriteApplied;

    /** dashscope | cosine */
    @Field("rerank_provider")
    private String rerankProvider;

    /** pass | web_search */
    @Field("grader_decision")
    private String graderDecision;

    @Field("web_search_triggered")
    private boolean webSearchTriggered;

    @Field("reference_count")
    private int referenceCount;

    @Field("token_total")
    private long tokenTotal;

    /** token 数为字符估算值（未收到 usage 帧） */
    @Field("token_estimated")
    private boolean tokenEstimated;

    @Field("create_time")
    private LocalDateTime createTime;
}
