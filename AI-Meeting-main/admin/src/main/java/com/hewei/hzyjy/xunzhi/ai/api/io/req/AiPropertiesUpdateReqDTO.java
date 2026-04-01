package com.hewei.hzyjy.xunzhi.ai.api.io.req;

import lombok.Data;


import java.math.BigDecimal;

/**
 * AI配置更新请求DTO
 * @author nageoffer
 */
@Data
public class AiPropertiesUpdateReqDTO {
    
    /**
     * ID
     */
    private Long id;
    
    /**
     * AI名称
     */
    private String aiName;
    
    /**
     * AI类型：spark、openai、claude等
     */
    private String aiType;
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * API密钥（部分AI需要）
     */
    private String apiSecret;
    
    /**
     * 项目ID (OpenAI等需要)
     */
    private String projectId;
    
    /**
     * 组织ID (OpenAI等需要)
     */
    private String organizationId;
    
    /**
     * API地址
     */
    private String apiUrl;
    
    /**
     * 模型名称
     */
    private String modelName;
    
    /**
     * 最大token数
     */
    private Integer maxTokens;
    
    /**
     * 温度参数
     */
    private BigDecimal temperature;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 是否启用 0：禁用 1：启用
     */
    private Integer isEnabled;
}