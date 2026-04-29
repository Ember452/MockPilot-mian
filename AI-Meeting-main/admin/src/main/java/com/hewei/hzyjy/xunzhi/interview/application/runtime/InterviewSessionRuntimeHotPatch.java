package com.hewei.hzyjy.xunzhi.interview.application.runtime;

import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewFlowState;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewRuntimeConfidence;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewRuntimeScoreAggregate;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewTurnLog;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 定义面试会话高频运行态的增量更新载体，
 * 用于按字段 Patch 热快照中的 flow、得分、最近轮次和幂等信息。
 *
 * @author 程序员牛肉
 */
@Data
@Builder
public class InterviewSessionRuntimeHotPatch {

    private Long userId;

    private String sessionStatus;

    private Long snapshotVersion;

    private String snapshotLevel;

    private InterviewRuntimeConfidence rebuildConfidence;

    private Date snapshotUpdatedAt;

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
}
