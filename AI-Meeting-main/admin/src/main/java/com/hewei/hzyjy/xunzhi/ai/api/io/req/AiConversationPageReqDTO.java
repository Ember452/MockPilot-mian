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

    /**
     * 对话模式过滤：normal-普通对话（含存量无字段会话），rag-知识库对话；不传则不过滤
     */
    private String chatMode;

    /**
     * 知识库ID过滤（仅 chatMode=rag 时生效）
     */
    private Long kbId;
}