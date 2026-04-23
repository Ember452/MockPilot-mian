package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.Data;

import java.util.List;

/**
 * Structured end-of-interview review feedback for report rendering.
 */
@Data
public class InterviewReviewFeedbackRespDTO {

    private String overallComment;

    private List<String> highlights;

    private List<String> improvementTips;

    private List<String> nextActions;
}
