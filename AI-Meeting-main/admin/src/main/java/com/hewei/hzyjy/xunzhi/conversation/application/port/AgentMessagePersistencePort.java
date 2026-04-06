package com.hewei.hzyjy.xunzhi.conversation.application.port;

import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AgentMessagePersistencePort {

    List<AgentMessage> findHistoryBySessionId(String sessionId);

    Page<AgentMessage> pageHistoryBySessionId(String sessionId, Pageable pageable);

    Page<AgentMessage> pageHistoryBySessionIds(List<String> sessionIds, Pageable pageable);

    Page<AgentMessage> pageAllHistory(Pageable pageable);

    void save(AgentMessage message);
}
