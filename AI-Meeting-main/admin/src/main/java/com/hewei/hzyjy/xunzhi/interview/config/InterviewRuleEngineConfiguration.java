package com.hewei.hzyjy.xunzhi.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Interview follow-up rule-engine configuration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "xunzhi-agent.interview.rule-engine")
public class InterviewRuleEngineConfiguration {

    private Boolean enable = true;

    private String defaultChainId = "default_followup_chain";

    private Boolean failOpen = true;

    private String ruleVersion = "v1.0.0";

    private Integer defaultMaxFollowUp = 2;

    private Integer defaultLowScoreThreshold = 60;
}
