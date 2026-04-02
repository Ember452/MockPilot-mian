package com.hewei.hzyjy.xunzhi.ai.api.io.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * AI配置响应DTO
 * @author nageoffer
 */
@Data
public class AiPropertiesRespDTO {
    
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
     * API密钥（脱敏显示）
     */
    private String apiKey;
    
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
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    
    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}