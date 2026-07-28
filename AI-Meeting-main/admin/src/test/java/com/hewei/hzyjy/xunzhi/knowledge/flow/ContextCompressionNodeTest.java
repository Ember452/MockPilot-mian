package com.hewei.hzyjy.xunzhi.knowledge.flow;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Small-to-Big 上下文注入：父块替换、parent_id 去重与存量子块回退测试。
 */
class ContextCompressionNodeTest {

    private static Map<String, Object> chunk(String content, String parentId, String parentContent) {
        Map<String, Object> map = new HashMap<>();
        map.put("content", content);
        map.put("file_name", "doc.md");
        if (parentId != null) {
            map.put("parent_id", parentId);
        }
        if (parentContent != null) {
            map.put("parent_content", parentContent);
        }
        return map;
    }

    @Test
    void parentContentInjectedInsteadOfChild() {
        String context = ContextCompressionNode.buildKnowledgeContext(
                List.of(chunk("子块片段", "d1_p0", "父块完整上下文内容")));

        assertTrue(context.contains("父块完整上下文内容"));
        assertFalse(context.contains("子块片段"));
        assertTrue(context.contains("[1]"));
    }

    @Test
    void sameParentDedupedButNumberingPreserved() {
        String context = ContextCompressionNode.buildKnowledgeContext(List.of(
                chunk("子块A", "d1_p0", "父块内容X"),
                chunk("子块B", "d1_p0", "父块内容X"),
                chunk("子块C", "d1_p1", "父块内容Y")));

        // 父块 X 只注入一次，第二次命中仅标注同源，编号仍与 references 对齐
        assertTrue(context.indexOf("父块内容X") == context.lastIndexOf("父块内容X"));
        assertTrue(context.contains("(内容同 [1])"));
        assertTrue(context.contains("[1]"));
        assertTrue(context.contains("[2]"));
        assertTrue(context.contains("[3]"));
        assertTrue(context.contains("父块内容Y"));
    }

    @Test
    void legacyChunkWithoutParentFallsBackToContent() {
        String context = ContextCompressionNode.buildKnowledgeContext(
                List.of(chunk("存量子块内容", null, null)));

        assertTrue(context.contains("存量子块内容"));
    }

    @Test
    void blankParentContentAlsoFallsBack() {
        String context = ContextCompressionNode.buildKnowledgeContext(
                List.of(chunk("子块内容", "d1_p0", "  ")));

        assertTrue(context.contains("子块内容"));
    }
}
