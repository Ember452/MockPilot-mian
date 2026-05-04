package com.hewei.hzyjy.xunzhi.interview.dao.entity;

import com.hewei.hzyjy.xunzhi.interview.api.io.req.DemeanorScoreDTO;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewFlowState;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewRuntimeConfidence;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewRuntimeScoreAggregate;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewTurnLog;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 定义面试会话运行态快照持久化实体，
 * 用于保存会话恢复所需的题目、流程、得分聚合、追问和最近轮次等关键状态。
 *
 * @author 程序员牛肉
 */
@Data
public class InterviewSessionRuntimeSnapshot {

    private String id;

    private String sessionId;

    private Long userId;

    private String sessionStatus;

    private Long snapshotVersion;

    private String snapshotLevel;

    private InterviewRuntimeConfidence rebuildConfidence;

    private Date snapshotUpdatedAt;

    private String resumeFileUrl;

    private String interviewType;

    private String direction;

    private Map<String, String> questions;

    private Map<String, String> suggestions;

    private Map<String, Object> resumeContext;

    private Integer resumeScore;

    private Integer demeanorScore;

    private DemeanorScoreDTO demeanorDetails;

    private InterviewFlowState flow;

    private InterviewRuntimeScoreAggregate scoreAggregate;

    private Map<String, String> followUpQuestions;

    private List<InterviewTurnLog> recentTurns;

    private Integer recentTurnCount;

    private Long archiveWatermark;

    private Long lastTurnSeq;

    private String lastAppliedRequestId;

    private String lastMutationId;

    private Date lastMutationTime;

    private String lastCommittedQuestionNumber;

    private String lastCommittedTurnDigest;

    private Long materialVersion;

    private Date createTime;

    private Date updateTime;
}
