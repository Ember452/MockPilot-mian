package com.hewei.hzyjy.xunzhi.agent.application;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentConversation;
import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentPropertiesDO;
import com.hewei.hzyjy.xunzhi.agent.dao.repository.AgentConversationRepository;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.AgentPropertiesLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentResolver {

    private final AgentConversationRepository agentConversationRepository;
    private final AgentPropertiesLoader agentPropertiesLoader;

    public Long resolveAgentId(String sessionId, Long requestedAgentId) {
        Long boundAgentId = findBoundAgentId(sessionId);
        if (boundAgentId != null) {
            if (requestedAgentId != null && !requestedAgentId.equals(boundAgentId)) {
                log.warn(
                        "Requested agentId {} overridden by session-bound agentId {}, sessionId={}",
                        requestedAgentId,
                        boundAgentId,
                        sessionId
                );
            }
            return boundAgentId;
        }
        return requestedAgentId;
    }

    public AgentPropertiesDO resolveAgent(String sessionId, Long requestedAgentId) {
        Long resolvedAgentId = resolveAgentId(sessionId, requestedAgentId);
        if (resolvedAgentId == null) {
            return null;
        }
        return agentPropertiesLoader.getByAgentId(resolvedAgentId);
    }

    public Long findBoundAgentId(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return null;
        }
        return agentConversationRepository.findBySessionIdAndDelFlag(sessionId, 0)
                .map(AgentConversation::getAgentId)
                .orElse(null);
    }
}
