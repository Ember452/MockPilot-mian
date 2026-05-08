package com.hewei.hzyjy.xunzhi.interview.service.cache;

public final class InterviewCacheKeys {

    private static final String QUESTIONS_KEY = "interview:questions:session:";
    private static final String SUGGESTIONS_KEY = "interview:suggestions:session:";
    private static final String RESUME_SCORE_KEY = "interview:resume_score:session:";
    private static final String RESUME_CONTEXT_KEY = "interview:resume_context:session:";
    private static final String DEMEANOR_SCORE_KEY = "interview:demeanor_score:session:";
    private static final String DIRECTION_KEY = "interview:direction:session:";
    private static final String FLOW_KEY = "interview:flow:session:";
    private static final String FOLLOW_UP_QUESTIONS_KEY = "interview:follow_up_questions:session:";
    private static final String ANSWER_REQUEST_KEY = "interview:answer:req:session:";
    private static final String TURNS_KEY = "interview:turns:session:";
    private static final String TURN_REQUEST_KEY = "interview:turn:req:session:";
    private static final String SESSION_SCORE_KEY = "interview:score:session:";
    private static final String SESSION_SCORE_SUM_KEY = "interview:score_sum:session:";
    private static final String SESSION_SCORE_COUNT_KEY = "interview:score_count:session:";
    private static final String DEMEANOR_PANIC_PREFIX = "demeanor:panic:";
    private static final String DEMEANOR_SERIOUSNESS_PREFIX = "demeanor:seriousness:";
    private static final String DEMEANOR_EMOTICON_PREFIX = "demeanor:emoticon:";
    private static final String DEMEANOR_COMPOSITE_PREFIX = "demeanor:composite:";

    private InterviewCacheKeys() {
    }

    public static String questions(String sessionId) {
        return QUESTIONS_KEY + sessionId;
    }

    public static String suggestions(String sessionId) {
        return SUGGESTIONS_KEY + sessionId;
    }

    public static String resumeScore(String sessionId) {
        return RESUME_SCORE_KEY + sessionId;
    }

    public static String resumeContext(String sessionId) {
        return RESUME_CONTEXT_KEY + sessionId;
    }

    public static String demeanorScore(String sessionId) {
        return DEMEANOR_SCORE_KEY + sessionId;
    }

    public static String direction(String sessionId) {
        return DIRECTION_KEY + sessionId;
    }

    public static String flow(String sessionId) {
        return FLOW_KEY + sessionId;
    }

    public static String followUpQuestions(String sessionId) {
        return FOLLOW_UP_QUESTIONS_KEY + sessionId;
    }

    public static String answerRequest(String sessionId) {
        return ANSWER_REQUEST_KEY + sessionId;
    }

    public static String turns(String sessionId) {
        return TURNS_KEY + sessionId;
    }

    public static String turnRequest(String sessionId) {
        return TURN_REQUEST_KEY + sessionId;
    }

    public static String sessionScore(String sessionId) {
        return SESSION_SCORE_KEY + sessionId;
    }

    public static String sessionScoreSum(String sessionId) {
        return SESSION_SCORE_SUM_KEY + sessionId;
    }

    public static String sessionScoreCount(String sessionId) {
        return SESSION_SCORE_COUNT_KEY + sessionId;
    }

    public static String demeanorPanic(String sessionId) {
        return DEMEANOR_PANIC_PREFIX + sessionId;
    }

    public static String demeanorSeriousness(String sessionId) {
        return DEMEANOR_SERIOUSNESS_PREFIX + sessionId;
    }

    public static String demeanorEmoticon(String sessionId) {
        return DEMEANOR_EMOTICON_PREFIX + sessionId;
    }

    public static String demeanorComposite(String sessionId) {
        return DEMEANOR_COMPOSITE_PREFIX + sessionId;
    }
}

