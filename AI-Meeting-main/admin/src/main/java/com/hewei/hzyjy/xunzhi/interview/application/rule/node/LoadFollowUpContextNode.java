package com.hewei.hzyjy.xunzhi.interview.application.rule.node;

import com.hewei.hzyjy.xunzhi.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("loadFollowUpContext")
public class LoadFollowUpContextNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        int resolvedMax = context.getResolvedMaxFollowUp() > 0 ? context.getResolvedMaxFollowUp() : Math.max(context.getMaxFollowUp(), 1);
        context.setResolvedMaxFollowUp(resolvedMax);
    }
}
