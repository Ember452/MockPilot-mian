package com.hewei.hzyjy.xunzhi.interview.application.rule;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.interview.config.InterviewRuleEngineConfiguration;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * LiteFlow-based follow-up rule service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewFollowUpRuleService {

    @Resource
    private final FlowExecutor flowExecutor;
    private final InterviewRuleEngineConfiguration ruleConfiguration;

    public InterviewFollowUpRuleDecision decide(InterviewFollowUpRuleContext context) {
        if (context == null) {
            return InterviewFollowUpRuleDecision.noFollowUp(
                    Math.max(resolveDefaultMaxFollowUp(), 1),
                    "RULE_CONTEXT_MISSING",
                    "rule context is missing",
                    resolveDefaultChainId(),
                    ruleConfiguration.getRuleVersion(),
                    true
            );
        }

        hydrateContext(context);
        if (!Boolean.TRUE.equals(ruleConfiguration.getEnable())) {
            return fallbackDecision(context, "RULE_ENGINE_DISABLED", "rule engine disabled");
        }

        try {
            LiteflowResponse response = executeChain(context.getChainId(), context);
            if (response == null || !response.isSuccess()) {
                Throwable cause = response == null ? null : response.getCause();
                throw new IllegalStateException("LiteFlow execution failed", cause);
            }
            context.finalizeDecision(false);
            return context.getDecision();
        } catch (Exception ex) {
            if (Boolean.TRUE.equals(ruleConfiguration.getFailOpen())) {
                log.warn(
                        "LiteFlow follow-up decision failed, fallback to legacy policy, sessionId={}, chainId={}, questionNumber={}",
                        context.getSessionId(),
                        context.getChainId(),
                        context.getQuestionNumber(),
                        ex
                );
                return fallbackDecision(context, "RULE_ENGINE_FALLBACK", "fallback to legacy follow-up policy");
            }
            throw new IllegalStateException("LiteFlow follow-up decision failed", ex);
        }
    }

    LiteflowResponse executeChain(String chainId, InterviewFollowUpRuleContext context) {
        return flowExecutor.execute2Resp(chainId, null, context);
    }

    private void hydrateContext(InterviewFollowUpRuleContext context) {
        context.setChainId(resolveDefaultChainId());
        context.setRuleVersion(ruleConfiguration.getRuleVersion());
        context.setResolvedMaxFollowUp(resolveMaxFollowUp(context.getMaxFollowUp()));
        context.setLowScoreThreshold(resolveLowScoreThreshold());
        context.setDecision(new InterviewFollowUpRuleDecision());
        context.setTerminated(false);
    }

    private InterviewFollowUpRuleDecision fallbackDecision(
            InterviewFollowUpRuleContext context,
            String fallbackReasonCode,
            String fallbackReasonText) {
        int resolvedMax = resolveMaxFollowUp(context.getMaxFollowUp());
        boolean underLimit = context.getFollowUpCount() < resolvedMax;
        boolean needFollowUp = context.isFollowUpNeededFromAi() && underLimit;
        String reasonCode = needFollowUp
                ? "AI_SUGGESTED"
                : (underLimit ? fallbackReasonCode : "FOLLOW_UP_LIMIT_REACHED");
        String reasonText = needFollowUp
                ? "follow_up_needed from ai result"
                : (underLimit ? fallbackReasonText : "follow-up limit reached");
        InterviewFollowUpRuleDecision decision = InterviewFollowUpRuleDecision.noFollowUp(
                resolvedMax,
                reasonCode,
                reasonText,
                resolveDefaultChainId(),
                ruleConfiguration.getRuleVersion(),
                true
        );
        decision.setNeedFollowUp(needFollowUp);
        return decision;
    }

    private String resolveDefaultChainId() {
        return StrUtil.isNotBlank(ruleConfiguration.getDefaultChainId())
                ? ruleConfiguration.getDefaultChainId()
                : "default_followup_chain";
    }

    private int resolveMaxFollowUp(int fallbackMaxFollowUp) {
        if (fallbackMaxFollowUp > 0) {
            return fallbackMaxFollowUp;
        }
        return resolveDefaultMaxFollowUp();
    }

    private int resolveDefaultMaxFollowUp() {
        Integer configured = ruleConfiguration.getDefaultMaxFollowUp();
        return configured != null && configured > 0 ? configured : 2;
    }

    private int resolveLowScoreThreshold() {
        Integer defaultThreshold = ruleConfiguration.getDefaultLowScoreThreshold();
        return defaultThreshold != null && defaultThreshold >= 0 ? defaultThreshold : 60;
    }
}
