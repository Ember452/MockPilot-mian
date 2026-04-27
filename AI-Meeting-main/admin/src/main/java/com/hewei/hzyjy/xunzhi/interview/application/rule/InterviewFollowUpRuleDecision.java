package com.hewei.hzyjy.xunzhi.interview.application.rule;

import lombok.Data;

/**
 * Follow-up rule decision output.
 */
@Data
public class InterviewFollowUpRuleDecision {

    private boolean needFollowUp;

    private int resolvedMaxFollowUp;

    private String reasonCode;

    private String reasonText;

    private String chainId;

    private String ruleVersion;

    private boolean fallback;

    public static InterviewFollowUpRuleDecision noFollowUp(
            int resolvedMaxFollowUp,
            String reasonCode,
            String reasonText,
            String chainId,
            String ruleVersion,
            boolean fallback) {
        InterviewFollowUpRuleDecision decision = new InterviewFollowUpRuleDecision();
        decision.setNeedFollowUp(false);
        decision.setResolvedMaxFollowUp(resolvedMaxFollowUp);
        decision.setReasonCode(reasonCode);
        decision.setReasonText(reasonText);
        decision.setChainId(chainId);
        decision.setRuleVersion(ruleVersion);
        decision.setFallback(fallback);
        return decision;
    }
}
