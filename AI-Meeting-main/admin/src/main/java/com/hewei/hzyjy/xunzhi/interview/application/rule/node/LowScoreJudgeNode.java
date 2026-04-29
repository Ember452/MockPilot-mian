package com.hewei.hzyjy.xunzhi.interview.application.rule.node;

import com.hewei.hzyjy.xunzhi.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("lowScoreJudge")
public class LowScoreJudgeNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        if (context.isTerminated()) {
            return;
        }
        Integer score = context.getScore();
        if (score != null && score < context.getLowScoreThreshold()) {
            context.markNeedFollowUp(
                    "LOW_SCORE",
                    "score below threshold: " + score + " < " + context.getLowScoreThreshold()
            );
        }
    }
}
