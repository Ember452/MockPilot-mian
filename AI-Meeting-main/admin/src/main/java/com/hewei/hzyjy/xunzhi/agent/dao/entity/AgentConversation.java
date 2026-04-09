package com.hewei.hzyjy.xunzhi.agent.dao.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 智能体会话表
 * @Document agent_conversation
 */
@Data
@Document(collection = "agent_conversation")
public class AgentConversation {
    /**
     * ID
     */
    @Id
    private String id;

    /**
     * 会话ID，UUID格式
     */
    @Indexed(unique = true)
    private String sessionId;

    /**
     * 用户ID
     */
    @Indexed
    private Long userId;

    /**
     * 智能体ID
     */
    @Indexed
    private Long agentId;

    /**
     * 会话标题，可从首条消息自动生成
     */
    private String conversationTitle;

    /**
     * 消息总数
     */
    private Integer messageCount;

    /**
     * 总Token消耗
     */
    private Integer totalTokens;

    /**
     * 会话状态：1-进行中，2-已结束，3-已删除
     */
    private Integer status;

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