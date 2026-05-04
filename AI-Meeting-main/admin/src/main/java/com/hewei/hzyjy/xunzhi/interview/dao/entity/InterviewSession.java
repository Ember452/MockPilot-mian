package com.hewei.hzyjy.xunzhi.interview.dao.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "interview_session")
public class InterviewSession {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionId;

    @Indexed
    private Long userId;

    @Indexed
    private String status;

    private String conversationTitle;

    private Long interviewerAgentId;

    private String resumeFileUrl;

    private String interviewType;

    private Date startTime;

    private Date endTime;

    @CreatedDate
    private Date createTime;

    @LastModifiedDate
    private Date updateTime;

    private Integer delFlag;
}
