package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 面试记录响应 DTO。
 */
@Data
public class InterviewRecordRespDTO {

    private Long id;

    private Long userId;

    private String sessionId;

    private Integer interviewScore;

    private Integer resumeScore;

    /**
     * INIT / IN_PROGRESS / FINISHED / EVALUATED
     */
    private String interviewStatus;

    private Integer questionCount;

    private Long interviewerAgentId;

    /**
     * 原始建议串（分号分隔）。
     */
    private String interviewSuggestions;

    /**
     * 建议结构化映射（编号 -> 建议文本）。
     */
    private Map<String, String> interviewSuggestionsMap;

    private String interviewDirection;

    private Date startTime;

    private Date endTime;

    private Integer durationSeconds;

    /**
     * 历史快照原文，兼容旧前端。
     */
    private String sessionSnapshotJson;

    /**
     * 雷达图聚合分值（结构化）。
     */
    private RadarChartDTO radarChart;

    /**
     * 雷达图维度列表（前端可直接绘图）。
     */
    private List<RadarDimensionItemRespDTO> radarDimensions;

    /**
     * 面试问答回放列表。
     */
    private List<InterviewPlaybackItemRespDTO> playbackItems;

    private InterviewReviewFeedbackRespDTO reviewFeedback;

    private Date createTime;

    private Date updateTime;
}
