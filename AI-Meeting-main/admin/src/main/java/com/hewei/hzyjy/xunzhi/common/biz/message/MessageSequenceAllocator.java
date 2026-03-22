package com.hewei.hzyjy.xunzhi.common.biz.message;

import cn.hutool.core.util.StrUtil;
import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentMessage;
import com.hewei.hzyjy.xunzhi.agent.dao.repository.AgentMessageRepository;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiMessage;
import com.hewei.hzyjy.xunzhi.ai.dao.repository.AiMessageRepository;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

/**
 * Allocate per-session message sequence numbers atomically.
 */
@Component
@RequiredArgsConstructor
public class MessageSequenceAllocator {

    private static final String AGENT_SEQ_KEY_PREFIX = "xunzhi:msg-seq:agent:";
    private static final String AI_SEQ_KEY_PREFIX = "xunzhi:msg-seq:ai:";
    private static final Duration KEY_TTL = Duration.ofDays(7);
    private static final DefaultRedisScript<Long> INIT_AND_INCR_SCRIPT = new DefaultRedisScript<>();

    static {
        INIT_AND_INCR_SCRIPT.setResultType(Long.class);
        INIT_AND_INCR_SCRIPT.setScriptText(
                "local key = KEYS[1]\n"
                        + "local seed = tonumber(ARGV[1])\n"
                        + "if redis.call('EXISTS', key) == 0 then\n"
                        + "  redis.call('SET', key, seed)\n"
                        + "end\n"
                        + "return redis.call('INCR', key)"
        );
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final AgentMessageRepository agentMessageRepository;
    private final AiMessageRepository aiMessageRepository;

    public int nextAgentMessageSeq(String sessionId) {
        int latest = latestAgentMessageSeq(sessionId);
        return next(AGENT_SEQ_KEY_PREFIX, sessionId, latest);
    }

    public int nextAiMessageSeq(String sessionId) {
        int latest = latestAiMessageSeq(sessionId);
        return next(AI_SEQ_KEY_PREFIX, sessionId, latest);
    }

    private int latestAgentMessageSeq(String sessionId) {
        AgentMessage last = agentMessageRepository.findTopBySessionIdAndDelFlagOrderByMessageSeqDesc(sessionId, 0);
        if (last == null || last.getMessageSeq() == null) {
            return 0;
        }
        return last.getMessageSeq();
    }

    private int latestAiMessageSeq(String sessionId) {
        AiMessage last = aiMessageRepository.findTopBySessionIdAndDelFlagOrderByMessageSeqDesc(sessionId, 0);
        if (last == null || last.getMessageSeq() == null) {
            return 0;
        }
        return last.getMessageSeq();
    }

    private int next(String keyPrefix, String sessionId, int latestSeq) {
        if (StrUtil.isBlank(sessionId)) {
            throw new ClientException("sessionId cannot be empty when allocating message sequence");
        }

        String key = keyPrefix + sessionId;
        Long seq = stringRedisTemplate.execute(
                INIT_AND_INCR_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(Math.max(latestSeq, 0))
        );
        if (seq == null || seq <= 0) {
            throw new ClientException("failed to allocate message sequence");
        }

        stringRedisTemplate.expire(key, KEY_TTL);
        return Math.toIntExact(seq);
    }
}
