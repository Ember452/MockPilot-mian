package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeBaseDO;
import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeDocument;
import com.hewei.hzyjy.xunzhi.knowledge.dao.mapper.KnowledgeBaseMapper;
import com.hewei.hzyjy.xunzhi.knowledge.dao.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 索引重建服务测试：锁互斥、串行重跑与 skipped 统计、force 换模型通道。
 */
class KnowledgeRebuildServiceTest {

    private KnowledgeBaseService knowledgeBaseService;
    private DocumentEtlPipeline documentEtlPipeline;
    private KnowledgeFileStore fileStore;
    private KnowledgeDocumentRepository documentRepository;
    private KnowledgeBaseMapper knowledgeBaseMapper;
    private EmbeddingService embeddingService;
    private VectorStore vectorStore;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private KnowledgeRebuildService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        knowledgeBaseService = mock(KnowledgeBaseService.class);
        documentEtlPipeline = mock(DocumentEtlPipeline.class);
        fileStore = mock(KnowledgeFileStore.class);
        documentRepository = mock(KnowledgeDocumentRepository.class);
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        embeddingService = mock(EmbeddingService.class);
        vectorStore = mock(VectorStore.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // 异步任务同步执行，便于断言重建结果
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        service = new KnowledgeRebuildService(knowledgeBaseService, documentEtlPipeline, fileStore,
                documentRepository, knowledgeBaseMapper, embeddingService, vectorStore, redisTemplate, executor);

        when(knowledgeBaseService.getKnowledgeBase(eq(1L), anyString())).thenReturn(new KnowledgeBaseDO());
    }

    private static KnowledgeDocument doc(String docId, String filePath) {
        KnowledgeDocument d = new KnowledgeDocument();
        d.setKbId(1L);
        d.setDocId(docId);
        d.setFileName(docId + ".txt");
        d.setFilePath(filePath);
        return d;
    }

    @Test
    void rejectedWhenLockAlreadyHeld() {
        when(valueOps.setIfAbsent(eq("kb:rebuild:1"), anyString(), any(Duration.class))).thenReturn(false);

        assertThrows(ClientException.class, () -> service.startRebuild(1L, "u", false));
        verify(vectorStore, never()).deleteIndex(any());
    }

    @Test
    void rebuildProcessesRetainedDocsAndSkipsMissingFiles() {
        when(valueOps.setIfAbsent(eq("kb:rebuild:1"), anyString(), any(Duration.class))).thenReturn(true);
        KnowledgeDocument retained = doc("d1", "1/d1.txt");
        KnowledgeDocument legacy = doc("d2", null);
        when(documentRepository.findByKbIdAndUsername(1L, "u")).thenReturn(List.of(retained, legacy));
        when(fileStore.read("1/d1.txt")).thenReturn("内容".getBytes());
        when(fileStore.read(null)).thenReturn(null);
        when(documentEtlPipeline.process(eq(1L), eq("u"), any(byte[].class), eq("d1.txt"), eq("d1"))).thenReturn(true);

        service.startRebuild(1L, "u", false);

        verify(vectorStore).deleteIndex(1L);
        verify(vectorStore).createIndexIfNotExists(1L);
        verify(documentEtlPipeline).process(eq(1L), eq("u"), any(byte[].class), eq("d1.txt"), eq("d1"));
        // 无留存文件的存量文档标记失败并计入 skipped
        assertEquals(3, legacy.getStatus());
        verify(documentRepository).save(legacy);

        ArgumentCaptor<String> progressCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps, atLeast(1)).set(eq("kb:rebuild:progress:1"), progressCaptor.capture(), any(Duration.class));
        assertEquals("{\"total\":2,\"done\":1,\"skipped\":1}", progressCaptor.getValue());

        // 重建结束释放锁
        verify(redisTemplate).delete("kb:rebuild:1");
    }

    @Test
    void nonForceValidatesEmbeddingCompatibility() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(documentRepository.findByKbIdAndUsername(1L, "u")).thenReturn(List.of());

        service.startRebuild(1L, "u", false);

        verify(knowledgeBaseService).ensureEmbeddingCompatible(1L, "u");
        verify(knowledgeBaseMapper, never()).updateById(any(KnowledgeBaseDO.class));
    }

    @Test
    void forceUpdatesKbEmbeddingMetadata() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(documentRepository.findByKbIdAndUsername(1L, "u")).thenReturn(List.of());
        when(embeddingService.getEmbeddingModel()).thenReturn("text-embedding-v9");
        when(embeddingService.getEmbeddingDimension()).thenReturn(1024);

        service.startRebuild(1L, "u", true);

        // force 通道：跳过兼容性校验，库元数据更新为当前 embedding 配置
        verify(knowledgeBaseService, never()).ensureEmbeddingCompatible(any(), anyString());
        ArgumentCaptor<KnowledgeBaseDO> kbCaptor = ArgumentCaptor.forClass(KnowledgeBaseDO.class);
        verify(knowledgeBaseMapper).updateById(kbCaptor.capture());
        assertEquals("text-embedding-v9", kbCaptor.getValue().getEmbeddingModel());
        assertEquals(1024, kbCaptor.getValue().getEmbeddingDim());
    }

    @Test
    void statusReportsRebuildingAndProgress() {
        when(redisTemplate.hasKey("kb:rebuild:1")).thenReturn(true);
        when(valueOps.get("kb:rebuild:progress:1")).thenReturn("{\"total\":3,\"done\":1,\"skipped\":0}");

        var status = service.getStatus(1L, "u");

        assertTrue((Boolean) status.get("rebuilding"));
        assertEquals(3, ((cn.hutool.json.JSONObject) status.get("progress")).getInt("total"));
    }
}
