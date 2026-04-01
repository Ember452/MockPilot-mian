package com.hewei.hzyjy.xunzhi.ai.api.io.req;

import lombok.Data;
/**
 * AI会话创建请求DTO
 * @author nageoffer
 */
@Data
public class AiSessionCreateReqDTO {
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * AI配置ID
     */
    private Long aiId;
    
    /**
     * 第一条消息
     */
    private String firstMessage;
}