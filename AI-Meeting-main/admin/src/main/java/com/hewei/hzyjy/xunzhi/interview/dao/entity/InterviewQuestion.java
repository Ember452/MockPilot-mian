package com.hewei.hzyjy.xunzhi.interview.dao.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * 面试题存储表
 * @Document interview_question
 */
@Data
@Document(collection = "interview_question")
public class InterviewQuestion {
    /**
     * ID
     */
    @Id
    private String id;

    /**
     * 会话ID
     */
    @Indexed
    private String sessionId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 面试题列表（保留用于向后兼容）
     */
    private List<String> questions;

    /**
     * 面试题JSON格式存储（按题号组织）
     * 格式：{"1": "题目内容1", "2": "题目内容2", ...}
     */
    private String questionsJson;

    /**
     * 建议列表（保留用于向后兼容）
     */
    private List<String> suggestions;

    /**
     * 建议JSON格式存储（按编号组织）
     * 格式：{"1": "建议内容1", "2": "建议内容2", ...}
     */
    private String suggestionsJson;

    /**
     * 简历评分
     */
    private Integer resumeScore;

    /**
     * 面试类型（如：backend、frontend等）
     */
    private String interviewType;

    /**
     * 简历文件URL
     */
    private String resumeFileUrl;

    /**
     * AI响应的原始JSON数据
     */
    private String rawResponseData;

    /**
     * 响应时间(毫秒)
     */
    private Integer responseTime;

    /**
     * Token消耗数量
     */
    private Integer tokenCount;

    /**
     * 错误信息（如果处理失败）
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @CreatedDate
    private Date createTime;

    /**
     * 更新时间
     */
    @LastModifiedDate
    private Date updateTime;

    /**
     * 删除标识 0：未删除 1：已删除
     */
    private Integer delFlag;
}