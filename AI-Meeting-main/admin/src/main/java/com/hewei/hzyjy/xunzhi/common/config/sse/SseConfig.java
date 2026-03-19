package com.hewei.hzyjy.xunzhi.common.config.sse;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * SSE配置类
 * 用于统一管理SSE连接的相关配置参数
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sse")
public class SseConfig {
    
    /**
     * SSE连接超时时间（毫秒）
     * 默认5分钟
     */
    private Long timeout = 300000L;
    
    /**
     * 心跳间隔时间（毫秒）
     * 默认10秒
     */
    private Long heartbeatInterval = 10000L;
    
    /**
     * AI接口连接超时时间（毫秒）
     * 默认30秒
     */
    private Integer connectTimeout = 30000;
    
    /**
     * AI接口读取超时时间（毫秒）
     * 默认5分钟
     */
    private Integer readTimeout = 300000;
    
    /**
     * 是否启用心跳机制
     * 默认启用
     */
    private Boolean enableHeartbeat = true;
    
    /**
     * 是否启用详细日志
     * 默认关闭
     */
    private Boolean enableVerboseLogging = false;
}