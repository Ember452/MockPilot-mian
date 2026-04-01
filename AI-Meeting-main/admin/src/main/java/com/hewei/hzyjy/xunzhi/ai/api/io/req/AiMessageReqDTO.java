package com.hewei.hzyjy.xunzhi.ai.api.io.req;

import lombok.Data;

import java.util.List;


/**
 * AI消息请求DTO
 * @author nageoffer
 */
@Data
public class AiMessageReqDTO {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户输入消息
     */
    private String inputMessage;
    
    /**
     * AI配置ID
     */
    private Long aiId;
    
    /**
     * 消息序号
     */
    private Integer messageSeq;
    
    /**
     * 用户名
     */
    private String userName;

    /**
     * 图片URL列表（多模态支持）
     */
    private List<String> imageUrls;

    /**
     * 文件URL列表（文档分析支持）
     */
    private List<String> fileUrls;
}