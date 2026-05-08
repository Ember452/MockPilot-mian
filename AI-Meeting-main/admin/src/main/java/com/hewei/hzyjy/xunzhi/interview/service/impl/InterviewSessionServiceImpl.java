package com.hewei.hzyjy.xunzhi.interview.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hewei.hzyjy.xunzhi.agent.application.BusinessAgentResolver;
import com.hewei.hzyjy.xunzhi.agent.application.BusinessAgentScene;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.InterviewConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewConversationRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewSessionCreateRespDTO;
import com.hewei.hzyjy.xunzhi.interview.application.runtime.InterviewSessionRuntimeSnapshotService;
import com.hewei.hzyjy.xunzhi.interview.application.InterviewSessionOwnershipService;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewSession;
import com.hewei.hzyjy.xunzhi.interview.dao.repository.InterviewSessionRepository;
import com.hewei.hzyjy.xunzhi.interview.service.InterviewSessionService;
import com.hewei.hzyjy.xunzhi.interview.service.model.InterviewSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewSessionOwnershipService ownershipService;
    private final BusinessAgentResolver businessAgentResolver;
    private final ObjectProvider<InterviewSessionRuntimeSnapshotService> runtimeSnapshotServiceProvider;

    @Override
    public InterviewSessionCreateRespDTO createSession(Long userId) {
        abandonActiveSessions(userId);

        InterviewSession session = new InterviewSession();
        session.setSessionId(IdUtil.getSnowflakeNextIdStr());
        session.setUserId(userId);
        session.setStatus(InterviewSessionStatus.DRAFT.name());
        session.setConversationTitle("Interview Session");
        session.setInterviewerAgentId(
                businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_QUESTION_ASKING).getId());
        session.setDelFlag(0);
        interviewSessionRepository.save(session);
        InterviewSessionRuntimeSnapshotService runtimeSnapshotService = runtimeSnapshotServiceProvider.getIfAvailable();
        if (runtimeSnapshotService != null) {
            runtimeSnapshotService.initializeDraftSnapshot(session);
        }
        return new InterviewSessionCreateRespDTO(session.getSessionId(), session.getStatus());
    }

    @Override
    public IPage<InterviewConversationRespDTO> pageConversations(Long userId, InterviewConversationPageReqDTO requestParam) {
        Pageable pageable = PageRequest.of(requestParam.getCurrent() - 1, requestParam.getSize());
        org.springframework.data.domain.Page<InterviewSession> sessionPage = queryPage(userId, requestParam, pageable);
        Page<InterviewConversationRespDTO> result = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        result.setTotal(sessionPage.getTotalElements());
        result.setRecords(sessionPage.getContent().stream().map(this::toRespDTO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public InterviewSession getBySessionId(String sessionId) {
        return interviewSessionRepository.findBySessionIdAndDelFlag(sessionId, 0).orElse(null);
    }

    @Override
    public InterviewSession requireOwnedSession(String sessionId, Long userId) {
        return ownershipService.requireOwnedSession(sessionId, userId);
    }

    @Override
    public void markResumeUploading(String sessionId, Long userId) {
        InterviewSession session = requireOwnedSession(sessionId, userId);
        session.setStatus(InterviewSessionStatus.RESUME_UPLOADING.name());
        interviewSessionRepository.save(session);
    }

    @Override
    public void markReady(String sessionId, Long userId, String resumeFileUrl, String interviewType) {
        InterviewSession session = requireOwnedSession(sessionId, userId);
        session.setStatus(InterviewSessionStatus.READY.name());
        session.setResumeFileUrl(resumeFileUrl);
        session.setInterviewType(interviewType);
        interviewSessionRepository.save(session);
    }

    @Override
    public void markDraft(String sessionId, Long userId) {
        InterviewSession session = requireOwnedSession(sessionId, userId);
        session.setStatus(InterviewSessionStatus.DRAFT.name());
        interviewSessionRepository.save(session);
    }

    @Override
    public void markInProgressIfReady(String sessionId, Long userId) {
        InterviewSession session = requireOwnedSession(sessionId, userId);
        if (!InterviewSessionStatus.READY.name().equals(session.getStatus())) {
            return;
        }
        session.setStatus(InterviewSessionStatus.IN_PROGRESS.name());
        if (session.getStartTime() == null) {
            session.setStartTime(new Date());
        }
        interviewSessionRepository.save(session);
    }

    @Override
    public void finishSession(String sessionId, Long userId) {
        InterviewSession session = requireOwnedSession(sessionId, userId);
        session.setStatus(InterviewSessionStatus.FINISHED.name());
        if (session.getStartTime() == null) {
            session.setStartTime(session.getCreateTime() == null ? new Date() : session.getCreateTime());
        }
        session.setEndTime(new Date());
        interviewSessionRepository.save(session);
    }

    @Override
    public void abandonActiveSessions(Long userId) {
        List<InterviewSession> sessions = interviewSessionRepository.findByUserIdAndStatusInAndDelFlagOrderByUpdateTimeDesc(
                userId,
                List.of(
                        InterviewSessionStatus.DRAFT.name(),
                        InterviewSessionStatus.RESUME_UPLOADING.name(),
                        InterviewSessionStatus.READY.name(),
                        InterviewSessionStatus.IN_PROGRESS.name()
                ),
                0
        );
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (InterviewSession session : sessions) {
            session.setStatus(InterviewSessionStatus.ABANDONED.name());
            session.setEndTime(new Date());
        }
        interviewSessionRepository.saveAll(sessions);
    }

    private org.springframework.data.domain.Page<InterviewSession> queryPage(
            Long userId,
            InterviewConversationPageReqDTO requestParam,
            Pageable pageable) {
        if (StrUtil.isNotBlank(requestParam.getKeyword())) {
            String keyword = requestParam.getKeyword().trim();
            if (StrUtil.isNotBlank(requestParam.getStatus())) {
                return interviewSessionRepository.findByUserIdAndStatusAndDelFlagAndTitleContaining(
                        userId,
                        requestParam.getStatus().trim(),
                        0,
                        keyword,
                        pageable
                );
            }
            return interviewSessionRepository.findByUserIdAndDelFlagAndTitleContaining(userId, 0, keyword, pageable);
        }
        if (StrUtil.isNotBlank(requestParam.getStatus())) {
            return interviewSessionRepository.findByUserIdAndStatusAndDelFlagOrderByUpdateTimeDesc(
                    userId,
                    requestParam.getStatus().trim(),
                    0,
                    pageable
            );
        }
        return interviewSessionRepository.findByUserIdAndDelFlagOrderByUpdateTimeDesc(userId, 0, pageable);
    }

    private InterviewConversationRespDTO toRespDTO(InterviewSession session) {
        InterviewConversationRespDTO respDTO = new InterviewConversationRespDTO();
        BeanUtils.copyProperties(session, respDTO);
        return respDTO;
    }
}
