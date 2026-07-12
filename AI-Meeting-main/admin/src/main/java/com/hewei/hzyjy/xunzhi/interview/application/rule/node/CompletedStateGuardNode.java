package com.hewei.hzyjy.xunzhi.interview.application.rule.node;

import com.hewei.hzyjy.xunzhi.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * 面试完成守卫，如果面试已经结束，就终止整个链，拒绝追问
 */
@LiteflowComponent("completedStateGuard")
public class CompletedStateGuardNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        if (context.isInterviewCompleted()) {
            context.markNoFollowUp("INTERVIEW_COMPLETED", "interview already completed");
        }
    }
}
