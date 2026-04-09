package com.hewei.hzyjy.xunzhi.agent.application;

import java.util.Arrays;
import java.util.List;

public enum BusinessAgentScene {

    GENERAL_AGENT_CHAT("general-agent-chat", "通用智能体"),
    INTERVIEW_QUESTION_EXTRACTION("interview-question-extraction", "面试出题官", "面试题出题官"),
    INTERVIEW_ANSWER_EVALUATION("interview-answer-evaluation", "用户答案评分官", "面试答案评分官"),
    INTERVIEW_DEMEANOR("interview-demeanor", "神态分析官", "神态评分面试官", "表情分析面试官"),
    INTERVIEW_QUESTION_ASKING("interview-question-asking", "面试提问官");

    private final String code;

    private final String defaultAgentName;

    private final List<String> candidateAgentNames;

    BusinessAgentScene(String code, String defaultAgentName, String... aliasAgentNames) {
        this.code = code;
        this.defaultAgentName = defaultAgentName;
        this.candidateAgentNames = Arrays.asList(buildCandidateNames(defaultAgentName, aliasAgentNames));
    }

    public String getCode() {
        return code;
    }

    public String getDefaultAgentName() {
        return defaultAgentName;
    }

    public List<String> getCandidateAgentNames() {
        return candidateAgentNames;
    }

    private static String[] buildCandidateNames(String defaultAgentName, String... aliasAgentNames) {
        String[] names = new String[aliasAgentNames.length + 1];
        names[0] = defaultAgentName;
        System.arraycopy(aliasAgentNames, 0, names, 1, aliasAgentNames.length);
        return names;
    }
}
