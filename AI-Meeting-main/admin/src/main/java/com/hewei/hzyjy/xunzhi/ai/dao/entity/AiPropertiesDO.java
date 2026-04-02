package com.hewei.hzyjy.xunzhi.ai.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * AI配置实体类
 * @author nageoffer
 */
@Data
@TableName("ai_properties")
public class AiPropertiesDO {
    
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
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
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 修改时间
     */
    private Date updateTime;
    
    /**
     * 删除标识 0：未删除 1：已删除
     */
    private Integer delFlag;
}