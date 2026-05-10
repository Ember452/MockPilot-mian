package com.hewei.hzyjy.xunzhi.interview.service.model;

public enum InterviewSessionStatus {

    DRAFT,
    RESUME_UPLOADING,
    READY,
    IN_PROGRESS,
    FINISHED,
    ABANDONED;

    public boolean isActive() {
        return this == DRAFT || this == RESUME_UPLOADING || this == READY || this == IN_PROGRESS;
    }

    public boolean canResume() {
        return this == READY || this == IN_PROGRESS;
    }
}
