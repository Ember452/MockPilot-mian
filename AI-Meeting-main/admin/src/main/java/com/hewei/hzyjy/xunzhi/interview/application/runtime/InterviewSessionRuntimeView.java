package com.hewei.hzyjy.xunzhi.interview.application.runtime;

import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewSessionRuntimeColdSnapshot;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewSessionRuntimeHotSnapshot;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewSessionRuntimeSnapshot;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewRuntimeConfidence;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewRuntimeLoadMode;
import lombok.Builder;
import lombok.Data;

/**
 * 定义面试会话运行态恢复后的统一视图对象，
 * 用于承载恢复置信度、加载模式、恢复来源以及快照上下文等信息。
 *
 * @author 程序员牛肉
 */
@Data
@Builder
public class InterviewSessionRuntimeView {

    private InterviewRuntimeConfidence confidence;

    private InterviewRuntimeLoadMode loadMode;

    private InterviewRuntimeRestoreSource restoreSource;

    private boolean cacheRebuilt;

    private InterviewSessionRuntimeHotSnapshot hotSnapshot;

    private InterviewSessionRuntimeColdSnapshot coldSnapshot;

    private InterviewSessionRuntimeSnapshot snapshot;

    public boolean canWrite() {
        return confidence == InterviewRuntimeConfidence.EXACT || confidence == InterviewRuntimeConfidence.DERIVED;
    }

    public boolean isTerminal() {
        return confidence == InterviewRuntimeConfidence.TERMINAL;
    }
}
