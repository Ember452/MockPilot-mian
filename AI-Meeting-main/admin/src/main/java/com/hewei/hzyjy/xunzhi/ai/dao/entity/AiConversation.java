package com.hewei.hzyjy.xunzhi.ai.dao.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * AI会话表
 * @Document ai_conversation
 */
@Data
@Document(collection = "ai_conversation")
public class AiConversation {
    
    /**
     * ID
     */
    @Id
    private String id;
    
    /**
     * 会话ID
     */
    @Indexed(unique = true)
    private String sessionId;
    
    /**
     * 用户名
     */
    @Indexed
    private String username;
    
    /**
     * AI配置ID
     */
    @Indexed
    private Long aiId;
    
    /**
     * 会话标题
     */
    private String title;
    
    /**
     * 会话状态：1-进行中，2-已结束
     */
    private Integer status;
    
    /**
     * 消息总数
     */
    private Integer messageCount;
    
    /**
     * 最后一条消息时间
     */
    private Date lastMessageTime;
    
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