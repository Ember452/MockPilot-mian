package com.hewei.hzyjy.xunzhi.interview.application.rule.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("missingPointsJudge")
public class MissingPointsJudgeNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        if (context.isTerminated()) {
            return;
        }
        boolean hasMissingPoints = CollUtil.isNotEmpty(context.getMissingPoints());
        boolean hasFollowUpHint = StrUtil.isNotBlank(context.getFollowUpQuestionHint());
        if (hasMissingPoints || hasFollowUpHint) {
            context.markNeedFollowUp("MISSING_POINTS", "missing points or follow-up hint detected");
        }
    }
}
