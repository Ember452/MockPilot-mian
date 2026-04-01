package com.hewei.hzyjy.xunzhi.ai.api.io.req;

import lombok.Data;

/**
 * AI会话分页查询请求DTO
 * @author nageoffer
 */
@Data
public class AiConversationPageReqDTO {
    
    /**
     * 当前页
     */
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * AI配置ID
     */
    private Long aiId;
    
    /**
     * 会话状态：1-进行中，2-已结束
     */
    private Integer status;
    
    /**
     * 会话标题（模糊查询）
     */
    private String title;
}