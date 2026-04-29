package com.hewei.hzyjy.xunzhi.interview.application.runtime;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewTurnLog;
import lombok.Getter;

import java.util.concurrent.ScheduledFuture;

/**
 * 定义单个面试会话的热刷新聚合桶，
 * 用于在本地节点内暂存同一 session 的待刷新意图、定时任务句柄和刷新中状态。
 *
 * @author 程序员牛肉
 */
@Getter
public class InterviewSessionRuntimeHotRefreshBucket {

    private final String sessionId;

    private InterviewSessionRuntimeHotRefreshTrigger pendingTrigger;

    private String pendingSnapshotLevel;

    private String pendingRequestId;

    private InterviewTurnLog pendingCommittedTurn;

    private boolean pendingPersistTurnArchive;

    private boolean pendingForceFlush;

    private long firstEventAt;

    private long lastEventAt;

    private long scheduledFlushAt;

    private boolean flushInProgress;

    private ScheduledFuture<?> scheduledFuture;

    public InterviewSessionRuntimeHotRefreshBucket(String sessionId) {
        this.sessionId = sessionId;
    }

    public void merge(InterviewSessionRuntimeHotRefreshRequest request) {
        if (request == null) {
            return;
        }
        long now = request.getTriggerAt() > 0 ? request.getTriggerAt() : System.currentTimeMillis();
        if (firstEventAt <= 0L) {
            firstEventAt = now;
        }
        lastEventAt = now;
        if (shouldReplaceTrigger(request.getTrigger())) {
            pendingTrigger = request.getTrigger();
            pendingSnapshotLevel = request.getSnapshotLevel();
        }
        if (StrUtil.isNotBlank(request.getRequestId())) {
            pendingRequestId = request.getRequestId().trim();
        }
        if (request.getCommittedTurn() != null) {
            pendingCommittedTurn = request.getCommittedTurn();
        }
        pendingPersistTurnArchive = pendingPersistTurnArchive || request.isPersistTurnArchive();
        pendingForceFlush = pendingForceFlush || request.isForceFlush();
    }

    public boolean hasPendingRefresh() {
        return pendingTrigger != null || StrUtil.isNotBlank(pendingSnapshotLevel);
    }

    public boolean isPendingForceFlush() {
        return pendingForceFlush;
    }

    public void attachScheduledFuture(ScheduledFuture<?> future, long scheduledFlushAt) {
        this.scheduledFuture = future;
        this.scheduledFlushAt = scheduledFlushAt;
    }

    public void clearScheduledFuture() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = null;
        scheduledFlushAt = 0L;
    }

    public InterviewSessionRuntimeHotRefreshRequest startFlush() {
        if (flushInProgress || !hasPendingRefresh()) {
            return null;
        }
        flushInProgress = true;
        clearScheduledFuture();
        InterviewSessionRuntimeHotRefreshRequest request = InterviewSessionRuntimeHotRefreshRequest.builder()
                .sessionId(sessionId)
                .trigger(pendingTrigger)
                .snapshotLevel(pendingSnapshotLevel)
                .requestId(pendingRequestId)
                .committedTurn(pendingCommittedTurn)
                .persistTurnArchive(pendingPersistTurnArchive)
                .forceFlush(pendingForceFlush)
                .triggerAt(lastEventAt > 0 ? lastEventAt : System.currentTimeMillis())
                .build();
        resetPendingState();
        return request;
    }

    public void completeFlush() {
        flushInProgress = false;
    }

    public void restoreFailedRequest(InterviewSessionRuntimeHotRefreshRequest request) {
        if (request == null) {
            return;
        }
        merge(request);
        pendingForceFlush = true;
    }

    public long computeScheduledFlushAt(long now, long debounceWindowMillis, long maxAggregateWindowMillis) {
        long firstAt = firstEventAt > 0 ? firstEventAt : now;
        long lastAt = lastEventAt > 0 ? lastEventAt : now;
        long debounceAt = lastAt + Math.max(debounceWindowMillis, 0L);
        long maxAt = firstAt + Math.max(maxAggregateWindowMillis, 0L);
        return Math.min(debounceAt, maxAt);
    }

    private boolean shouldReplaceTrigger(InterviewSessionRuntimeHotRefreshTrigger trigger) {
        if (trigger == null) {
            return false;
        }
        if (pendingTrigger == null) {
            return true;
        }
        return trigger.getPriority() >= pendingTrigger.getPriority();
    }

    private void resetPendingState() {
        pendingTrigger = null;
        pendingSnapshotLevel = null;
        pendingRequestId = null;
        pendingCommittedTurn = null;
        pendingPersistTurnArchive = false;
        pendingForceFlush = false;
        firstEventAt = 0L;
        lastEventAt = 0L;
    }
}
