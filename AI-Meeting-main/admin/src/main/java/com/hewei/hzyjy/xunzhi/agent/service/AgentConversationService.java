package com.hewei.hzyjy.xunzhi.agent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.agent.api.io.req.AgentConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentConversationRespDTO;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentSessionCreateRespDTO;

/**
 * 智能体会话服务接口
 */
public interface AgentConversationService {

    /**
     * 创建新会话
     */
    String createConversation(String username, Long agentId, String firstMessage);

    /**
     * 创建新会话并生成标题
     */
    AgentSessionCreateRespDTO createConversationWithTitle(String username, Long agentId, String firstMessage);

    /**
     * 分页查询用户会话列表
     */
    IPage<AgentConversationRespDTO> pageConversations(String username, AgentConversationPageReqDTO reqDTO);

    /**
     * 更新会话信息
     */
    void updateConversation(String sessionId, Integer messageCount, Integer totalTokens);

    /**
     * 结束会话
     */
    void endConversation(String sessionId);

    void endConversation(String sessionId, Long userId);
}
