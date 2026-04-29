package com.hewei.hzyjy.xunzhi.interview.application.runtime;

import com.hewei.hzyjy.xunzhi.interview.api.io.req.DemeanorScoreDTO;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 定义面试会话低频材料的增量更新载体，
 * 用于按字段 Patch 冷快照中的题目、简历上下文和评分材料信息。
 *
 * @author 程序员牛肉
 */
@Data
@Builder
public class InterviewSessionRuntimeColdPatch {

    private Long userId;

    private Long materialVersion;

    private String resumeFileUrl;

    private String interviewType;

    private String direction;

    private Map<String, String> questions;

    private Map<String, String> suggestions;

    private Map<String, Object> resumeContext;

    private Integer resumeScore;

    private Integer demeanorScore;

    private DemeanorScoreDTO demeanorDetails;

    private Date materialUpdatedAt;
}
