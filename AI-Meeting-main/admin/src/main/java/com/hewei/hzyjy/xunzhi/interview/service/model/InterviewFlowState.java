package com.hewei.hzyjy.xunzhi.interview.service.model;

import lombok.Data;

/**
 *
 * Redis中的面试流程状态缓存
 */
@Data
public class InterviewFlowState {

    private String status;

    private Integer currentIndex;

    private String currentQuestionNumber;

    private Integer totalQuestions;

    private Integer followUpCount;

    private Integer maxFollowUp;

    private Integer version;

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }
}

