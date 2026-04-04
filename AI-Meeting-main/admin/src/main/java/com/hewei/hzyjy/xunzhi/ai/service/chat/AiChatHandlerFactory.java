package com.hewei.hzyjy.xunzhi.ai.service.chat;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

/**
 * AI聊天处理器工厂
 * 目前统一使用UniversalAiChatHandler处理所有兼容OpenAI协议的模型
 */
@Component
public class AiChatHandlerFactory {
    
    private final UniversalAiChatHandler universalAiChatHandler;

    public AiChatHandlerFactory(UniversalAiChatHandler universalAiChatHandler) {
        this.universalAiChatHandler = universalAiChatHandler;
    }

    public AiChatHandler getHandler(String aiType) {
        if (StrUtil.isBlank(aiType)) {
            return null;
        }
        // 通用处理器支持所有兼容OpenAI协议的模型
        if (universalAiChatHandler.supports(aiType)) {
            return universalAiChatHandler;
        }
        return null;
    }
}
