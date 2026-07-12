package com.hewei.hzyjy.xunzhi.interview.application.rule.node;

import com.hewei.hzyjy.xunzhi.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * 追问次数上限守卫
 */
@LiteflowComponent("followUpLimitGuard")
public class FollowUpLimitGuardNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        if (context.isTerminated()) {
            return;
        }
        if (context.getFollowUpCount() >= context.getResolvedMaxFollowUp()) {
            context.markNoFollowUp("FOLLOW_UP_LIMIT_REACHED", "follow-up limit reached");
        }
    }
}
