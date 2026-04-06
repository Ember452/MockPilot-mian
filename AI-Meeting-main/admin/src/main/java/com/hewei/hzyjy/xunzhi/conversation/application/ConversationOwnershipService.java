package com.hewei.hzyjy.xunzhi.conversation.application;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentConversation;
import com.hewei.hzyjy.xunzhi.agent.dao.repository.AgentConversationRepository;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.common.enums.InterviewErrorCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Centralized conversation ownership checks shared across domains.
 */
@Component
@RequiredArgsConstructor
public class ConversationOwnershipService {

    private final AgentConversationRepository agentConversationRepository;

    public AgentConversation requireOwnedConversation(String sessionId, Long userId) {
        if (StrUtil.isBlank(sessionId)) {
            throw new ClientException(InterviewErrorCodeEnum.SESSION_ID_EMPTY);
        }
        validateUserId(userId);

        Optional<AgentConversation> conversationOpt = agentConversationRepository.findBySessionIdAndDelFlag(sessionId, 0);
        if (conversationOpt.isEmpty()) {
            throw new ClientException(InterviewErrorCodeEnum.CONVERSATION_NOT_FOUND);
        }
        AgentConversation conversation = conversationOpt.get();
        if (!userId.equals(conversation.getUserId())) {
            throw new ClientException(InterviewErrorCodeEnum.CONVERSATION_ACCESS_DENIED);
        }
        return conversation;
    }

    public List<String> listOwnedSessionIds(Long userId) {
        validateUserId(userId);
        List<AgentConversation> conversations = agentConversationRepository.findByUserIdAndDelFlag(userId, 0);
        if (conversations == null || conversations.isEmpty()) {
            return Collections.emptyList();
        }
        return conversations.stream()
                .map(AgentConversation::getSessionId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ClientException(InterviewErrorCodeEnum.INVALID_USER_ID);
        }
    }
}
