package com.hewei.hzyjy.xunzhi.common.config.xunfei;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 讯飞配置属性类
 * 从application.yml中读取xunfei配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "xunfei.lat-key")
public class XunfeiLatProperties {
    
    /**
     * 讯飞应用ID
     */
    private String appId;
    
    /**
     * 讯飞API Key
     */
    private String apiKey;
    
    /**
     * 讯飞API Secret
     */
    private String apiSecret;
}