package com.hewei.hzyjy.xunzhi.ai.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hewei.hzyjy.xunzhi.ai.service.AiConversationService;
import com.hewei.hzyjy.xunzhi.common.convention.annotation.CurrentUser;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiConversationPageReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiSessionCreateReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiConversationRespDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiSessionCreateRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/xunzhi/v1/ai/conversations")
@RequiredArgsConstructor
public class AiConversationController {

    private final AiConversationService aiConversationService;

    @PostMapping
    public Result<AiSessionCreateRespDTO> createConversation(@RequestBody AiSessionCreateReqDTO requestParam, @CurrentUser String username) {
        AiSessionCreateRespDTO result = aiConversationService.createConversationWithTitle(
                username,
                requestParam.getAiId(),
                requestParam.getFirstMessage()
        );
        return Results.success(result);
    }

    @GetMapping
    public Result<IPage<AiConversationRespDTO>> pageConversations(
            AiConversationPageReqDTO requestParam,
            @CurrentUser String username) {
        IPage<AiConversationRespDTO> result = aiConversationService.pageConversations(username, requestParam);
        return Results.success(result);
    }

    @PutMapping("/{sessionId}")
    public Result<Void> updateConversation(@PathVariable String sessionId,
                                           @RequestParam(required = false) Integer messageCount,
                                           @RequestParam(required = false) String title,
                                           @CurrentUser String username) {
        aiConversationService.updateConversation(sessionId, messageCount, title, username);
        return Results.success();
    }

    @PutMapping("/{sessionId}/end")
    public Result<Void> endConversation(@PathVariable String sessionId,
                                        @CurrentUser String username) {
        aiConversationService.endConversation(sessionId, username);
        return Results.success();
    }

    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteConversation(@PathVariable String sessionId,
                                           @CurrentUser String username) {
        aiConversationService.deleteConversation(sessionId, username);
        return Results.success();
    }

    @DeleteMapping
    public Result<Void> clearAllConversations(@CurrentUser String username) {
        aiConversationService.clearAllConversations(username);
        return Results.success();
    }

    @GetMapping("/{sessionId}")
    public Result<AiConversationRespDTO> getConversationById(@PathVariable String sessionId,
                                                             @CurrentUser String username) {
        AiConversationRespDTO result = aiConversationService.getConversationBySessionId(sessionId, username);
        return Results.success(result);
    }
}
