package com.hewei.hzyjy.xunzhi.conversation.application;

import com.hewei.hzyjy.xunzhi.common.biz.message.MessageSequenceAllocator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageSequenceService {

    private final MessageSequenceAllocator messageSequenceAllocator;

    public int nextAiMessageSeq(String sessionId) {
        return messageSequenceAllocator.nextAiMessageSeq(sessionId);
    }

    public int nextAgentMessageSeq(String sessionId) {
        return messageSequenceAllocator.nextAgentMessageSeq(sessionId);
    }
}
