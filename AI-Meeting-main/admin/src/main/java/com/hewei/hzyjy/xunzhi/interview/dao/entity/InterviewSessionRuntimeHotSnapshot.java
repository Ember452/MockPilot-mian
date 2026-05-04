package com.hewei.hzyjy.xunzhi.interview.dao.entity;

import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewFlowState;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewRuntimeConfidence;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewRuntimeScoreAggregate;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewTurnLog;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 定义面试会话高频运行态快照持久化实体，
 * 用于保存可快速恢复当前面试流程所需的 flow、得分聚合、最近轮次与幂等控制状态。
 *
 * @author 程序员牛肉
 */
@Data
@Document(collection = "interview_session_runtime_hot_snapshot")
public class InterviewSessionRuntimeHotSnapshot {

    /**
     * Mongo 文档主键，用于唯一标识一条高频运行态快照记录。
     */
    @Id
    private String id;

    /**
     * 面试会话唯一标识，用于按 session 维度定位热快照。
     */
    @Indexed(unique = true)
    private String sessionId;

    /**
     * 当前会话所属用户 ID，用于补充运行态归属关系。
     */
    @Indexed
    private Long userId;

    /**
     * 当前会话状态，如进行中、已完成、已关闭等。
     */
    private String sessionStatus;

    /**
     * 热快照版本号，每次高频运行态刷新后递增，用于标识检查点新旧。
     */
    private Long snapshotVersion;

    /**
     * 当前热快照所处业务阶段，如 DRAFT、QUESTION_READY、ACTIVE、FINALIZED。
     */
    private String snapshotLevel;

    /**
     * 当前热快照的恢复置信度，用于标识恢复后是否可继续写入。
     */
    private InterviewRuntimeConfidence rebuildConfidence;

    /**
     * 业务语义下的热快照刷新时间，用于表示运行态最近一次被持久化的时刻。
     */
    private Date snapshotUpdatedAt;

    /**
     * 当前面试流程状态，描述进行到哪一题、是否在追问以及是否已结束。
     */
    private InterviewFlowState flow;

    /**
     * 当前运行态得分聚合，包含累计得分、计分次数和会话总分。
     */
    private InterviewRuntimeScoreAggregate scoreAggregate;

    /**
     * 当前会话派生出的追问题集合，通常以题号映射追问内容。
     */
    private Map<String, String> followUpQuestions;

    /**
     * 最近几轮问答窗口，用于快速恢复上下文与支撑软回放幂等补偿。
     */
    private List<InterviewTurnLog> recentTurns;

    /**
     * 最近轮次窗口的数量，便于快速感知当前热状态中保留了多少轮记录。
     */
    private Integer recentTurnCount;

    /**
     * 归档水位，表示当前热快照已同步覆盖到第几条 Turn Archive 记录。
     */
    private Long archiveWatermark;

    /**
     * 最近一次已归档轮次的顺序号，
     * 用于在热快照 CAS 更新时做轮次单调保护和恢复边界判断。
     */
    private Long lastTurnSeq;

    /**
     * 最近一次已成功写入运行态的请求 ID，用于答题幂等和重复请求判定。
     */
    private String lastAppliedRequestId;

    /**
     * 最近一次成功提交到热快照的业务变更标识，
     * 这里通常复用答题 requestId，用于处理超时重试下的幂等确认。
     */
    private String lastMutationId;

    /**
     * 最近一次成功提交热快照变更的时间，
     * 用于故障排查和提交结果未知场景下的辅助确认。
     */
    private Date lastMutationTime;

    /**
     * 最近一次成功提交所对应的题号，用于恢复和重复提交补偿时定位上下文。
     */
    private String lastCommittedQuestionNumber;

    /**
     * 最近一次成功提交轮次的内容摘要，用于在 requestId 缺失时辅助做软回放幂等。
     */
    private String lastCommittedTurnDigest;

    /**
     * 热快照文档创建时间。
     */
    @CreatedDate
    private Date createTime;

    /**
     * 热快照文档最后更新时间。
     */
    @LastModifiedDate
    private Date updateTime;
}
