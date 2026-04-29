package com.hewei.hzyjy.xunzhi.interview.application.runtime;

/**
 * 定义面试会话运行态懒恢复的范围枚举，
 * 用于区分只恢复流程、得分、回放、材料或整包运行态等不同场景。
 *
 * @author 程序员牛肉
 */
public enum InterviewRuntimeRehydrateScope {

    FLOW_ONLY,
    SCORE_ONLY,
    PLAYBACK_ONLY,
    MATERIAL_ONLY,
    HOT_RUNTIME,
    FULL_RUNTIME;

    public boolean includesQuestionMaterial() {
        return switch (this) {
            case FLOW_ONLY, MATERIAL_ONLY, HOT_RUNTIME, FULL_RUNTIME -> true;
            default -> false;
        };
    }

    public boolean includesSuggestionMaterial() {
        return this == MATERIAL_ONLY || this == FULL_RUNTIME;
    }

    public boolean includesResumeMaterial() {
        return this == MATERIAL_ONLY || this == FULL_RUNTIME;
    }

    public boolean includesFlow() {
        return this == FLOW_ONLY || this == HOT_RUNTIME || this == FULL_RUNTIME;
    }

    public boolean includesFollowUpQuestions() {
        return this == FLOW_ONLY || this == HOT_RUNTIME || this == FULL_RUNTIME;
    }

    public boolean includesScore() {
        return this == SCORE_ONLY || this == HOT_RUNTIME || this == FULL_RUNTIME;
    }

    public boolean includesTurns() {
        return this == PLAYBACK_ONLY || this == HOT_RUNTIME || this == FULL_RUNTIME;
    }

    public boolean includesRequestIds() {
        return includesTurns();
    }
}
