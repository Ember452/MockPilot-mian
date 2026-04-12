package com.hewei.hzyjy.xunzhi.agent.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hewei.hzyjy.xunzhi.agent.api.io.req.AgentConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentConversationRespDTO;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentSessionCreateRespDTO;
import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentConversation;
import com.hewei.hzyjy.xunzhi.agent.dao.repository.AgentConversationRepository;
import com.hewei.hzyjy.xunzhi.agent.service.AgentConversationService;
import com.hewei.hzyjy.xunzhi.auth.application.CurrentUserService;
import com.hewei.hzyjy.xunzhi.conversation.application.ConversationOwnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentConversationServiceImpl implements AgentConversationService {

    private final AgentConversationRepository agentConversationRepository;
    private final CurrentUserService currentUserService;
    private final ConversationOwnershipService conversationOwnershipService;

    @Override
    public String createConversation(String username, Long agentId, String firstMessage) {
        Long userId = getUserIdByUsername(username);
        String sessionId = IdUtil.getSnowflakeNextIdStr();

        AgentConversation conversation = new AgentConversation();
        conversation.setSessionId(sessionId);
        conversation.setUserId(userId);
        conversation.setAgentId(agentId);
        conversation.setConversationTitle(generateTitle(firstMessage));
        conversation.setMessageCount(0);
        conversation.setTotalTokens(0);
        conversation.setStatus(1);
        conversation.setDelFlag(0);

        agentConversationRepository.save(conversation);
        return sessionId;
    }

    @Override
    public AgentSessionCreateRespDTO createConversationWithTitle(String username, Long agentId, String firstMessage) {
        String sessionId = createConversation(username, agentId, firstMessage);
        String title = generateTitle(firstMessage);
        return new AgentSessionCreateRespDTO(sessionId, title);
    }

    @Override
    public IPage<AgentConversationRespDTO> pageConversations(String username, AgentConversationPageReqDTO reqDTO) {
        Long userId = getUserIdByUsername(username);
        Pageable pageable = PageRequest.of(reqDTO.getCurrent() - 1, reqDTO.getSize());
        org.springframework.data.domain.Page<AgentConversation> conversationPage = queryConversations(userId, reqDTO, pageable);

        Page<AgentConversationRespDTO> resultPage = new Page<>(reqDTO.getCurrent(), reqDTO.getSize());
        resultPage.setTotal(conversationPage.getTotalElements());
        resultPage.setRecords(
                conversationPage.getContent().stream()
                        .map(conversation -> {
                            AgentConversationRespDTO respDTO = new AgentConversationRespDTO();
                            BeanUtils.copyProperties(conversation, respDTO);
                            return respDTO;
                        })
                        .collect(Collectors.toList())
        );
        return resultPage;
    }

    @Override
    public void updateConversation(String sessionId, Integer messageCount, Integer totalTokens) {
        agentConversationRepository.findBySessionIdAndDelFlag(sessionId, 0)
                .ifPresent(conversation -> {
                    conversation.setMessageCount(messageCount);
                    conversation.setTotalTokens(totalTokens);
                    agentConversationRepository.save(conversation);
                });
    }

    @Override
    public void endConversation(String sessionId) {
        agentConversationRepository.findBySessionIdAndDelFlag(sessionId, 0)
                .ifPresent(conversation -> {
                    conversation.setStatus(2);
                    agentConversationRepository.save(conversation);
                });
    }

    @Override
    public void endConversation(String sessionId, Long userId) {
        AgentConversation conversation = conversationOwnershipService.requireOwnedConversation(sessionId, userId);
        conversation.setStatus(2);
        agentConversationRepository.save(conversation);
    }

    private org.springframework.data.domain.Page<AgentConversation> queryConversations(
            Long userId,
            AgentConversationPageReqDTO reqDTO,
            Pageable pageable) {
        if (StrUtil.isNotBlank(reqDTO.getKeyword())) {
            return queryByKeyword(userId, reqDTO, pageable);
        }
        if (reqDTO.getStatus() != null) {
            return agentConversationRepository.findByUserIdAndStatusAndDelFlagOrderByUpdateTimeDesc(
                    userId,
                    reqDTO.getStatus(),
                    0,
                    pageable
            );
        }
        return agentConversationRepository.findByUserIdAndDelFlagOrderByUpdateTimeDesc(userId, 0, pageable);
    }

    private org.springframework.data.domain.Page<AgentConversation> queryByKeyword(
            Long userId,
            AgentConversationPageReqDTO reqDTO,
            Pageable pageable) {
        String keyword = reqDTO.getKeyword().trim();
        if (reqDTO.getStatus() != null) {
            return agentConversationRepository.findByUserIdAndStatusAndDelFlagAndTitleContaining(
                    userId,
                    reqDTO.getStatus(),
                    0,
                    keyword,
                    pageable
            );
        }
        return agentConversationRepository.findByUserIdAndDelFlagAndTitleContaining(userId, 0, keyword, pageable);
    }

    private String generateTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.trim().isEmpty()) {
            return "New Conversation";
        }
        String title = firstMessage.trim();
        if (title.length() > 20) {
            title = title.substring(0, 20) + "...";
        }
        return title;
    }

    private Long getUserIdByUsername(String username) {
        return currentUserService.getUserIdByUsername(username);
    }
}
