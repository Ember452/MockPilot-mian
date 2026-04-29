package com.hewei.hzyjy.xunzhi.interview.application.runtime;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewTurnLog;
import lombok.Builder;
import lombok.Data;

/**
 * 定义热快照刷新意图请求模型，
 * 用于承载一次运行态热刷新所需的触发类型、请求标识和已提交轮次等上下文。
 *
 * @author 程序员牛肉
 */
@Data
@Builder
public class InterviewSessionRuntimeHotRefreshRequest {

    private String sessionId;

    private InterviewSessionRuntimeHotRefreshTrigger trigger;

    private String snapshotLevel;

    private String requestId;

    private InterviewTurnLog committedTurn;

    private boolean persistTurnArchive;

    private boolean forceFlush;

    private long triggerAt;

    public static InterviewSessionRuntimeHotRefreshRequest questionReady(String sessionId) {
        return buildDefault(sessionId, InterviewSessionRuntimeHotRefreshTrigger.QUESTION_READY, null, null);
    }

    public static InterviewSessionRuntimeHotRefreshRequest demeanorReady(String sessionId) {
        return buildDefault(sessionId, InterviewSessionRuntimeHotRefreshTrigger.DEMEANOR_READY, null, null);
    }

    public static InterviewSessionRuntimeHotRefreshRequest answerCommitted(
            String sessionId,
            String requestId,
            InterviewTurnLog committedTurn) {
        return buildDefault(sessionId, InterviewSessionRuntimeHotRefreshTrigger.ANSWER_COMMITTED, requestId, committedTurn);
    }

    public static InterviewSessionRuntimeHotRefreshRequest finalized(String sessionId) {
        return buildDefault(sessionId, InterviewSessionRuntimeHotRefreshTrigger.FINALIZED, null, null);
    }

    private static InterviewSessionRuntimeHotRefreshRequest buildDefault(
            String sessionId,
            InterviewSessionRuntimeHotRefreshTrigger trigger,
            String requestId,
            InterviewTurnLog committedTurn) {
        return InterviewSessionRuntimeHotRefreshRequest.builder()
                .sessionId(StrUtil.trim(sessionId))
                .trigger(trigger)
                .snapshotLevel(trigger == null ? null : trigger.getSnapshotLevel())
                .requestId(StrUtil.trim(requestId))
                .committedTurn(committedTurn)
                .persistTurnArchive(trigger != null && trigger.isPersistTurnArchive())
                .forceFlush(trigger != null && trigger.isForceFlush())
                .triggerAt(System.currentTimeMillis())
                .build();
    }
}
