package com.hewei.hzyjy.xunzhi.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 复习闭环配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "xunzhi-agent.review")
public class ReviewProperties {

    /**
     * 弱项抽取 LLM 调用超时（毫秒）
     */
    private long extractTimeoutMs = 5000;

    /**
     * 单场面试最多生成的复习条目数
     */
    private int maxItemsPerSession = 8;
}
