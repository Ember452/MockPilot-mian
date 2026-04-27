package com.hewei.hzyjy.xunzhi.interview.application;

import com.hewei.hzyjy.xunzhi.interview.api.io.req.DemeanorEvaluationReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.InterviewAnswerReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.InterviewQuestionReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewAnswerRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewQuestionRespDTO;

public interface InterviewWorkflowService {

    InterviewQuestionRespDTO extractInterviewQuestions(InterviewQuestionReqDTO requestParam);

    InterviewAnswerRespDTO answerInterviewQuestion(String sessionId, InterviewAnswerReqDTO requestParam);

    InterviewAnswerRespDTO getNextQuestion(String sessionId);

    InterviewAnswerRespDTO getCurrentQuestion(String sessionId);

    String evaluateDemeanor(DemeanorEvaluationReqDTO requestParam);
}
