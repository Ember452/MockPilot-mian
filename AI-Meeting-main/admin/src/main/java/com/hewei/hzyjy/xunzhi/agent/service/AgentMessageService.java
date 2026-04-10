package com.hewei.hzyjy.xunzhi.agent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.user.api.io.req.UserMessageReqDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AgentMessageService {

    SseEmitter agentChatSse(UserMessageReqDTO requestParam);

    SseEmitter agentChatSse(UserMessageReqDTO requestParam, Long userId);

    List<AgentMessageHistoryRespDTO> getConversationHistory(String sessionId);

    List<AgentMessageHistoryRespDTO> getConversationHistory(String sessionId, Long userId);

    IPage<AgentMessageHistoryRespDTO> pageHistoryMessages(String sessionId, Integer current, Integer size);

    IPage<AgentMessageHistoryRespDTO> pageHistoryMessages(String sessionId, Integer current, Integer size, Long userId);
}
