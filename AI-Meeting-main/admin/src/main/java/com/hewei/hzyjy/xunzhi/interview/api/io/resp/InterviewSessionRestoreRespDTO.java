package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.Data;

import java.util.Map;

@Data
public class InterviewSessionRestoreRespDTO {

    private String sessionId;

    private String status;

    private Boolean canResume;

    private String resumeFileUrl;

    private Integer resumeScore;

    private String interviewType;

    private Map<String, String> suggestions;
}
