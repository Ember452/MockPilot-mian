package com.hewei.hzyjy.xunzhi.toolkit.xunfei;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AIContentAccumulator token 统计单测：usage 真值优先 / 字符估算两条路径。
 */
class AIContentAccumulatorTokenTest {

    private static byte[] sse(String json) {
        return ("data: " + json).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void usageFrameTakesPrecedence() {
        AIContentAccumulator accumulator = new AIContentAccumulator();
        accumulator.appendChunk(sse("{\"choices\":[{\"delta\":{\"content\":\"你好世界\"}}]}"));
        // 末帧携带 usage.total_tokens 真值
        accumulator.appendChunk(sse("{\"choices\":[],\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":23,\"total_tokens\":123}}"));

        assertEquals(123L, accumulator.getTotalTokens());
        assertFalse(accumulator.isTokenEstimated());
    }

    @Test
    void estimatesWhenNoUsageFrame() {
        AIContentAccumulator accumulator = new AIContentAccumulator();
        // 4 个 CJK 字符 + 8 个 ASCII 字符 → 4 + 8/4 = 6
        accumulator.appendChunk(sse("{\"choices\":[{\"delta\":{\"content\":\"你好世界\"}}]}"));
        accumulator.appendChunk(sse("{\"choices\":[{\"delta\":{\"content\":\"abcdefgh\"}}]}"));

        assertTrue(accumulator.isTokenEstimated());
        assertEquals(6L, accumulator.getTotalTokens());
    }

    @Test
    void estimateIncludesReasoningContent() {
        AIContentAccumulator accumulator = new AIContentAccumulator();
        accumulator.appendChunk(sse("{\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}"));
        accumulator.appendChunk(sse("{\"choices\":[{\"delta\":{\"reasoning_content\":\"思考中\"}}]}"));

        // content 2 CJK + reasoning 3 CJK
        assertEquals(5L, accumulator.getTotalTokens());
        assertTrue(accumulator.isTokenEstimated());
    }

    @Test
    void resetClearsUsageTruth() {
        AIContentAccumulator accumulator = new AIContentAccumulator();
        accumulator.appendChunk(sse("{\"choices\":[],\"usage\":{\"total_tokens\":88}}"));
        assertFalse(accumulator.isTokenEstimated());

        accumulator.reset();

        assertTrue(accumulator.isTokenEstimated());
        assertEquals(0L, accumulator.getTotalTokens());
    }

    @Test
    void estimateTokensFormula() {
        assertEquals(0L, AIContentAccumulator.estimateTokens(null));
        assertEquals(0L, AIContentAccumulator.estimateTokens(""));
        // 纯 CJK：1 字/token
        assertEquals(3L, AIContentAccumulator.estimateTokens("知识库"));
        // 纯 ASCII：4 字符/token 向上取整，5 字符 → 2
        assertEquals(2L, AIContentAccumulator.estimateTokens("abcde"));
        // 混合：2 CJK + 6 其他 → 2 + ceil(6/4)=2 → 4
        assertEquals(4L, AIContentAccumulator.estimateTokens("你好, RAG!"));
    }
}
