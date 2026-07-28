package com.hewei.hzyjy.xunzhi.knowledge.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 引用来源构建与 Prompt 模板渲染纯函数测试。
 */
class RagChatServiceTest {

    @Test
    void buildReferencesMapsChunkFields() {
        List<Map<String, Object>> chunks = List.of(Map.of(
                "file_name", "guide.pdf",
                "doc_id", "d1",
                "chunk_index", 3,
                "_rerank_score", 0.87,
                "content", "Elasticsearch 混合检索说明"
        ));

        List<Map<String, Object>> references = RagChatService.buildReferences(chunks, false);

        assertEquals(1, references.size());
        Map<String, Object> ref = references.get(0);
        assertEquals("guide.pdf", ref.get("fileName"));
        assertEquals("d1", ref.get("docId"));
        assertEquals(3, ref.get("chunkIndex"));
        assertEquals(0.87, ref.get("score"));
        assertEquals("Elasticsearch 混合检索说明", ref.get("snippet"));
    }

    @Test
    void buildReferencesTruncatesSnippetTo200Chars() {
        String longContent = "长".repeat(500);
        List<Map<String, Object>> chunks = List.of(Map.of("content", longContent));

        List<Map<String, Object>> references = RagChatService.buildReferences(chunks, false);

        assertEquals(200, ((String) references.get(0).get("snippet")).length());
    }

    @Test
    void webSearchDegradationReturnsEmptyReferences() {
        List<Map<String, Object>> chunks = List.of(Map.of("content", "abc"));

        assertTrue(RagChatService.buildReferences(chunks, true).isEmpty());
        assertTrue(RagChatService.buildReferences(null, false).isEmpty());
        assertTrue(RagChatService.buildReferences(List.of(), false).isEmpty());
    }

    @Test
    void renderTemplateReplacesPlaceholders() {
        String rendered = RagChatService.renderTemplate(
                "资料：{context}\n问题：{question}", "参考内容", "什么是RRF");

        assertEquals("资料：参考内容\n问题：什么是RRF", rendered);
    }

    @Test
    void renderTemplateToleratesNullValues() {
        assertEquals("资料：/问题：",
                RagChatService.renderTemplate("资料：{context}/问题：{question}", null, null));
    }
}
