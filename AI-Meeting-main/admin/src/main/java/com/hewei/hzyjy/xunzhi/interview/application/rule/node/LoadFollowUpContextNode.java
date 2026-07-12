package com.hewei.hzyjy.xunzhi.interview.application.rule.node;

import com.hewei.hzyjy.xunzhi.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * 上下文加载节点
 * 标准化最大追问次数，如果外界传了就沿用，没有的话就用默认兜底
 *
 */
@LiteflowComponent("loadFollowUpContext")
public class LoadFollowUpContextNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        int resolvedMax = context.getResolvedMaxFollowUp() > 0 ? context.getResolvedMaxFollowUp() : Math.max(context.getMaxFollowUp(), 1);
        context.setResolvedMaxFollowUp(resolvedMax);
    }
}
