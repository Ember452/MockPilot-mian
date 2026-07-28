package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * embedding 模型兼容性校验三分支测试：legacy 放行 / 一致放行 / 不一致拒绝。
 */
class EmbeddingServiceTest {

    @Test
    void legacyBlankModelPasses() {
        assertDoesNotThrow(() ->
                EmbeddingService.validateEmbeddingCompatibility(null, "text-embedding-v4"));
        assertDoesNotThrow(() ->
                EmbeddingService.validateEmbeddingCompatibility("", "text-embedding-v4"));
        assertDoesNotThrow(() ->
                EmbeddingService.validateEmbeddingCompatibility("  ", "text-embedding-v4"));
    }

    @Test
    void sameModelPasses() {
        assertDoesNotThrow(() ->
                EmbeddingService.validateEmbeddingCompatibility("text-embedding-v4", "text-embedding-v4"));
    }

    @Test
    void mismatchedModelRejectedWithBothNames() {
        ClientException ex = assertThrows(ClientException.class, () ->
                EmbeddingService.validateEmbeddingCompatibility("text-embedding-v3", "text-embedding-v4"));

        assertTrue(ex.getErrorMessage().contains("text-embedding-v3"));
        assertTrue(ex.getErrorMessage().contains("text-embedding-v4"));
    }
}
