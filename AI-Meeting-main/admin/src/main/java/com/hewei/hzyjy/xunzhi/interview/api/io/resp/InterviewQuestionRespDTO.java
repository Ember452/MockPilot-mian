package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class InterviewQuestionRespDTO {

    private String id;

    private String sessionId;

    private String userName;

    private Map<String, String> questions;

    private Map<String, String> suggestions;

    private String interviewType;

    private String resumeFileUrl;

    private Integer responseTime;

    private Integer tokenCount;

    private Integer resumeScore;

    private Integer questionCount;

    private Integer suggestionCount;

    private Integer isSuccess = 1;

    private String errorMessage;

    private Date createTime;

    private Date updateTime;
}
