package com.hewei.hzyjy.xunzhi.interview.dao.entity;

import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewTurnLog;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 定义面试会话轮次归档持久化实体，
 * 用于保存每次答题提交后的轮次回放数据并支撑会话重建与软回放幂等。
 *
 * @author 程序员牛肉
 */
@Data
@Document(collection = "interview_session_turn_archive")
public class InterviewSessionTurnArchive {

    @Id
    private String id;

    @Indexed
    private String sessionId;

    @Indexed
    private String requestId;

    @Indexed
    private Long seq;

    private Long snapshotVersion;

    private InterviewTurnLog turnPayload;

    @CreatedDate
    private Date createdAt;
}
