package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.Data;

import java.util.Date;

@Data
public class InterviewConversationRespDTO {

    private String sessionId;

    private String conversationTitle;

    private String status;

    private String interviewType;

    private String resumeFileUrl;

    private Date createTime;

    private Date updateTime;
}
