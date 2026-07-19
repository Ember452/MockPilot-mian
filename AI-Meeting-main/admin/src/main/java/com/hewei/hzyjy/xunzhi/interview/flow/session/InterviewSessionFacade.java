package com.hewei.hzyjy.xunzhi.interview.flow.session;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.common.enums.InterviewErrorCodeEnum;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.DemeanorEvaluationReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.InterviewAnswerReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.InterviewConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.InterviewQuestionReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewAnswerRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewConversationRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewQuestionRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewRecordRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewSessionCreateRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewSessionRestoreRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.RadarChartDTO;
import com.hewei.hzyjy.xunzhi.interview.application.InterviewWorkflowService;
import com.hewei.hzyjy.xunzhi.interview.application.runtime.InterviewSessionRuntimeRehydrateService;
import com.hewei.hzyjy.xunzhi.interview.application.runtime.InterviewRuntimeRehydrateScope;
import com.hewei.hzyjy.xunzhi.interview.application.runtime.InterviewSessionRuntimeSnapshotService;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewQuestion;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewSession;
import com.hewei.hzyjy.xunzhi.interview.flow.report.InterviewResumePreviewService;
import com.hewei.hzyjy.xunzhi.interview.service.InterviewQuestionCacheService;
import com.hewei.hzyjy.xunzhi.interview.service.InterviewQuestionService;
import com.hewei.hzyjy.xunzhi.interview.service.InterviewRecordService;
import com.hewei.hzyjy.xunzhi.interview.service.InterviewSessionService;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewRuntimeLoadMode;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewSessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 面试会话统一门面服务，对外暴露面试全流程API。
 * 负责会话创建、简历提取、答题、神态评估、会话恢复等核心操作的编排，
 * 将前端请求路由到对应的子服务，不包含具体业务逻辑。
 *
 * @author 系统开发团队
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewSessionFacade {

    private final InterviewWorkflowService interviewWorkflowService;
    private final InterviewQuestionCacheService interviewQuestionCacheService;
    private final InterviewQuestionService interviewQuestionService;
    private final InterviewRecordService interviewRecordService;
    private final InterviewResumePreviewService interviewResumePreviewService;
    private final InterviewSessionService interviewSessionService;
    private final InterviewSessionRuntimeSnapshotService runtimeSnapshotService;
    private final InterviewSessionRuntimeRehydrateService runtimeRehydrateService;

    /**
     * 创建新的面试会话，初始化状态为草稿。
     */
    public InterviewSessionCreateRespDTO createSession(Long userId) {
        return interviewSessionService.createSession(userId);
    }

    /**
     * 分页查询用户的面试会话列表，支持关键词搜索和状态筛选。
     */
    public IPage<InterviewConversationRespDTO> pageConversations(Long userId, InterviewConversationPageReqDTO requestParam) {
        return interviewSessionService.pageConversations(userId, requestParam);
    }

    public List<AgentMessageHistoryRespDTO> getConversationHistory(String sessionId, Long userId) {
        interviewSessionService.requireOwnedSession(sessionId, userId);
        throw new ClientException("interview history is not enabled", InterviewErrorCodeEnum.INTERVIEW_SESSION_INVALID_STATE);
    }

    public IPage<AgentMessageHistoryRespDTO> pageHistoryMessages(
            String sessionId,
            Integer current,
            Integer size,
            Long userId) {
        if (StrUtil.isNotBlank(sessionId)) {
            interviewSessionService.requireOwnedSession(sessionId, userId);
        }
        throw new ClientException("interview history paging is not enabled", InterviewErrorCodeEnum.INTERVIEW_SESSION_INVALID_STATE);
    }

    /**
     * 结束面试会话，统一走 finalize 收口：落记录 + 补齐会话结束态（内部含锁与重试）。
     */
    public void finishSession(String sessionId, Long userId) {
        interviewRecordService.saveInterviewRecordFromRedis(sessionId, userId);
    }

    /**
     * 结束对话，finishSession 的别名接口。
     */
    public void endConversation(String sessionId, Long userId) {
        finishSession(sessionId, userId);
    }

    /**
     * 从简历中提取面试题目。
     * 走上传简历->AI提取->结构化落库->状态推进的完整链路。
     * 提取成功推进到READY状态，失败回落到DRAFT状态。
     */
    public InterviewQuestionRespDTO extractInterviewQuestions(
            String sessionId,
            MultipartFile resumePdf,
            Long userId,
            String username) {
        // 1) 进入提取前先把会话打到“上传中”，避免并发接口误判状态。
        interviewSessionService.markResumeUploading(sessionId, userId);

        // 2) 组装提取请求并委托 workflow（内部会做上传、AI 调用、结构化落库）。
        InterviewQuestionReqDTO reqDTO = new InterviewQuestionReqDTO();
        reqDTO.setUserName(username);
        reqDTO.setSessionId(sessionId);
        reqDTO.setResumePdf(resumePdf);

        InterviewQuestionRespDTO response = interviewWorkflowService.extractInterviewQuestions(reqDTO);
        if (response != null && Integer.valueOf(1).equals(response.getIsSuccess())) {
            // 3) 提取成功后推进到 READY，并同步会话级简历地址与面试方向。
            interviewSessionService.markReady(
                    sessionId,
                    userId,
                    response.getResumeFileUrl(),
                    response.getInterviewType()
            );
            runtimeSnapshotService.refreshAfterQuestionExtraction(sessionId);
            return response;
        }

        // 3) 提取失败回落到 DRAFT，保留后续重试入口。
        interviewSessionService.markDraft(sessionId, userId);
        return response;
    }

    /**
     * 面试答题主入口。
     * 先校验会话可继续，首次答题推进到IN_PROGRESS，然后委托答题编排流水线处理。
     * 内部处理幂等/加锁/评估/推进等完整流程。
     */
    public InterviewAnswerRespDTO answerInterviewQuestion(
            String sessionId,
            InterviewAnswerReqDTO requestParam,
            Long userId) {
        // 1) 先校验会话可继续（归属 + 状态）。
        ensureInterviewCanProceed(sessionId, userId);
        // 2) 首次答题把 READY 提升到 IN_PROGRESS。
        interviewSessionService.markInProgressIfReady(sessionId, userId);
        // 3) 委托答题编排流水线，内部处理幂等/加锁/评估/推进。
        requestParam.setSessionId(sessionId);
        return interviewWorkflowService.answerInterviewQuestion(sessionId, requestParam);
    }

    /**
     * 获取下一道面试题目，不推进游标，仅返回当前题。
     */
    public InterviewAnswerRespDTO getNextQuestion(String sessionId, Long userId) {
        ensureInterviewCanProceed(sessionId, userId);
        interviewSessionService.markInProgressIfReady(sessionId, userId);
        return interviewWorkflowService.getNextQuestion(sessionId);
    }

    /**
     * 获取当前面试题目，不推进游标。
     */
    public InterviewAnswerRespDTO getCurrentQuestion(String sessionId, Long userId) {
        ensureInterviewCanProceed(sessionId, userId);
        InterviewAnswerRespDTO response = interviewWorkflowService.getCurrentQuestion(sessionId);
        if (response != null && Boolean.TRUE.equals(response.getIsSuccess()) && !Boolean.TRUE.equals(response.getFinished())) {
            interviewSessionService.markInProgressIfReady(sessionId, userId);
        }
        return response;
    }

    /**
     * 加载简历预览资源。
     */
    public InterviewResumePreviewService.ResumePreviewResource loadResumePreview(String sessionId, Long userId) {
        interviewSessionService.requireOwnedSession(sessionId, userId);
        return interviewResumePreviewService.loadResumePreview(sessionId);
    }

    /**
     * 恢复面试会话，返回会话状态、简历信息、面试方向、题目建议、简历评分等完整数据。
     * 优先级：会话主表 > question表 > 缓存回补。
     */
    public InterviewSessionRestoreRespDTO restoreInterviewSession(String sessionId, Long userId) {
        // 1) 先恢复会话主信息（状态、简历、方向等主字段）。
        InterviewSession session = interviewSessionService.requireOwnedSession(sessionId, userId);
        runtimeRehydrateService.ensureRuntime(sessionId, InterviewRuntimeLoadMode.READ_ONLY, InterviewRuntimeRehydrateScope.MATERIAL_ONLY);

        InterviewSessionRestoreRespDTO response = new InterviewSessionRestoreRespDTO();
        response.setSessionId(sessionId);
        response.setStatus(session.getStatus());
        response.setCanResume(isSessionResumable(session));
        response.setResumeFileUrl(session.getResumeFileUrl());
        response.setInterviewType(session.getInterviewType());

        // 2) 再用 question 表补齐 resume/interviewType/resumeScore，降低对缓存依赖。
        InterviewQuestion question = interviewQuestionService.getBySessionId(sessionId);
        if (question != null) {
            if (StrUtil.isBlank(response.getResumeFileUrl())) {
                response.setResumeFileUrl(question.getResumeFileUrl());
            }
            if (StrUtil.isBlank(response.getInterviewType())) {
                response.setInterviewType(question.getInterviewType());
            }
            response.setResumeScore(question.getResumeScore());
        }

        // 3) 最后回补缓存态字段（suggestions、resumeScore），保证前端恢复页可直接渲染。
        Map<String, String> suggestions = interviewQuestionCacheService.getSessionInterviewSuggestions(sessionId);
        if (suggestions == null || suggestions.isEmpty()) {
            interviewQuestionCacheService.loadInterviewSuggestionsFromDatabase(sessionId);
            suggestions = interviewQuestionCacheService.getSessionInterviewSuggestions(sessionId);
        }
        response.setSuggestions(suggestions);

        Integer resumeScore = interviewQuestionCacheService.getSessionResumeScore(sessionId);
        if (resumeScore == null) {
            interviewQuestionCacheService.loadResumeScoreFromDatabase(sessionId);
            resumeScore = interviewQuestionCacheService.getSessionResumeScore(sessionId);
        }
        if (resumeScore != null) {
            response.setResumeScore(resumeScore);
        }

        if (StrUtil.isBlank(response.getInterviewType()) && question != null) {
            response.setInterviewType(question.getInterviewType());
        }
        return response;
    }

    /**
     * 获取会话的面试题目列表，缓存miss时从数据库回补。
     */
    public Map<String, String> getSessionInterviewQuestions(String sessionId, Long userId) {
        interviewSessionService.requireOwnedSession(sessionId, userId);
        runtimeRehydrateService.ensureRuntime(sessionId, InterviewRuntimeLoadMode.READ_ONLY, InterviewRuntimeRehydrateScope.MATERIAL_ONLY);

        Map<String, String> questions = interviewQuestionCacheService.getSessionInterviewQuestions(sessionId);
        if (questions == null || questions.isEmpty()) {
            interviewQuestionCacheService.loadInterviewQuestionsFromDatabase(sessionId);
            questions = interviewQuestionCacheService.getSessionInterviewQuestions(sessionId);
        }
        return questions;
    }

    /**
     * 获取会话总分，读取顺序：缓存 > 记录快照，避免缓存丢失导致分数回退。
     */
    public Integer getSessionTotalScore(String sessionId, Long userId) {
        interviewSessionService.requireOwnedSession(sessionId, userId);
        runtimeRehydrateService.ensureRuntime(sessionId, InterviewRuntimeLoadMode.READ_ONLY, InterviewRuntimeRehydrateScope.SCORE_ONLY);
        // 分数读取顺序：缓存 > 记录快照，避免缓存丢失导致分数回退。
        Integer score = interviewQuestionCacheService.getSessionTotalScore(sessionId);
        if (score != null && score > 0) {
            return score;
        }
        InterviewRecordRespDTO record = interviewRecordService.getBySessionId(sessionId, userId);
        if (record != null && record.getInterviewScore() != null) {
            return record.getInterviewScore();
        }
        return score;
    }

    /**
     * 获取会话的面试建议列表，缓存miss时从数据库回补。
     */
    public Map<String, String> getSessionInterviewSuggestions(String sessionId, Long userId) {
        interviewSessionService.requireOwnedSession(sessionId, userId);
        runtimeRehydrateService.ensureRuntime(sessionId, InterviewRuntimeLoadMode.READ_ONLY, InterviewRuntimeRehydrateScope.MATERIAL_ONLY);

        Map<String, String> suggestions = interviewQuestionCacheService.getSessionInterviewSuggestions(sessionId);
        if (suggestions == null || suggestions.isEmpty()) {
            interviewQuestionCacheService.loadInterviewSuggestionsFromDatabase(sessionId);
            suggestions = interviewQuestionCacheService.getSessionInterviewSuggestions(sessionId);
        }
        return suggestions;
    }

    /**
     * 获取简历评分，缓存miss时从数据库回补。
     */
    public Integer getSessionResumeScore(String sessionId, Long userId) {
        interviewSessionService.requireOwnedSession(sessionId, userId);
        runtimeRehydrateService.ensureRuntime(sessionId, InterviewRuntimeLoadMode.READ_ONLY, InterviewRuntimeRehydrateScope.MATERIAL_ONLY);

        Integer resumeScore = interviewQuestionCacheService.getSessionResumeScore(sessionId);
        if (resumeScore == null) {
            interviewQuestionCacheService.loadResumeScoreFromDatabase(sessionId);
            resumeScore = interviewQuestionCacheService.getSessionResumeScore(sessionId);
        }
        return resumeScore;
    }

    /**
     * 获取雷达图数据，读取顺序：缓存 > 记录快照。
     */
    public RadarChartDTO getRadarChartData(String sessionId, Long userId) {
        interviewSessionService.requireOwnedSession(sessionId, userId);
        runtimeRehydrateService.ensureRuntime(sessionId, InterviewRuntimeLoadMode.READ_ONLY, InterviewRuntimeRehydrateScope.FULL_RUNTIME);
        RadarChartDTO radar = interviewQuestionCacheService.getRadarChartData(sessionId);
        if (hasRadarSignal(radar)) {
            return radar;
        }
        InterviewRecordRespDTO record = interviewRecordService.getBySessionId(sessionId, userId);
        if (record != null && record.getRadarChart() != null) {
            return record.getRadarChart();
        }
        return radar;
    }

    /**
     * 评估神态
     */
    public String evaluateDemeanor(
            String sessionId,
            MultipartFile userPhoto,
            String requestSessionId,
            Long userId,
            String username) {
        // 1) 神态评估只允许在可继续的会话上执行。
        ensureInterviewCanProceed(sessionId, userId);
        if (requestSessionId != null && !sessionId.equals(requestSessionId)) {
            throw new ClientException("sessionId mismatch between path and request parameter");
        }

        // 2) 组装请求并委托 workflow（内部处理上传、AI 评估与分值落缓存）。
        DemeanorEvaluationReqDTO reqDTO = new DemeanorEvaluationReqDTO();
        reqDTO.setUserName(username);
        reqDTO.setSessionId(sessionId);
        reqDTO.setUserPhoto(userPhoto);
        return interviewWorkflowService.evaluateDemeanor(reqDTO);
    }

    /**
     * 校验会话是否可继续：先校验归属，再校验状态是否可恢复。
     */
    private void ensureInterviewCanProceed(String sessionId, Long userId) {
        InterviewSession session = interviewSessionService.requireOwnedSession(sessionId, userId);
        if (session == null || !isSessionResumable(session)) {
            throw new ClientException(InterviewErrorCodeEnum.INTERVIEW_SESSION_INVALID_STATE);
        }
    }

    /**
     * 判断会话是否可恢复，只有活跃状态（READY/IN_PROGRESS等）才可恢复。
     */
    private boolean isSessionResumable(InterviewSession session) {
        if (session == null || StrUtil.isBlank(session.getStatus())) {
            return false;
        }
        try {
            return InterviewSessionStatus.valueOf(session.getStatus()).canResume();
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 判断雷达图是否有有效信号（至少一个维度 > 0）。
     */
    private boolean hasRadarSignal(RadarChartDTO radar) {
        if (radar == null) {
            return false;
        }
        return positive(radar.getResumeScore())
                || positive(radar.getInterviewPerformance())
                || positive(radar.getDemeanorEvaluation())
                || positive(radar.getProfessionalSkills())
                || positive(radar.getPotentialIndex());
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }
}