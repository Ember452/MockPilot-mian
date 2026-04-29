package com.hewei.hzyjy.xunzhi.interview.application.runtime;

/**
 * 定义热快照刷新触发类型及其默认策略，
 * 用于区分普通中间态刷新与关键检查点立即刷新场景。
 *
 * @author 程序员牛肉
 */
public enum InterviewSessionRuntimeHotRefreshTrigger {

    QUESTION_READY("QUESTION_READY", false, false, 10),
    DEMEANOR_READY("DEMEANOR_READY", false, false, 20),
    ANSWER_COMMITTED("ACTIVE", true, true, 30),
    FINALIZED("FINALIZED", true, false, 40);

    private final String snapshotLevel;

    private final boolean forceFlush;

    private final boolean persistTurnArchive;

    private final int priority;

    InterviewSessionRuntimeHotRefreshTrigger(
            String snapshotLevel,
            boolean forceFlush,
            boolean persistTurnArchive,
            int priority) {
        this.snapshotLevel = snapshotLevel;
        this.forceFlush = forceFlush;
        this.persistTurnArchive = persistTurnArchive;
        this.priority = priority;
    }

    public String getSnapshotLevel() {
        return snapshotLevel;
    }

    public boolean isForceFlush() {
        return forceFlush;
    }

    public boolean isPersistTurnArchive() {
        return persistTurnArchive;
    }

    public int getPriority() {
        return priority;
    }
}
