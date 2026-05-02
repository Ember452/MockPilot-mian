package com.hewei.hzyjy.xunzhi.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 定义面试热快照合并写与防抖刷新的配置参数，
 * 用于控制防抖窗口、最大聚合窗口、强制刷新等待时长和失败重试节奏。
 *
 * @author 程序员牛肉
 */
@Data
@Component
@ConfigurationProperties(prefix = "xunzhi-agent.interview.hot-refresh")
public class InterviewHotRefreshConfiguration {

    private Boolean enable = true;

    private Long debounceWindowMillis = 150L;

    private Long maxAggregateWindowMillis = 500L;

    private Long forceFlushWaitMillis = 600L;

    private Long failureRetryDelayMillis = 200L;

    private Integer maxImmediateFlushAttempts = 2;
}
