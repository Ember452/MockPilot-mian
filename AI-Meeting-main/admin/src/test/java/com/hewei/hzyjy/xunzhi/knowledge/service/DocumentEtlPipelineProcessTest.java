package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.hewei.hzyjy.xunzhi.knowledge.dao.entity.KnowledgeDocument;
import com.hewei.hzyjy.xunzhi.knowledge.dao.mapper.KnowledgeBaseMapper;
import com.hewei.hzyjy.xunzhi.knowledge.dao.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ETL 同步入口行为测试：新建落盘、复用 docId 重灌、失败态 status=3。
 */
class DocumentEtlPipelineProcessTest {

    private VectorStore vectorStore;
    private KnowledgeDocumentRepository documentRepository;
    private KnowledgeFileStore fileStore;
    private DocumentEtlPipeline pipeline;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        documentRepository = mock(KnowledgeDocumentRepository.class);
        KnowledgeBaseMapper knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        fileStore = mock(KnowledgeFileStore.class);
        pipeline = new DocumentEtlPipeline(vectorStore, embeddingService, documentRepository, knowledgeBaseMapper, fileStore);
        when(embeddingService.embed(anyString())).thenReturn(List.of(0.1f));
        // selectById 返回 null 跳过知识库计数刷新，聚焦文档状态断言
        when(knowledgeBaseMapper.selectById(any())).thenReturn(null);
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private KnowledgeDocument lastSavedDoc() {
        ArgumentCaptor<KnowledgeDocument> captor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(documentRepository, atLeast(1)).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void newDocumentPersistsFileBeforeIndexing() {
        when(fileStore.save(eq(1L), anyString(), eq("txt"), any())).thenReturn("1/x.txt");

        boolean ok = pipeline.process(1L, "u", bytes("知识内容"), "a.txt", null);

        assertTrue(ok);
        verify(fileStore).save(eq(1L), anyString(), eq("txt"), any());
        verify(vectorStore).indexChunks(eq(1L), anyList());
        KnowledgeDocument saved = lastSavedDoc();
        assertEquals(2, saved.getStatus());
        assertEquals("1/x.txt", saved.getFilePath());
    }

    @Test
    void persistFailureDoesNotBlockEtl() {
        // 落盘失败（返回 null）fail-open：ETL 照常完成，filePath 为空
        when(fileStore.save(any(), anyString(), anyString(), any())).thenReturn(null);

        boolean ok = pipeline.process(1L, "u", bytes("知识内容"), "a.txt", null);

        assertTrue(ok);
        KnowledgeDocument saved = lastSavedDoc();
        assertEquals(2, saved.getStatus());
        assertEquals(null, saved.getFilePath());
    }

    @Test
    void parseFailureMarksStatusFailed() {
        // 非法 pdf 字节触发解析异常
        boolean ok = pipeline.process(1L, "u", bytes("not a pdf"), "a.pdf", null);

        assertFalse(ok);
        assertEquals(3, lastSavedDoc().getStatus());
        verify(vectorStore, never()).indexChunks(anyLong(), anyList());
    }

    @Test
    void emptyTextMarksStatusFailed() {
        boolean ok = pipeline.process(1L, "u", bytes("   "), "a.txt", null);

        assertFalse(ok);
        assertEquals(3, lastSavedDoc().getStatus());
        verify(vectorStore, never()).indexChunks(anyLong(), anyList());
    }

    @Test
    void indexingExceptionMarksStatusFailed() {
        doThrow(new RuntimeException("es down")).when(vectorStore).indexChunks(anyLong(), anyList());

        boolean ok = pipeline.process(1L, "u", bytes("知识内容"), "a.txt", null);

        assertFalse(ok);
        assertEquals(3, lastSavedDoc().getStatus());
    }

    @Test
    void reuseDocIdReindexesExistingRecordWithoutPersisting() {
        KnowledgeDocument existing = new KnowledgeDocument();
        existing.setKbId(1L);
        existing.setDocId("doc-1");
        existing.setFileName("a.txt");
        existing.setFilePath("1/doc-1.txt");
        when(documentRepository.findByDocId("doc-1")).thenReturn(List.of(existing));

        boolean ok = pipeline.process(1L, "u", bytes("重建内容"), "a.txt", "doc-1");

        assertTrue(ok);
        // 复用路径不重复落盘，保留原 filePath
        verify(fileStore, never()).save(any(), anyString(), anyString(), any());
        verify(vectorStore).indexChunks(eq(1L), anyList());
        assertEquals(2, existing.getStatus());
        assertEquals("1/doc-1.txt", existing.getFilePath());
    }

    @Test
    void reuseUnknownDocIdReturnsFalse() {
        when(documentRepository.findByDocId("missing")).thenReturn(List.of());

        boolean ok = pipeline.process(1L, "u", bytes("内容"), "a.txt", "missing");

        assertFalse(ok);
        verify(vectorStore, never()).indexChunks(anyLong(), anyList());
    }
}
