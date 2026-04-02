package com.hewei.hzyjy.xunzhi.ai.api.io.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * AI会话响应DTO
 * @author nageoffer
 */
@Data
public class AiConversationRespDTO {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * AI配置ID
     */
    private Long aiId;
    
    /**
     * AI名称
     */
    private String aiName;
    
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastMessageTime;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}