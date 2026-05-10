package com.hewei.hzyjy.xunzhi.interview.service.model;

import lombok.Builder;
import lombok.Data;

/**
 * 面试单轮日志结构体（用于 Redis 缓存与快照持久化）。
 */
@Data
@Builder
public class InterviewTurnLog {

    private Long timestamp;

    private String requestId;

    private String questionNumber;

    private String questionContent;

    private String answerContent;

    private Integer score;

    private Integer totalScore;

    private String feedback;

    private Boolean followUpNeeded;

    private Boolean isFollowUp;

    private Integer followUpCount;

    private String nextQuestionNumber;

    private String nextQuestion;

    private Boolean finished;
}
