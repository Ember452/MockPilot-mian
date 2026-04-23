package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.Data;

/**
 * 面试问答回放项。
 */
@Data
public class InterviewPlaybackItemRespDTO {

    /**
     * 回放序号（从1开始）。
     */
    private Integer seq;

    /**
     * 时间戳（毫秒）。
     */
    private Long timestamp;

    /**
     * 请求幂等ID。
     */
    private String requestId;

    /**
     * 题号。
     */
    private String questionNumber;

    /**
     * 题目内容。
     */
    private String questionContent;

    /**
     * 用户回答文本。
     */
    private String answerContent;

    /**
     * 本轮得分。
     */
    private Integer score;

    /**
     * 本轮反馈。
     */
    private String feedback;

    /**
     * 累计总分。
     */
    private Integer totalScore;

    /**
     * 是否触发了继续追问判断。
     */
    private Boolean followUpNeeded;

    /**
     * 当前轮是否为追问。
     */
    private Boolean isFollowUp;

    /**
     * 当前主问题下的追问轮次。
     */
    private Integer followUpCount;

    /**
     * 下一题题号。
     */
    private String nextQuestionNumber;

    /**
     * 下一题题干。
     */
    private String nextQuestion;

    /**
     * 是否结束。
     */
    private Boolean finished;
}
