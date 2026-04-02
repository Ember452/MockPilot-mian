package com.hewei.hzyjy.xunzhi.ai.api.io.resp;

import lombok.Data;

/**
 * AI会话创建响应DTO
 * @author nageoffer
 */
@Data
public class AiSessionCreateRespDTO {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 会话标题
     */
    private String conversationTitle;
}