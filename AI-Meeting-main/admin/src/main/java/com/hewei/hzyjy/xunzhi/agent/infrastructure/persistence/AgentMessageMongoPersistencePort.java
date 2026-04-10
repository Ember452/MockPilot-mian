package com.hewei.hzyjy.xunzhi.agent.infrastructure.persistence;

import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentMessage;
import com.hewei.hzyjy.xunzhi.agent.dao.repository.AgentMessageRepository;
import com.hewei.hzyjy.xunzhi.conversation.application.port.AgentMessagePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentMessageMongoPersistencePort implements AgentMessagePersistencePort {

    private final AgentMessageRepository agentMessageRepository;

    @Override
    public List<AgentMessage> findHistoryBySessionId(String sessionId) {
        return agentMessageRepository.findBySessionIdAndDelFlagOrderByMessageSeqAsc(sessionId, 0);
    }

    @Override
    public Page<AgentMessage> pageHistoryBySessionId(String sessionId, Pageable pageable) {
        return agentMessageRepository.findBySessionIdAndDelFlagOrderByCreateTimeAsc(sessionId, 0, pageable);
    }

    @Override
    public Page<AgentMessage> pageHistoryBySessionIds(List<String> sessionIds, Pageable pageable) {
        return agentMessageRepository.findBySessionIdInAndDelFlagOrderByCreateTimeDesc(sessionIds, 0, pageable);
    }

    @Override
    public Page<AgentMessage> pageAllHistory(Pageable pageable) {
        return agentMessageRepository.findByDelFlagOrderByCreateTimeDesc(0, pageable);
    }

    @Override
    public void save(AgentMessage message) {
        agentMessageRepository.save(message);
    }
}
