package com.hewei.hzyjy.xunzhi.ai.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hewei.hzyjy.xunzhi.common.database.BaseDO;
import com.hewei.hzyjy.xunzhi.common.database.BaseMessage;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * AI消息表
 * @Document ai_message
 */
@Data
@Document(collection = "ai_message")
public class AiMessage {
    
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
     * 消息类型：1-用户消息，2-AI回复
     */
    private Integer messageType;
    
    /**
     * 消息内容
     */
    private String messageContent;

    /**
     * 深度思考内容 (DeepSeek R1)
     */
    private String reasoningContent;
    
    /**
     * 消息序号，同一会话内递增
     */
    private Integer messageSeq;
    
    /**
     * 父消息ID，用于消息关联
     */
    private Long parentMsgId;
    
    /**
     * Token消耗数量
     */
    private Integer tokenCount;
    
    /**
     * 响应时间(毫秒)
     */
    private Integer responseTime;
    
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