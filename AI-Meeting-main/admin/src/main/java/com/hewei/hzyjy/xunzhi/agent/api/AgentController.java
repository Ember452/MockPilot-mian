package com.hewei.hzyjy.xunzhi.agent.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.agent.api.io.req.AgentConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.agent.api.io.req.AgentSessionCreateReqDTO;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentConversationRespDTO;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentMessageHistoryRespDTO;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentSessionCreateRespDTO;
import com.hewei.hzyjy.xunzhi.agent.application.BusinessAgentResolver;
import com.hewei.hzyjy.xunzhi.agent.application.BusinessAgentScene;
import com.hewei.hzyjy.xunzhi.agent.service.AgentConversationService;
import com.hewei.hzyjy.xunzhi.agent.service.AgentMessageService;
import com.hewei.hzyjy.xunzhi.common.convention.annotation.CurrentUser;
import com.hewei.hzyjy.xunzhi.common.convention.context.UserContext;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.user.api.io.req.UserMessageReqDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.util.List;

/**
 *Agent文字聊天接口
 * @author nageoffer
 * @date 2023/9/27
 */
@Slf4j
@RestController
@RequestMapping("/api/xunzhi/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final BusinessAgentResolver businessAgentResolver;
    private final AgentMessageService agentMessageService;
    private final AgentConversationService agentConversationService;


    /**
     * 创建Agent会话
     * @param requestParam 会话创建请求参数
     * @return 会话ID和标题
     */
    @PostMapping("/sessions")
    public Result<AgentSessionCreateRespDTO> createSession(
            @RequestBody AgentSessionCreateReqDTO requestParam,
            @CurrentUser UserContext currentUser) {
        AgentSessionCreateRespDTO result = agentConversationService.createConversationWithTitle(
                currentUser.getUsername(),
                businessAgentResolver.resolveRequired(BusinessAgentScene.GENERAL_AGENT_CHAT).getId(),
                requestParam.getFirstMessage()
        );

        return Results.success(result);
    }

    /**
     * Agent文字聊天SSE接口
     * @return SSE流
     */
    @PostMapping("/sessions/{sessionId}/chat")
    public SseEmitter chat(
            @PathVariable String sessionId,
            @RequestBody UserMessageReqDTO requestParam,
            @CurrentUser UserContext currentUser) {
        if (currentUser != null) {
            requestParam.setUserName(currentUser.getUsername());
        }
        requestParam.setSessionId(sessionId);
        return agentMessageService.agentChatSse(requestParam, currentUser.getUserId());
    }


    /**
     * 分页查询用户会话列表
     */
    @GetMapping("/conversations")
    public Result<IPage<AgentConversationRespDTO>> pageConversations(
            AgentConversationPageReqDTO requestParam,
            @CurrentUser UserContext currentUser) {
        return Results.success(agentConversationService.pageConversations(currentUser.getUsername(), requestParam));
    }

    /**
     * 查询会话历史消息
     */
    @GetMapping("/conversations/{sessionId}/messages")
    public Result<List<AgentMessageHistoryRespDTO>> getConversationHistory(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(agentMessageService.getConversationHistory(sessionId, currentUser.getUserId()));
    }

    /**
     * 分页查询历史消息
     */
    @GetMapping("/messages/history")
    public Result<IPage<AgentMessageHistoryRespDTO>> pageHistoryMessages(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @CurrentUser UserContext currentUser) {
        return Results.success(agentMessageService.pageHistoryMessages(sessionId, current, size, currentUser.getUserId()));
    }

    /**
     * 结束会话
     */
    @PutMapping("/conversations/{sessionId}/end")
    public Result<Void> endConversation(@PathVariable String sessionId,
                                        @CurrentUser UserContext currentUser) {
        agentConversationService.endConversation(sessionId, currentUser.getUserId());
        return Results.success();
    }

}
