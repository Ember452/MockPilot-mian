package com.hewei.hzyjy.xunzhi.interview.application.rule.node;

import com.hewei.hzyjy.xunzhi.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("followUpDecisionFinalize")
public class FollowUpDecisionFinalizeNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        context.finalizeDecision(false);
    }
}
