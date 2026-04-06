package com.hewei.hzyjy.xunzhi.conversation.application;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.BaseMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.conversation.application.port.AgentMessagePersistencePort;
import com.hewei.hzyjy.xunzhi.conversation.application.port.AiMessagePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class ConversationMessageHistoryService {

    private final AiMessagePersistencePort aiMessagePersistencePort;
    private final AgentMessagePersistencePort agentMessagePersistencePort;

    public List<AiMessageHistoryRespDTO> listAiHistory(String sessionId) {
        return mapList(aiMessagePersistencePort.findHistoryBySessionId(sessionId), AiMessageHistoryRespDTO::new);
    }

    public List<AgentMessageHistoryRespDTO> listAgentHistory(String sessionId) {
        return mapList(agentMessagePersistencePort.findHistoryBySessionId(sessionId), AgentMessageHistoryRespDTO::new);
    }

    public IPage<AiMessageHistoryRespDTO> pageAiHistory(String sessionId, Integer current, Integer size) {
        Pageable pageable = PageRequest.of(current - 1, size);
        return mapPage(aiMessagePersistencePort.pageHistoryBySessionId(sessionId, pageable), current, size,
                AiMessageHistoryRespDTO::new);
    }

    public IPage<AiMessageHistoryRespDTO> pageAiHistory(List<String> sessionIds, Integer current, Integer size) {
        Pageable pageable = PageRequest.of(current - 1, size);
        return mapPage(aiMessagePersistencePort.pageHistoryBySessionIds(sessionIds, pageable), current, size,
                AiMessageHistoryRespDTO::new);
    }

    public IPage<AiMessageHistoryRespDTO> pageAllAiHistory(Integer current, Integer size) {
        Pageable pageable = PageRequest.of(current - 1, size);
        return mapPage(aiMessagePersistencePort.pageAllHistory(pageable), current, size, AiMessageHistoryRespDTO::new);
    }

    public IPage<AgentMessageHistoryRespDTO> pageAgentHistory(String sessionId, Integer current, Integer size) {
        Pageable pageable = PageRequest.of(current - 1, size);
        return mapPage(agentMessagePersistencePort.pageHistoryBySessionId(sessionId, pageable), current, size,
                AgentMessageHistoryRespDTO::new);
    }

    public IPage<AgentMessageHistoryRespDTO> pageAgentHistory(List<String> sessionIds, Integer current, Integer size) {
        Pageable pageable = PageRequest.of(current - 1, size);
        return mapPage(agentMessagePersistencePort.pageHistoryBySessionIds(sessionIds, pageable), current, size,
                AgentMessageHistoryRespDTO::new);
    }

    public IPage<AgentMessageHistoryRespDTO> pageAllAgentHistory(Integer current, Integer size) {
        Pageable pageable = PageRequest.of(current - 1, size);
        return mapPage(agentMessagePersistencePort.pageAllHistory(pageable), current, size,
                AgentMessageHistoryRespDTO::new);
    }

    private <S, T extends BaseMessageHistoryRespDTO> List<T> mapList(List<S> source, Supplier<T> targetSupplier) {
        List<T> result = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return result;
        }
        for (S item : source) {
            T target = targetSupplier.get();
            BeanUtils.copyProperties(item, target);
            result.add(target);
        }
        return result;
    }

    private <S, T extends BaseMessageHistoryRespDTO> IPage<T> mapPage(
            org.springframework.data.domain.Page<S> source,
            Integer current,
            Integer size,
            Supplier<T> targetSupplier) {
        Page<T> target = new Page<>(current, size);
        target.setTotal(source.getTotalElements());
        target.setRecords(mapList(source.getContent(), targetSupplier));
        return target;
    }
}
