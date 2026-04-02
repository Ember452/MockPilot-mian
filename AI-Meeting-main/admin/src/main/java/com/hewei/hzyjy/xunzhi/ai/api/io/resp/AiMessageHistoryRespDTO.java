package com.hewei.hzyjy.xunzhi.ai.api.io.resp;

import com.hewei.hzyjy.xunzhi.ai.api.io.BaseMessageHistoryRespDTO;
import lombok.Data;

import java.util.Date;

/**
 * AI消息历史响应DTO
 * @author nageoffer
 */
@Data
public class AiMessageHistoryRespDTO implements BaseMessageHistoryRespDTO {
    
    /**
     * 消息ID
     */
    private String id;
    
    /**
     * 会话ID
     */
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
     * 消息序号
     */
    private Integer messageSeq;
    
    /**
     * Token消耗数量
     */
    private Integer tokenCount;
    
    /**
     * 响应时间(毫秒)
     */
    private Integer responseTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private Date createTime;
}