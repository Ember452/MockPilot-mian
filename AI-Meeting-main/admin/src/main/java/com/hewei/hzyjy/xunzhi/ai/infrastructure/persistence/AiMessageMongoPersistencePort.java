package com.hewei.hzyjy.xunzhi.ai.infrastructure.persistence;

import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiMessage;
import com.hewei.hzyjy.xunzhi.ai.dao.repository.AiMessageRepository;
import com.hewei.hzyjy.xunzhi.conversation.application.port.AiMessagePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiMessageMongoPersistencePort implements AiMessagePersistencePort {

    private final AiMessageRepository aiMessageRepository;

    @Override
    public List<AiMessage> findHistoryBySessionId(String sessionId) {
        return aiMessageRepository.findBySessionIdAndDelFlagOrderByMessageSeqAsc(sessionId, 0);
    }

    @Override
    public Page<AiMessage> pageHistoryBySessionId(String sessionId, Pageable pageable) {
        return aiMessageRepository.findBySessionIdAndDelFlagOrderByCreateTimeAsc(sessionId, 0, pageable);
    }

    @Override
    public Page<AiMessage> pageHistoryBySessionIds(List<String> sessionIds, Pageable pageable) {
        return aiMessageRepository.findBySessionIdInAndDelFlagOrderByCreateTimeDesc(sessionIds, 0, pageable);
    }

    @Override
    public Page<AiMessage> pageAllHistory(Pageable pageable) {
        return aiMessageRepository.findByDelFlagOrderByCreateTimeDesc(0, pageable);
    }

    @Override
    public void save(AiMessage message) {
        aiMessageRepository.save(message);
    }
}
