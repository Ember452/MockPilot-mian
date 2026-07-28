package com.hewei.hzyjy.xunzhi.knowledge.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 父子分块参数化切分纯函数测试。
 */
class DocumentEtlPipelineTest {

    @Test
    void blankTextReturnsEmpty() {
        assertTrue(DocumentEtlPipeline.chunkText("", 400, 50).isEmpty());
        assertTrue(DocumentEtlPipeline.chunkText("   ", 400, 50).isEmpty());
        assertTrue(DocumentEtlPipeline.chunkText(null, 400, 50).isEmpty());
    }

    @Test
    void shortTextSingleChunk() {
        List<String> chunks = DocumentEtlPipeline.chunkText("短文本内容", 400, 50);
        assertEquals(1, chunks.size());
        assertEquals("短文本内容", chunks.get(0));
    }

    @Test
    void paragraphsAggregatedUntilMaxSizeWithOverlap() {
        String paraA = "甲".repeat(300);
        String paraB = "乙".repeat(300);
        List<String> chunks = DocumentEtlPipeline.chunkText(paraA + "\n\n" + paraB, 400, 50);

        assertEquals(2, chunks.size());
        assertEquals(paraA, chunks.get(0));
        // 第二块以前一块末尾 50 字重叠开头，保持上下文连续
        assertTrue(chunks.get(1).startsWith("甲".repeat(50)));
        assertTrue(chunks.get(1).endsWith(paraB));
    }

    @Test
    void zeroOverlapCarriesNothing() {
        String paraA = "甲".repeat(300);
        String paraB = "乙".repeat(300);
        List<String> chunks = DocumentEtlPipeline.chunkText(paraA + "\n\n" + paraB, 400, 0);

        assertEquals(2, chunks.size());
        assertEquals(paraA, chunks.get(0));
        // 父块切分（overlap=0）不携带前块尾部
        assertEquals(paraB, chunks.get(1));
    }

    @Test
    void oversizedParagraphSplitAtSentenceEnd() {
        // 900 字超长段落（> maxSize*2），第 451 字处句号应成为切分点
        String text = "字".repeat(450) + "。" + "词".repeat(449);
        List<String> chunks = DocumentEtlPipeline.chunkText(text, 400, 50);

        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).endsWith("。"));
        assertEquals(451, chunks.get(0).length());
    }

    @Test
    void parentThenChildTwoLevelChunking() {
        // 模拟 ETL 两级切分：父块 1600/0 → 父块内子块 400/50
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append("段".repeat(350)).append("\n\n");
        }
        List<String> parents = DocumentEtlPipeline.chunkText(sb.toString(), 1600, 0);
        assertTrue(parents.size() > 1);

        for (String parent : parents) {
            List<String> children = DocumentEtlPipeline.chunkText(parent, 400, 50);
            assertTrue(children.size() >= 1);
            for (String child : children) {
                // 子块不超过 maxSize*2 的硬上限（句末切分回退窗口内）
                assertTrue(child.length() <= 800, "child too long: " + child.length());
            }
        }
    }
}
